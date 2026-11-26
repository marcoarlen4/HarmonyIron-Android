package com.harmonyiron.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.data.entity.WorkoutPlanEntity
import com.harmonyiron.app.ui.theme.IronColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlansScreen() {
    val context = LocalContext.current
    var plans by remember { mutableStateOf<List<WorkoutPlanEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            plans = HarmonyIronDatabase.getInstance(context).workoutPlanDao().getAllPlans()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Background)
            .padding(20.dp)
    ) {
        Text("计划库", color = IronColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("支持推拉腿、超级组与递减组周期编排", color = IronColors.TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(plans) { plan ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (plan.isActive) "当前启用中" else "备选计划",
                            color = if (plan.isActive) IronColors.AccentGreen else IronColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(plan.name, color = IronColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        if (plan.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(plan.description, color = IronColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
