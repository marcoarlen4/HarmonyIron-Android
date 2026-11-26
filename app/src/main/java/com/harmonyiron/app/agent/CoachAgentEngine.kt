package com.harmonyiron.app.agent

import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.data.entity.PlanItemEntity
import com.harmonyiron.app.data.entity.PlanSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class PlanSnapshotData(
    val daysWithItems: Map<Long, List<PlanItemEntity>>
)

/**
 * 端侧原生 ReAct / Tool-Calling 力量教练引擎：
 * 负责多轮本地查库、生成专业诊断与 Diff 提案，以及安全落盘与快照回滚。
 */
class CoachAgentEngine(
    private val db: HarmonyIronDatabase,
    private val configStore: AiConfigStore,
    private val client: OpenAiCompatibleClient = OpenAiCompatibleClient()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun runAnalysis(userPrompt: String): Result<CoachAgentResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val config = configStore.load()
            val effectiveUrl = if (config.useByok) config.baseUrl else AiConfig.BUILTIN_GATEWAY_URL
            val effectiveKey = if (config.useByok) config.apiKey else "builtin-trial-token"

            if (config.useByok && effectiveKey.isBlank()) {
                error("请先在右上角「设置」中填写 BYOK API Key")
            }

            val tools = LocalCoachTools(db)
            tools.clearProposal()
            val toolDefs = tools.getToolDefinitions()
            val executedTools = mutableListOf<String>()

            val messages = mutableListOf(
                ChatMessageDto(role = "system", content = SYSTEM_PROMPT),
                ChatMessageDto(role = "user", content = userPrompt)
            )

            var finalContent = ""
            for (step in 0 until MAX_REACT_STEPS) {
                val response = client.chatCompletion(
                    baseUrl = effectiveUrl,
                    apiKey = effectiveKey,
                    requestPayload = ChatCompletionRequest(
                        model = config.modelName.ifBlank { AiConfig.DEFAULT_MODEL },
                        messages = messages,
                        tools = toolDefs
                    )
                ).getOrThrow()

                val assistantMsg = response.choices.firstOrNull()?.message
                    ?: error("模型未返回有效响应")
                messages.add(assistantMsg)

                val toolCalls = assistantMsg.toolCalls
                if (toolCalls.isNullOrEmpty()) {
                    finalContent = assistantMsg.content.orEmpty()
                    break
                }

                for (call in toolCalls) {
                    executedTools.add(call.function.name)
                    val toolResult = tools.executeTool(call.function.name, call.function.arguments)
                    messages.add(
                        ChatMessageDto(
                            role = "tool",
                            toolCallId = call.id,
                            name = call.function.name,
                            content = toolResult
                        )
                    )
                }
            }

            CoachAgentResponse(
                analysisReport = finalContent.ifBlank { "已完成分析，请查看下方训练计划调整建议。" },
                diffProposal = tools.latestProposal,
                executedTools = executedTools.distinct()
            )
        }
    }

    /**
     * 应用用户选中的 Diff 修改项（支持局部选择性替换或一键全部更新），并在落盘前自动保存快照。
     */
    suspend fun applyPlanDiff(
        proposal: PlanDiffProposal,
        selectedChangeIds: Set<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val selectedChanges = filterSelectedChanges(proposal, selectedChangeIds)
            require(selectedChanges.isNotEmpty()) { "请至少勾选一项调整建议" }

            val activePlan = db.workoutPlanDao().getAllPlans().let { plans ->
                plans.firstOrNull { it.isActive } ?: plans.firstOrNull()
            } ?: error("未找到可更新的训练计划")

            // 1. 更新前自动保存当前计划快照
            val days = db.workoutPlanDao().getDaysForPlan(activePlan.id)
            val snapshotMap = mutableMapOf<Long, List<PlanItemEntity>>()
            for (day in days) {
                snapshotMap[day.id] = db.workoutPlanDao().getItemsForDay(day.id)
            }
            db.workoutPlanDao().insertSnapshot(
                PlanSnapshotEntity(
                    planId = activePlan.id,
                    snapshotJson = json.encodeToString(PlanSnapshotData(snapshotMap)),
                    reason = proposal.summary
                )
            )

            // 2. 逐条应用选中的变更
            var appliedCount = 0
            for (change in selectedChanges) {
                val dayItems = db.workoutPlanDao().getItemsForDay(change.planDayId)
                when (change.actionType) {
                    "REPLACE_EXERCISE", "UPDATE_PARAMS" -> {
                        val target = dayItems.firstOrNull { it.id == change.targetItemId }
                        if (target != null) {
                            db.workoutPlanDao().updatePlanItem(
                                target.copy(
                                    exerciseId = change.newExerciseId ?: target.exerciseId,
                                    targetSets = change.newSets ?: target.targetSets,
                                    targetReps = change.newReps ?: target.targetReps,
                                    defaultRestSeconds = change.newRestSeconds ?: target.defaultRestSeconds
                                )
                            )
                            appliedCount++
                        }
                    }
                    "ADD_EXERCISE" -> {
                        val exId = change.newExerciseId
                        if (exId != null) {
                            val nextOrder = (dayItems.maxOfOrNull { it.sortOrder } ?: 0) + 1
                            db.workoutPlanDao().insertPlanItems(
                                listOf(
                                    PlanItemEntity(
                                        planDayId = change.planDayId,
                                        exerciseId = exId,
                                        sortOrder = nextOrder,
                                        targetSets = change.newSets ?: 3,
                                        targetReps = change.newReps ?: 10,
                                        defaultRestSeconds = change.newRestSeconds ?: 90
                                    )
                                )
                            )
                            appliedCount++
                        }
                    }
                    "REMOVE_EXERCISE" -> {
                        val targetId = change.targetItemId
                        if (targetId != null) {
                            db.workoutPlanDao().deletePlanItemById(targetId)
                            appliedCount++
                        }
                    }
                }
            }
            appliedCount
        }
    }

    /**
     * 一键回滚至最近一次修改前的计划快照。
     */
    suspend fun rollbackLatestSnapshot(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val activePlan = db.workoutPlanDao().getAllPlans().let { plans ->
                plans.firstOrNull { it.isActive } ?: plans.firstOrNull()
            } ?: error("未找到当前计划")

            val snapshot = db.workoutPlanDao().getLatestSnapshot(activePlan.id)
                ?: error("暂无可回滚的历史快照")

            val parsed = json.decodeFromString<PlanSnapshotData>(snapshot.snapshotJson)
            for ((dayId, items) in parsed.daysWithItems) {
                db.workoutPlanDao().replaceItemsForDay(dayId, items)
            }
            "已成功回滚至修改前版本（原调整原因：${snapshot.reason}）"
        }
    }

    companion object {
        private const val MAX_REACT_STEPS = 6

        fun filterSelectedChanges(
            proposal: PlanDiffProposal,
            selectedIds: Set<String>
        ): List<DiffChangeItem> = proposal.changes.filter { it.id in selectedIds }

        private const val SYSTEM_PROMPT = """
你是「鸿铁（HarmonyIron）」内置的资深力量训练与体能教练。
你的职责：
1. 必须先调用本地工具（get_training_summary、get_current_plan、get_exercise_progress 等）获取真实训练数据，严禁凭空捏造数据。
2. 基于渐进超负荷、推拉平衡、RPE 疲劳管理及减载周（Deload）原则给出简洁、专业、可执行的诊断建议。
3. 当建议修改计划时，必须调用 propose_plan_diff 工具输出结构化变更（使用 get_current_plan 返回的真实 planDayId、itemId 和 availableExercises 中的 exerciseId），不要口头承诺修改成功。
4. 用简体中文回复，重点突出数据依据与动作调整理由。
"""
    }
}
