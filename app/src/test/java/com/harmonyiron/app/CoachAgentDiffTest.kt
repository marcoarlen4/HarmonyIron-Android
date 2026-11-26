package com.harmonyiron.app

import com.harmonyiron.app.agent.CoachAgentEngine
import com.harmonyiron.app.agent.LocalCoachTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachAgentDiffTest {

    @Test
    fun parsePlanDiffProposal_parsesValidChanges() {
        val rawJson = """
            {
              "summary": "左肩不适避让大重量杠铃推举，更换为上斜哑铃卧推",
              "changes": [
                {
                  "id": "c1",
                  "actionType": "REPLACE_EXERCISE",
                  "planDayId": 1,
                  "targetItemId": 3,
                  "newExerciseId": 2,
                  "newSets": 3,
                  "newReps": 10,
                  "title": "替换站姿杠铃推举",
                  "beforeDesc": "杠铃站姿推举 3x8",
                  "afterDesc": "上斜哑铃卧推 3x10",
                  "reason": "降低肩峰撞击压力"
                },
                {
                  "id": "c2",
                  "actionType": "UPDATE_PARAMS",
                  "planDayId": 1,
                  "targetItemId": 1,
                  "newSets": 3,
                  "newReps": 8,
                  "title": "平板卧推减载",
                  "beforeDesc": "4x6",
                  "afterDesc": "3x8",
                  "reason": "控制 RPE 疲劳"
                }
              ]
            }
        """.trimIndent()

        val result = LocalCoachTools.parsePlanDiffProposal(rawJson)
        assertTrue(result.isSuccess)
        val proposal = result.getOrThrow()
        assertEquals(2, proposal.changes.size)

        val selected = CoachAgentEngine.filterSelectedChanges(proposal, setOf("c1"))
        assertEquals(1, selected.size)
        assertEquals("REPLACE_EXERCISE", selected.first().actionType)
    }

    @Test
    fun parsePlanDiffProposal_rejectsEmptyChanges() {
        val emptyJson = """{"summary": "无需调整", "changes": []}"""
        val result = LocalCoachTools.parsePlanDiffProposal(emptyJson)
        assertTrue(result.isFailure)
    }
}
