package com.harmonyiron.app

import com.harmonyiron.app.data.engine.StrengthMath
import com.harmonyiron.app.data.entity.WorkoutSetLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthMathTest {

    @Test
    fun estimate1RM_handlesSingleRepAndInvalidInputs() {
        assertEquals(100.0, StrengthMath.estimate1RM(100.0, 1), 0.01)
        assertEquals(0.0, StrengthMath.estimate1RM(0.0, 5), 0.01)
        assertEquals(0.0, StrengthMath.estimate1RM(80.0, 0), 0.01)
        // Epley: 100 * (1 + 6/30) = 120.0
        assertEquals(120.0, StrengthMath.estimate1RM(100.0, 6), 0.01)
    }

    @Test
    fun calculateTotalVolume_sumsValidSets() {
        val logs = listOf(
            WorkoutSetLogEntity(sessionId = 1, exerciseId = 1, setNumber = 1, weightKg = 100.0, reps = 5),
            WorkoutSetLogEntity(sessionId = 1, exerciseId = 1, setNumber = 2, weightKg = 100.0, reps = 5),
            WorkoutSetLogEntity(sessionId = 1, exerciseId = 1, setNumber = 3, weightKg = -10.0, reps = 5)
        )
        assertEquals(1000.0, StrengthMath.calculateTotalVolume(logs), 0.01)
    }

    @Test
    fun isNewPR_detectsImprovementCorrectly() {
        assertTrue(StrengthMath.isNewPR(100.0, 6, 115.0))
        assertFalse(StrengthMath.isNewPR(100.0, 5, 120.0))
    }
}
