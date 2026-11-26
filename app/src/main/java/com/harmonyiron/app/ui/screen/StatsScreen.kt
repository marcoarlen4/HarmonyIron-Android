package com.harmonyiron.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.data.engine.StrengthMath
import com.harmonyiron.app.ui.theme.IronColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据分析页面：轻量 Compose Canvas 自绘训练容量柱状图与核心动作 1RM 趋势折线图。
 */
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    var recentVolumes by remember { mutableStateOf<List<Float>>(emptyList()) }
    var bench1RMHistory by remember { mutableStateOf<List<Float>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = HarmonyIronDatabase.getInstance(context)
            val sessions = db.workoutSessionDao().getAllSessions().take(7).reversed()
            recentVolumes = sessions.map { it.totalVolumeKg.toFloat() }
            val benchLogs = db.workoutSessionDao().getHistoryForExercise(1L).takeLast(10)
            bench1RMHistory = benchLogs.map { StrengthMath.estimate1RM(it.weightKg, it.reps).toFloat() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Background)
            .padding(20.dp)
    ) {
        Text("数据分析", color = IronColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("渐进超负荷与 1RM 力量趋势追踪", color = IronColors.TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("近期单次训练容量 (kg)", color = IronColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                val barData = if (recentVolumes.isEmpty()) listOf(3200f, 3600f, 4100f, 3900f, 4500f) else recentVolumes
                VolumeBarChart(values = barData, modifier = Modifier.fillMaxWidth().height(140.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("杠铃平板卧推 · 估算 1RM 趋势 (kg)", color = IronColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                val lineData = if (bench1RMHistory.size < 2) listOf(80f, 82.5f, 85f, 85f, 90f) else bench1RMHistory
                OneRmLineChart(values = lineData, modifier = Modifier.fillMaxWidth().height(140.dp))
            }
        }
    }
}

@Composable
private fun VolumeBarChart(values: List<Float>, modifier: Modifier = Modifier) {
    val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        val count = values.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = slotWidth * 0.52f
        values.forEachIndexed { index, value ->
            val ratio = (value / maxVal).coerceIn(0.05f, 1f)
            val barHeight = size.height * ratio
            val left = index * slotWidth + (slotWidth - barWidth) / 2f
            val top = size.height - barHeight
            drawRoundRect(
                color = IronColors.PrimaryOrange,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }
    }
}

@Composable
private fun OneRmLineChart(values: List<Float>, modifier: Modifier = Modifier) {
    val minVal = values.minOrNull() ?: 0f
    val maxVal = values.maxOrNull() ?: 100f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = (value - minVal) / range
            val y = size.height - (normalizedY * size.height * 0.8f + size.height * 0.1f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = IronColors.AccentGreen, radius = 8f, center = Offset(x, y))
        }
        drawPath(
            path = path,
            color = IronColors.AccentGreen,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
    }
}
