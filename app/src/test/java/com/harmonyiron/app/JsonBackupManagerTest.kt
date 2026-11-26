package com.harmonyiron.app

import com.harmonyiron.app.data.backup.HarmonyIronBackupPayload
import com.harmonyiron.app.data.backup.JsonBackupManager
import com.harmonyiron.app.data.entity.ExerciseEntity
import com.harmonyiron.app.data.entity.WorkoutPlanEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonBackupManagerTest {

    @Test
    fun exportAndImport_roundTripPreservesData() {
        val payload = HarmonyIronBackupPayload(
            schemaVersion = 1,
            platform = "android",
            exportedAt = 1732600000000L,
            exercises = listOf(
                ExerciseEntity(id = 1, name = "杠铃平板卧推", muscleGroup = "胸", equipment = "杠铃")
            ),
            plans = listOf(
                WorkoutPlanEntity(id = 1, name = "推拉腿", isActive = true)
            )
        )

        val jsonStr = JsonBackupManager.exportToJson(payload)
        val restored = JsonBackupManager.importFromJson(jsonStr)

        assertTrue(restored.isSuccess)
        val parsed = restored.getOrThrow()
        assertEquals(1, parsed.exercises.size)
        assertEquals("杠铃平板卧推", parsed.exercises.first().name)
        assertEquals("推拉腿", parsed.plans.first().name)
    }

    @Test
    fun importFromJson_rejectsInvalidSchemaVersion() {
        val invalidJson = """{"schemaVersion": 0, "platform": "android"}"""
        val result = JsonBackupManager.importFromJson(invalidJson)
        assertTrue(result.isFailure)
    }
}
