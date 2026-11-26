package com.harmonyiron.app.data.backup

import com.harmonyiron.app.data.entity.CoachProfileEntity
import com.harmonyiron.app.data.entity.ExerciseEntity
import com.harmonyiron.app.data.entity.WorkoutPlanEntity
import com.harmonyiron.app.data.entity.WorkoutSessionEntity
import com.harmonyiron.app.data.entity.WorkoutSetLogEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 鸿铁（HarmonyIron）双端通用标准化 JSON 备份载荷，支持鸿蒙与安卓间无缝数据迁移。
 */
@Serializable
data class HarmonyIronBackupPayload(
    val schemaVersion: Int = 1,
    val platform: String = "android",
    val exportedAt: Long = System.currentTimeMillis(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val plans: List<WorkoutPlanEntity> = emptyList(),
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val setLogs: List<WorkoutSetLogEntity> = emptyList(),
    val coachProfile: CoachProfileEntity? = null
)

object JsonBackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportToJson(payload: HarmonyIronBackupPayload): String {
        return json.encodeToString(payload)
    }

    fun importFromJson(rawJson: String): Result<HarmonyIronBackupPayload> {
        return runCatching {
            val parsed = json.decodeFromString<HarmonyIronBackupPayload>(rawJson)
            require(parsed.schemaVersion >= 1) { "不支持的备份文件版本: ${parsed.schemaVersion}" }
            parsed
        }
    }
}
