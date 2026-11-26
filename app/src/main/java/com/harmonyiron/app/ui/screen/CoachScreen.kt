package com.harmonyiron.app.ui.screen

import androidx.compose.foundation.background
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
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.ui.theme.IronColors
import kotlinx.coroutines.launch

/**
 * AI 力量教练主页：一键周期复盘、自然语言追问、Diff 预览选择性替换 / 一键更新与快照回滚。
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

    fun triggerAnalysis(query: String) {
        if (isLoading) return
        isLoading = true
        statusMessage = null
        scope.launch {
            engine.runAnalysis(query).fold(
                onSuccess = { res ->
                    agentResponse = res
                    selectedMap.clear()
                    res.diffProposal?.changes?.forEach { item ->
                        selectedMap[item.id] = true
                    }
                },
                onFailure = { err ->
                    statusMessage = "分析失败：${err.message}"
                }
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
                Text("端侧本地数据驱动 · 安全 Diff 计划更新", color = IronColors.TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置与 BYOK", tint = IronColors.TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 快捷操作栏
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { triggerAnalysis("请结合我近14天的本地训练容量与动作数据，做一次周期复盘诊断，如有必要请给出计划调整提案。") },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("一键周复盘", color = Color.White, fontWeight = FontWeight.Bold)
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
                Text("回滚上一版", color = IronColors.TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 自然语言追问输入框
        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            placeholder = { Text("例如：左肩不适，帮我把推胸动作换成低风险的", fontSize = 13.sp) },
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
                Text("正在调用本地数据工具进行分析...", color = IronColors.TextSecondary, fontSize = 13.sp)
            }
        }

        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(msg, color = IronColors.AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        agentResponse?.let { res ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (res.executedTools.isNotEmpty()) {
                        Text(
                            "已读取本地工具：${res.executedTools.joinToString(", ")}",
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
