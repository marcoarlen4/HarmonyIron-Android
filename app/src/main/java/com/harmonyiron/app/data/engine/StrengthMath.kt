package com.harmonyiron.app.data.engine

import com.harmonyiron.app.data.entity.WorkoutSetLogEntity
import kotlin.math.roundToInt

/**
 * 力量训练科学计算引擎：1RM 估算、训练容量（Volume）与 PR 里程碑判定。
 */
object StrengthMath {

    /**
     * 基于 Epley 公式估算单次最大极限重量（1RM）。
     * 当次数 <= 0 或重量 <= 0 时返回 0.0；当次数为 1 时直接返回当前重量。
     */
    fun estimate1RM(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        val raw = weightKg * (1.0 + reps / 30.0)
        return (raw * 10.0).roundToInt() / 10.0
    }

    /**
     * 计算单组训练容量（Volume = 重量 × 次数）。
     */
    fun calculateSetVolume(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        return weightKg * reps
    }

    /**
     * 汇总多组训练总容量（剔除无效数据）。
     */
    fun calculateTotalVolume(logs: List<WorkoutSetLogEntity>): Double {
        return logs.sumOf { calculateSetVolume(it.weightKg, it.reps) }
    }

    /**
     * 判断当前完成组是否刷新历史 1RM 个人纪录（PR）。
     */
    fun isNewPR(currentWeightKg: Double, currentReps: Int, historicalMax1RM: Double): Boolean {
        val current1RM = estimate1RM(currentWeightKg, currentReps)
        return current1RM > 0.0 && current1RM > historicalMax1RM
    }
}
