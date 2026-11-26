package com.harmonyiron.app.agent

import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.data.engine.StrengthMath
import com.harmonyiron.app.data.entity.CoachProfileEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * 端侧本地只读与提案工具集：供 ReAct Agent 按需精准读取本地训练数据并输出结构化 Diff。
 */
class LocalCoachTools(private val db: HarmonyIronDatabase) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var latestProposal: PlanDiffProposal? = null
        private set

    var pendingQuestionArgs: InterviewQuestionArgs? = null
        private set

    fun clearSessionOutputs() {
        latestProposal = null
        pendingQuestionArgs = null
    }

    fun getToolDefinitions(): List<ToolDefinitionDto> = listOf(
        ToolDefinitionDto(
            function = FunctionSpecDto(
                name = "get_training_summary",
                description = "获取指定天数内的训练总容量、完成练次与各肌群训练组数分布",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("days") {
                            put("type", "integer")
                            put("description", "统计天数，例如 7 或 28")
                        }
                    }
                    put("required", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("days")) })
                }
            )
        ),
        ToolDefinitionDto(
            function = FunctionSpecDto(
                name = "get_exercise_progress",
                description = "查询特定动作近期的重量、次数、RPE 疲劳度及估算 1RM 变化趋势",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("exercise_name") {
                            put("type", "string")
                            put("description", "动作名称关键字，如 卧推、深蹲")
                        }
                    }
                    put("required", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("exercise_name")) })
                }
            )
        ),
        ToolDefinitionDto(
            function = FunctionSpecDto(
                name = "get_current_plan",
                description = "获取当前正在启用的训练计划结构、每日分化及每个动作的参数（含 itemId、dayId、exerciseId）",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {}
                }
            )
        ),
        ToolDefinitionDto(
            function = FunctionSpecDto(
                name = "get_user_coach_profile",
                description = "读取用户已沉淀的教练档案（伤病史、单次训练时长上限、器械偏好）",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {}
                }
            )
        ),
        ToolDefinitionDto(
            function = FunctionSpecDto(
                name = "ask_user_choice",
                description = "向用户弹出单张结构化选项卡片进行深度访谈追问（每次仅问 1 个关键决策点，首项应标记 isRecommended=true 并附个人数据理由）",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("question") {
                            put("type", "string")
                            put("description", "简洁明确的问题描述")
                        }
                        putJsonObject("allowMultiSelect") {
                            put("type", "boolean")
                            put("description", "是否允许多选，默认 false")
                        }
                        putJsonObject("options") {
                            put("type", "array")
                            put("description", "选项列表，每项包含 label, isRecommended(boolean), reason(可选)")
                            putJsonObject("items") { put("type", "object") }
                        }
                    }
                    put("required", buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("question"))
                        add(kotlinx.serialization.json.JsonPrimitive("options"))
                    })
                }
            )
        ),
        ToolDefinitionDto(
            function = FunctionSpecDto(
                name = "update_coach_profile",
                description = "将访谈中确认的长期训练约束（伤病史、单次时长上限、器械偏好）持久化沉淀至本地教练档案",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("injuryNotes") { put("type", "string") }
                        putJsonObject("maxSessionMinutes") { put("type", "integer") }
                        putJsonObject("preferredEquipment") { put("type", "string") }
                    }
                }
            )
        ),
        ToolDefinitionDto(
            function = FunctionSpecDto(
                name = "propose_plan_diff",
                description = "向用户提交结构化的训练计划修改提案（不会直接修改数据库，由用户预览勾选后生效）",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("summary") {
                            put("type", "string")
                            put("description", "本次调整的总体思路概述")
                        }
                        putJsonObject("changes") {
                            put("type", "array")
                            put("description", "具体变更列表，每项包含 id, actionType(REPLACE_EXERCISE/UPDATE_PARAMS/ADD_EXERCISE/REMOVE_EXERCISE), planDayId, targetItemId, newExerciseId, newSets, newReps, newRestSeconds, title, beforeDesc, afterDesc, reason")
                            putJsonObject("items") {
                                put("type", "object")
                            }
                        }
                    }
                    put("required", buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("summary"))
                        add(kotlinx.serialization.json.JsonPrimitive("changes"))
                    })
                }
            )
        )
    )

    suspend fun executeTool(name: String, rawArguments: String): String {
        val argsObj = runCatching { json.decodeFromString<JsonObject>(rawArguments) }
            .getOrDefault(buildJsonObject {})

        return when (name) {
            "get_training_summary" -> {
                val days = argsObj["days"]?.jsonPrimitive?.intOrNull ?: 14
                buildTrainingSummary(days)
            }
            "get_exercise_progress" -> {
                val exName = argsObj["exercise_name"]?.jsonPrimitive?.content.orEmpty()
                buildExerciseProgress(exName)
            }
            "get_current_plan" -> buildCurrentPlanJson()
            "get_user_coach_profile" -> buildCoachProfileJson()
            "ask_user_choice" -> handleAskUserChoice(rawArguments)
            "update_coach_profile" -> handleUpdateCoachProfile(argsObj)
            "propose_plan_diff" -> handleProposeDiff(rawArguments)
            else -> """{"error": "未知工具: $name"}"""
        }
    }

    private suspend fun buildTrainingSummary(days: Int): String {
        val cutoff = System.currentTimeMillis() - days.coerceAtLeast(1) * 24L * 60 * 60 * 1000
        val sessions = db.workoutSessionDao().getAllSessions().filter { it.startedAt >= cutoff }
        val exercises = db.exerciseDao().getAll().associateBy { it.id }

        val muscleSetCounts = mutableMapOf<String, Int>()
        var totalVolume = 0.0
        for (session in sessions) {
            totalVolume += session.totalVolumeKg
            val logs = db.workoutSessionDao().getLogsForSession(session.id)
            for (log in logs) {
                val muscle = exercises[log.exerciseId]?.muscleGroup ?: "其他"
                muscleSetCounts[muscle] = (muscleSetCounts[muscle] ?: 0) + 1
            }
        }

        val result = buildJsonObject {
            put("periodDays", days)
            put("completedSessions", sessions.size)
            put("totalVolumeKg", totalVolume)
            putJsonObject("muscleGroupSets") {
                muscleSetCounts.forEach { (k, v) -> put(k, v) }
            }
        }
        return json.encodeToString(result)
    }

    private suspend fun buildExerciseProgress(queryName: String): String {
        val exercises = db.exerciseDao().getAll()
        val matched = exercises.firstOrNull { it.name.contains(queryName, ignoreCase = true) }
            ?: return """{"error": "未找到包含 '$queryName' 的动作"}"""

        val logs = db.workoutSessionDao().getHistoryForExercise(matched.id).takeLast(12)
        val recordsJson = buildJsonArray {
            logs.forEach { log ->
                add(buildJsonObject {
                    put("weightKg", log.weightKg)
                    put("reps", log.reps)
                    put("rpe", log.rpe ?: 0f)
                    put("estimated1RM", StrengthMath.estimate1RM(log.weightKg, log.reps))
                    put("isPr", log.isPr)
                })
            }
        }
        return json.encodeToString(buildJsonObject {
            put("exerciseId", matched.id)
            put("exerciseName", matched.name)
            put("recentLogs", recordsJson)
        })
    }

    private suspend fun buildCurrentPlanJson(): String {
        val plans = db.workoutPlanDao().getAllPlans()
        val active = plans.firstOrNull { it.isActive } ?: plans.firstOrNull()
            ?: return """{"error": "当前无可用训练计划"}"""

        val exercises = db.exerciseDao().getAll().associateBy { it.id }
        val days = db.workoutPlanDao().getDaysForPlan(active.id)

        val daysJson = buildJsonArray {
            days.forEach { day ->
                val items = db.workoutPlanDao().getItemsForDay(day.id)
                add(buildJsonObject {
                    put("planDayId", day.id)
                    put("dayOrder", day.dayOrder)
                    put("title", day.title)
                    put("items", buildJsonArray {
                        items.forEach { item ->
                            add(buildJsonObject {
                                put("itemId", item.id)
                                put("exerciseId", item.exerciseId)
                                put("exerciseName", exercises[item.exerciseId]?.name ?: "未知")
                                put("targetSets", item.targetSets)
                                put("targetReps", item.targetReps)
                                put("defaultRestSeconds", item.defaultRestSeconds)
                                put("setType", item.setType)
                            })
                        }
                    })
                })
            }
        }

        val availableExercises = buildJsonArray {
            exercises.values.forEach { ex ->
                add(buildJsonObject {
                    put("exerciseId", ex.id)
                    put("name", ex.name)
                    put("muscleGroup", ex.muscleGroup)
                    put("equipment", ex.equipment)
                })
            }
        }

        return json.encodeToString(buildJsonObject {
            put("planId", active.id)
            put("planName", active.name)
            put("days", daysJson)
            put("availableExercises", availableExercises)
        })
    }

    private suspend fun buildCoachProfileJson(): String {
        val profile = db.coachProfileDao().getProfile()
        return if (profile == null) {
            """{"injuryNotes": "", "maxSessionMinutes": 60, "preferredEquipment": ""}"""
        } else {
            json.encodeToString(profile)
        }
    }

    private fun handleAskUserChoice(rawArguments: String): String {
        return parseInterviewQuestionArgs(rawArguments).fold(
            onSuccess = { args ->
                pendingQuestionArgs = args
                """{"status": "waiting_user_choice"}"""
            },
            onFailure = { err ->
                """{"error": "提问卡片格式校验失败: ${err.message}"}"""
            }
        )
    }

    private suspend fun handleUpdateCoachProfile(argsObj: JsonObject): String {
        val current = db.coachProfileDao().getProfile() ?: CoachProfileEntity()
        val updated = current.copy(
            injuryNotes = argsObj["injuryNotes"]?.jsonPrimitive?.content ?: current.injuryNotes,
            maxSessionMinutes = argsObj["maxSessionMinutes"]?.jsonPrimitive?.intOrNull ?: current.maxSessionMinutes,
            preferredEquipment = argsObj["preferredEquipment"]?.jsonPrimitive?.content ?: current.preferredEquipment,
            updatedAt = System.currentTimeMillis()
        )
        db.coachProfileDao().upsert(updated)
        return """{"status": "profile_updated"}"""
    }

    private fun handleProposeDiff(rawArguments: String): String {
        return parsePlanDiffProposal(rawArguments).fold(
            onSuccess = { proposal ->
                latestProposal = proposal
                """{"status": "proposed", "changeCount": ${proposal.changes.size}}"""
            },
            onFailure = { err ->
                """{"error": "Diff 提案格式校验失败: ${err.message}"}"""
            }
        )
    }

    companion object {
        private val lenientJson = Json { ignoreUnknownKeys = true }

        fun parsePlanDiffProposal(rawJson: String): Result<PlanDiffProposal> = runCatching {
            val proposal = lenientJson.decodeFromString<PlanDiffProposal>(rawJson)
            require(proposal.changes.isNotEmpty()) { "变更列表不能为空" }
            proposal
        }

        fun parseInterviewQuestionArgs(rawJson: String): Result<InterviewQuestionArgs> = runCatching {
            val parsed = lenientJson.decodeFromString<InterviewQuestionArgs>(rawJson)
            require(parsed.question.isNotBlank()) { "问题内容不能为空" }
            require(parsed.options.isNotEmpty()) { "选项列表不能为空" }
            parsed
        }
    }
}
