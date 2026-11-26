package com.harmonyiron.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonyiron.app.agent.AiConfigStore
import com.harmonyiron.app.agent.CoachAgentEngine
import com.harmonyiron.app.agent.CoachAgentResponse
import com.harmonyiron.app.agent.InterviewQuestion
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.ui.theme.IronColors
import kotlinx.coroutines.launch

/**
 * AI 力量教练主页：一键周期复盘、Grill-Coach 深度定制访谈、自然语言追问、Diff 选择性替换与快照回滚。
 */
@Composable
fun CoachScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember {
        CoachAgentEngine(
            db = HarmonyIronDatabase.getInstance(context),
            configStore = AiConfigStore(context)
        )
    }

    var promptInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var agentResponse by remember { mutableStateOf<CoachAgentResponse?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val selectedMap = remember { mutableStateMapOf<String, Boolean>() }

    fun handleSuccess(res: CoachAgentResponse) {
        agentResponse = res
        selectedMap.clear()
        res.diffProposal?.changes?.forEach { item ->
            selectedMap[item.id] = true
        }
    }

    fun triggerAnalysis(query: String) {
        if (isLoading) return
        isLoading = true
        statusMessage = null
        scope.launch {
            engine.runAnalysis(query).fold(
                onSuccess = { handleSuccess(it) },
                onFailure = { statusMessage = "分析失败：${it.message}" }
            )
            isLoading = false
        }
    }

    fun answerInterview(question: InterviewQuestion, answerText: String) {
        val currentRes = agentResponse ?: return
        if (isLoading) return
        isLoading = true
        statusMessage = null
        scope.launch {
            engine.resumeAfterChoice(
                previousMessages = currentRes.sessionMessages,
                toolCallId = question.toolCallId,
                userAnswer = answerText,
                currentRound = question.roundIndex
            ).fold(
                onSuccess = { handleSuccess(it) },
                onFailure = { statusMessage = "访谈续接失败：${it.message}" }
            )
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AI 力量教练", color = IronColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("本地数据驱动 · Grill-Coach 深度定制", color = IronColors.TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置与 BYOK", tint = IronColors.TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 快捷操作栏（一键周复盘 / 深度定制访谈 / 回滚）
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { triggerAnalysis("请结合我近14天的本地训练容量与动作数据，做一次周期复盘诊断，如有必要请给出计划调整提案。") },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("一键复盘", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = { triggerAnalysis("请启动 Grill-Coach 深度访谈模式，为我定制或重构下一阶段周期训练计划。先查库补全已知数据，再逐一追问未知约束。") },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = IronColors.AccentGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("深度定制", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        engine.rollbackLatestSnapshot().fold(
                            onSuccess = { msg -> statusMessage = msg },
                            onFailure = { err -> statusMessage = err.message }
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("回滚", color = IronColors.TextPrimary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 自然语言追问输入框
        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            placeholder = { Text("例如：最近练得太累了想改计划 / 左肩不适换动作", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val q = promptInput.trim()
                if (q.isNotEmpty()) triggerAnalysis(q)
            },
            enabled = !isLoading && promptInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = IronColors.SurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("发送给 AI 教练", color = IronColors.PrimaryOrange, fontWeight = FontWeight.Bold)
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = IronColors.PrimaryOrange, modifier = Modifier.width(20.dp).height(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("正在分析本地训练数据...", color = IronColors.TextSecondary, fontSize = 13.sp)
            }
        }

        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(msg, color = IronColors.AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Grill-Coach 结构化选项卡片（当 Agent 暂停等待用户抉择时展示）
        agentResponse?.pendingQuestion?.let { question ->
            Spacer(modifier = Modifier.height(16.dp))
            GrillInterviewCard(
                question = question,
                enabled = !isLoading,
                onSubmitAnswer = { answer -> answerInterview(question, answer) }
            )
        }

        agentResponse?.let { res ->
            if (res.pendingQuestion == null && res.analysisReport.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (res.executedTools.isNotEmpty()) {
                            Text(
                                "已调用本地工具：${res.executedTools.joinToString(", ")}",
                                color = IronColors.AccentGreen,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text("教练诊断报告", color = IronColors.PrimaryOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(res.analysisReport, color = IronColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }

            // Diff 对比预览卡片（选择性替换 / 一键更新）
            res.diffProposal?.let { proposal ->
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = IronColors.SurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("计划调整提案（Diff 预览）", color = IronColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(proposal.summary, color = IronColors.TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        proposal.changes.forEach { change ->
                            val checked = selectedMap[change.id] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMap[change.id] = !checked }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { selectedMap[change.id] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = IronColors.PrimaryOrange)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(change.title, color = IronColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("原方案：${change.beforeDesc}", color = IronColors.TextSecondary, fontSize = 12.sp)
                                    Text("建议改：${change.afterDesc}", color = IronColors.AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("理由：${change.reason}", color = IronColors.TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val ids = selectedMap.filterValues { it }.keys
                                    scope.launch {
                                        engine.applyPlanDiff(proposal, ids).fold(
                                            onSuccess = { count -> statusMessage = "已选择性应用 $count 项调整（旧计划已自动备份快照）" },
                                            onFailure = { err -> statusMessage = "应用失败：${err.message}" }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("应用选中项", color = IronColors.TextPrimary)
                            }
                            Button(
                                onClick = {
                                    val allIds = proposal.changes.map { it.id }.toSet()
                                    scope.launch {
                                        engine.applyPlanDiff(proposal, allIds).fold(
                                            onSuccess = { count -> statusMessage = "已一键更新全部 $count 项调整（旧计划已自动备份快照）" },
                                            onFailure = { err -> statusMessage = "更新失败：${err.message}" }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("一键全部更新", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrillInterviewCard(
    question: InterviewQuestion,
    enabled: Boolean,
    onSubmitAnswer: (String) -> Unit
) {
    val multiSelections = remember(question.toolCallId) { mutableStateMapOf<String, Boolean>() }
    var showCustomInput by remember(question.toolCallId) { mutableStateOf(false) }
    var customText by remember(question.toolCallId) { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = IronColors.SurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, IronColors.PrimaryOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "GRILL-COACH 深度对齐 · 第 ${question.roundIndex} 问",
                color = IronColors.PrimaryOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = question.question,
                color = IronColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            question.options.forEach { option ->
                val isChecked = multiSelections[option.label] ?: false
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(
                            if (option.isRecommended) IronColors.PrimaryOrange.copy(alpha = 0.14f) else IronColors.Surface,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = enabled) {
                            if (question.allowMultiSelect) {
                                multiSelections[option.label] = !isChecked
                            } else {
                                onSubmitAnswer(option.label)
                            }
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (question.allowMultiSelect) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { multiSelections[option.label] = it },
                                colors = CheckboxDefaults.colors(checkedColor = IronColors.PrimaryOrange)
                            )
                        }
                        if (option.isRecommended) {
                            Text(
                                text = "[推荐] ",
                                color = IronColors.AccentGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = option.label,
                            color = IronColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    if (!option.reason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(option.reason, color = IronColors.TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            if (question.allowMultiSelect) {
                Button(
                    onClick = {
                        val picked = multiSelections.filterValues { it }.keys.joinToString("、")
                        if (picked.isNotBlank()) onSubmitAnswer(picked)
                    },
                    enabled = enabled && multiSelections.any { it.value },
                    colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认多选并继续", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (showCustomInput) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("输入您的具体情况...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { if (customText.isNotBlank()) onSubmitAnswer(customText.trim()) },
                    enabled = enabled && customText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("提交自定义回答", color = Color.White)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { showCustomInput = !showCustomInput }, enabled = enabled) {
                    Text("其他（自定义输入）", color = IronColors.TextSecondary, fontSize = 12.sp)
                }
                TextButton(
                    onClick = { onSubmitAnswer("跳过追问，请基于已有本地数据直接生成计划调整提案") },
                    enabled = enabled
                ) {
                    Text("跳过追问，直接生成方案", color = IronColors.AccentGreen, fontSize = 12.sp)
                }
            }
        }
    }
}
