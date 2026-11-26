package com.harmonyiron.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonyiron.app.data.backup.HarmonyIronBackupPayload
import com.harmonyiron.app.data.backup.JsonBackupManager
import com.harmonyiron.app.data.db.HarmonyIronDatabase
import com.harmonyiron.app.ui.theme.IronColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Background)
            .padding(20.dp)
    ) {
        Text("设置与隐私", color = IronColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = IronColors.Surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("纯本地数据安全", color = IronColors.AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "鸿铁（HarmonyIron）V1.0 采用纯本地数据库存储所有训练记录，零广告、零社交上传，并支持与鸿蒙端互通的标准化 JSON 备份。",
                    color = IronColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val db = HarmonyIronDatabase.getInstance(context)
                            val payload = HarmonyIronBackupPayload(
                                exercises = db.exerciseDao().getAll(),
                                plans = db.workoutPlanDao().getAllPlans(),
                                sessions = db.workoutSessionDao().getAllSessions()
                            )
                            val jsonStr = JsonBackupManager.exportToJson(payload)
                            val outFile = File(context.getExternalFilesDir(null), "harmony_iron_backup.json")
                            outFile.writeText(jsonStr)
                            withContext(Dispatchers.Main) {
                                exportStatus = "已导出至：${outFile.absolutePath}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IronColors.PrimaryOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("导出双端通用 JSON 备份", color = Color.White, fontWeight = FontWeight.Bold)
                }

                exportStatus?.let { status ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(status, color = IronColors.AccentGreen, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("当前版本：V1.0.0 (MVP) · V1.1 将上线端侧 AI 力量教练", color = IronColors.TextSecondary, fontSize = 12.sp)
    }
}
