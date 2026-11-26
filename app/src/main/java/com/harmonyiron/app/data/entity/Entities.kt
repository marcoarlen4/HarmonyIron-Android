package com.harmonyiron.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 力量训练动作实体（内置动作库 + 用户自定义动作）。
 */
@Serializable
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String, // 胸 / 背 / 腿 / 肩 / 手臂 / 核心
    val equipment: String,   // 杠铃 / 哑铃 / 固定器械 / 绳索 / 徒手
    val isCustom: Boolean = false
)

/**
 * 训练分化计划主表（如：推拉腿三分化、五分化等）。
 */
@Serializable
@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 训练计划中的单日分化（如：Day 1 推力日）。
 */
@Serializable
@Entity(
    tableName = "plan_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class PlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayOrder: Int,
    val title: String
)

/**
 * 单日计划中的动作编排项（支持超级组 supersetGroupId 与递减组 setType）。
 */
@Serializable
@Entity(
    tableName = "plan_items",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planDayId"), Index("exerciseId")]
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: Int,
    val defaultRestSeconds: Int = 90,
    val setType: String = "NORMAL",      // NORMAL / WARMUP / DROP_SET
    val supersetGroupId: String? = null  // 相同非空标识表示同一超级组
)

/**
 * 单次训练会话记录。
 */
@Serializable
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayId: Long? = null,
    val title: String,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val totalVolumeKg: Double = 0.0
)

/**
 * 单组实际完成记录（包含重量、次数、RPE 疲劳度及是否达成 PR）。
 */
@Serializable
@Entity(
    tableName = "workout_set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutSetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Float? = null,
    val setType: String = "NORMAL",
    val isPr: Boolean = false,
    val completedAt: Long = System.currentTimeMillis()
)

/**
 * V1.1 预留：AI 教练更新计划前自动保存的历史快照表，支持一键回滚。
 */
@Serializable
@Entity(tableName = "plan_snapshots")
data class PlanSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val snapshotJson: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * V1.2 预留：Grill-Coach 深度访谈沉淀的用户教练档案（伤病史、时长限制、器械偏好）。
 */
@Serializable
@Entity(tableName = "coach_profile")
data class CoachProfileEntity(
    @PrimaryKey val id: Int = 1,
    val injuryNotes: String = "",
    val maxSessionMinutes: Int = 60,
    val preferredEquipment: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
