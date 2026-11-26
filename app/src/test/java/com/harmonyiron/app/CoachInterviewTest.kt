package com.harmonyiron.app

import com.harmonyiron.app.agent.LocalCoachTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachInterviewTest {

    @Test
    fun parseInterviewQuestionArgs_parsesRecommendedAndOptions() {
        val rawJson = """
            {
              "question": "检测到你近 3 周硬拉 RPE 持续达到 9.5，本周是否需要安排减载？",
              "allowMultiSelect": false,
              "options": [
                {
                  "label": "切入 1 周减载期（容量降为 60%）",
                  "isRecommended": true,
                  "reason": "近 3 周硬拉 RPE ≥ 9.5 且 1RM 停滞"
                },
                {
                  "label": "保持当前强度继续冲击",
                  "isRecommended": false
                }
              ]
            }
        """.trimIndent()

        val result = LocalCoachTools.parseInterviewQuestionArgs(rawJson)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertFalse(parsed.allowMultiSelect)
        assertEquals(2, parsed.options.size)
        assertTrue(parsed.options.first().isRecommended)
    }

    @Test
    fun parseInterviewQuestionArgs_rejectsEmptyOptions() {
        val invalidJson = """{"question": "测试问题", "options": []}"""
        val result = LocalCoachTools.parseInterviewQuestionArgs(invalidJson)
        assertTrue(result.isFailure)
    }
}
