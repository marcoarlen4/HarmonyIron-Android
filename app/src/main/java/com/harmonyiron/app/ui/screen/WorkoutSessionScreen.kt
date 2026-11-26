package com.harmonyiron.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.data.engine.StrengthMath
import com.harmonyiron.app.data.entity.WorkoutSessionEntity
import com.harmonyiron.app.data.entity.WorkoutSetLogEntity
import com.harmonyiron.app.service.RestTimerService
import com.harmonyiron.app.ui.theme.IronColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ActiveExerciseUi(
    val exerciseId: Long,
    val name: String,
    val targetSets: Int,
    val restSeconds: Int,
    val setType: String,
    val completedSets: Int = 0,
    val lastWeightKg: Double = 60.0,
    val lastReps: Int = 8,
    val historicalMax1RM: Double = 0.0
)

/**
 * 练中沉浸式全屏页：历史重量预填、单手 +/- 快调、RPE 标记、组间锁屏倒计时联动。
 */
@Composable
fun WorkoutSessionScreen(
    planDayId: Long,
    dayTitle: String,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exercises = remember { mutableStateListOf<ActiveExerciseUi>() }
    var selectedIdx by remember { mutableIntStateOf(0) }
    var currentWeight by remember { mutableDoubleStateOf(60.0) }
    var currentReps by remember { mutableIntStateOf(8) }
    var currentRpe by remember { mutableFloatStateOf(8.0f) }
    var sessionId by remember { mutableStateOf<Long?>(null) }
    var sessionTotalVolume by remember { mutableDoubleStateOf(0.0) }
    var prBannerText by remember { mutableStateOf<String?>(null) }

    val restRemaining by RestTimerService.remainingSeconds.collectAsState()
    val isRestRunning by RestTimerService.isRunning.collectAsState()

    LaunchedEffect(planDayId) {
        withContext(Dispatchers.IO) {
            val db = HarmonyIronDatabase.getInstance(context)
            val allExercises = db.exerciseDao().getAll().associateBy { it.id }
            val items = db.workoutPlanDao().getItemsForDay(planDayId)
            val loaded = items.map { item ->
                val ex = allExercises[item.exerciseId]
                val latest = db.workoutSessionDao().getLatestLogForExercise(item.exerciseId)
                val history = db.workoutSessionDao().getHistoryForExercise(item.exerciseId)
                val max1RM = history.maxOfOrNull { StrengthMath.estimate1RM(it.weightKg, it.reps) } ?: 0.0
                ActiveExerciseUi(
                    exerciseId = item.exerciseId,
                    name = ex?.name ?: "未知动作",
                    targetSets = item.targetSets,
                    restSeconds = item.defaultRestSeconds,
                    setType = item.setType,
                    lastWeightKg = latest?.weightKg ?: 60.0,
                    lastReps = latest?.reps ?: item.targetReps,
                    historicalMax1RM = max1RM
                )
            }
            val newSessionId = db.workoutSessionDao().insertSession(
                WorkoutSessionEntity(planDayId = planDayId, title = dayTitle)
            )
            withContext(Dispatchers.Main) {
                sessionId = newSessionId
                exercises.clear()
                exercises.addAll(loaded)
                if (loaded.isNotEmpty()) {
                    currentWeight = loaded[0].lastWeightKg
                    currentReps = loaded[0].lastReps
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Background)
            .padding(16.dp)
    ) {
        // 顶部栏与结束训练按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("练中沉浸模式", color = IronColors.PrimaryOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(dayTitle, color = IronColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    val sid = sessionId
                    scope.launch(Dispatchers.IO) {
                        if (sid != null) {
                            HarmonyIronDatabase.getInstance(context).workoutSessionDao()
                                .finishSession(sid, System.currentTimeMillis(), sessionTotalVolume)
                        }
                        RestTimerService.skip(context)
                        withContext(Dispatchers.Main) { onFinishSession() }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IronColors.AccentGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("完成训练", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // 组间倒计时实时状态条
        if (isRestRunning && restRemaining > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IronColors.PrimaryOrange.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "组间休息倒计时：${restRemaining}s（锁屏已同步）",
                    color = IronColors.PrimaryOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                TextButton(onClick = { RestTimerService.skip(context) }) {
                    Text("跳过", color = IronColors.TextPrimary)
                }
            }
        }

        prBannerText?.let { banner ->
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IronColors.AccentGreen.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(banner, color = IronColors.AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 动作切换列表
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(exercises) { idx, item ->
                val isSelected = idx == selectedIdx
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) IronColors.SurfaceElevated else IronColors.Surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable {
                            selectedIdx = idx
                            currentWeight = item.lastWeightKg
                            currentReps = item.lastReps
                            prBannerText = null
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, color = IronColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "上次参考：${item.lastWeightKg} kg × ${item.lastReps} 次 | 休息 ${item.restSeconds}s",
                                color = IronColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "${item.completedSets}/${item.targetSets} 组",
                            color = if (item.completedSets >= item.targetSets) IronColors.AccentGreen else IronColors.PrimaryOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 底部单手拇指热区快调面板
        val active = exercises.getOrNull(selectedIdx)
        if (active != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "当前记录：${active.name}（第 ${active.completedSets + 1} 组）",
                        color = IronColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 重量与次数单手 +/- 调节
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StepperControl(
                            label = "重量 (kg)",
                            valueText = String.format("%.1f", currentWeight),
                            onMinus = { currentWeight = (currentWeight - 2.5).coerceAtLeast(0.0) },
                            onPlus = { currentWeight += 2.5 },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        StepperControl(
                            label = "次数 (reps)",
                            valueText = "$currentReps",
                            onMinus = { currentReps = (currentReps - 1).coerceAtLeast(1) },
                            onPlus = { currentReps += 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // RPE 快捷选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RPE 强度", color = IronColors.TextSecondary, fontSize = 12.sp)
                        listOf(7.0f, 8.0f, 9.0f, 10.0f).forEach { rpe ->
                            val selected = currentRpe == rpe
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) IronColors.PrimaryOrange else IronColors.SurfaceElevated,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { currentRpe = rpe }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (rpe == 10.0f) "10(力竭)" else "${rpe.toInt()}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val sid = sessionId ?: return@Button
                            val isPr = StrengthMath.isNewPR(currentWeight, currentReps, active.historicalMax1RM)
                            val setVolume = StrengthMath.calculateSetVolume(currentWeight, currentReps)
                            val nextSetNumber = active.completedSets + 1

                            scope.launch(Dispatchers.IO) {
                                HarmonyIronDatabase.getInstance(context).workoutSessionDao().insertSetLog(
                                    WorkoutSetLogEntity(
                                        sessionId = sid,
                                        exerciseId = active.exerciseId,
                                        setNumber = nextSetNumber,
                                        weightKg = currentWeight,
                                        reps = currentReps,
                                        rpe = currentRpe,
                                        setType = active.setType,
                                        isPr = isPr
                                    )
                                )
                            }

                            sessionTotalVolume += setVolume
                            val updated1RM = maxOf(active.historicalMax1RM, StrengthMath.estimate1RM(currentWeight, currentReps))
                            exercises[selectedIdx] = active.copy(
                                completedSets = nextSetNumber,
                                lastWeightKg = currentWeight,
                                lastReps = currentReps,
                                historicalMax1RM = updated1RM
                            )
                            if (isPr) {
                                prBannerText = "新纪录 PR！${active.name} 估算 1RM 提升至 ${updated1RM} kg"
                            }
                            RestTimerService.start(context, active.restSeconds, active.name)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("完成本组 · 开启 ${active.restSeconds}s 倒计时", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperControl(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(IronColors.SurfaceElevated, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = IronColors.TextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(IronColors.Background, RoundedCornerShape(8.dp))
                    .clickable { onMinus() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("-", color = IronColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(valueText, color = IronColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Box(
                modifier = Modifier
                    .background(IronColors.Background, RoundedCornerShape(8.dp))
                    .clickable { onPlus() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("+", color = IronColors.PrimaryOrange, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
