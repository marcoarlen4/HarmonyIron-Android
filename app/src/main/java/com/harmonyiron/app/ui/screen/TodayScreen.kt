package com.harmonyiron.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.data.entity.PlanDayEntity
import com.harmonyiron.app.ui.theme.IronColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TodayScreen(
    onStartWorkout: (dayId: Long, dayTitle: String) -> Unit
) {
    val context = LocalContext.current
    var activePlanName by remember { mutableStateOf("经典推拉腿三分化（PPL）") }
    var planDays by remember { mutableStateOf<List<PlanDayEntity>>(emptyList()) }
    var weeklySessionsCount by remember { mutableStateOf(0) }
    var weeklyVolumeKg by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = HarmonyIronDatabase.getInstance(context)
            val plans = db.workoutPlanDao().getAllPlans()
            val active = plans.firstOrNull { it.isActive } ?: plans.firstOrNull()
            if (active != null) {
                activePlanName = active.name
                planDays = db.workoutPlanDao().getDaysForPlan(active.id)
            }
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val recent = db.workoutSessionDao().getAllSessions().filter { it.startedAt >= weekAgo }
            weeklySessionsCount = recent.size
            weeklyVolumeKg = recent.sumOf { it.totalVolumeKg }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Background)
            .padding(20.dp)
    ) {
        Text(
            text = "鸿铁 · HARMONY IRON",
            color = IronColors.PrimaryOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "准备开练",
            color = IronColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 本周总容量概览卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("本周总容量", color = IronColors.TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${weeklyVolumeKg.toInt()} kg",
                        color = IronColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("本周已完成", color = IronColors.TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$weeklySessionsCount 练",
                        color = IronColors.AccentGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "当前周期：$activePlanName",
            color = IronColors.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        planDays.forEach { day ->
            Card(
                colors = CardDefaults.cardColors(containerColor = IronColors.SurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAY ${day.dayOrder}",
                            color = IronColors.PrimaryOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = day.title,
                            color = IronColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { onStartWorkout(day.id, day.title) },
                        colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("开始", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
