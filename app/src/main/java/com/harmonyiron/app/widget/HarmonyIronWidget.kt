package com.harmonyiron.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 鸿铁（HarmonyIron）Jetpack Glance 桌面微件：展示本周总容量与一键开练入口。
 */
class HarmonyIronWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = withContext(Dispatchers.IO) {
            val db = HarmonyIronDatabase.getInstance(context)
            val sessions = db.workoutSessionDao().getAllSessions()
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val weeklySessions = sessions.filter { it.startedAt >= weekAgo }
            val weeklyVolume = weeklySessions.sumOf { it.totalVolumeKg }
            Pair(weeklySessions.size, weeklyVolume)
        }

        provideContent {
            WidgetContent(
                weeklyCount = summary.first,
                weeklyVolumeKg = summary.second
            )
        }
    }

    @Composable
    private fun WidgetContent(weeklyCount: Int, weeklyVolumeKg: Double) {
        val openWorkoutIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("harmonyiron://workout/today")
        ).apply {
            setPackage("com.harmonyiron.app")
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF14161C)))
                .cornerRadius(16.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.Start,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "鸿铁 · 本周训练",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF9CA3AF)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "${weeklyVolumeKg.toInt()} kg",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "本周已完成 $weeklyCount 练",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF00E676)),
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0xFFFF5722)))
                    .cornerRadius(10.dp)
                    .padding(vertical = 8.dp)
                    .clickable(actionStartActivity(openWorkoutIntent)),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "一键开练",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFFFFFF)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class HarmonyIronWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HarmonyIronWidget()
}
