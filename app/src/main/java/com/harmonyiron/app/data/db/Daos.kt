package com.harmonyiron.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.harmonyiron.app.data.entity.CoachProfileEntity
import com.harmonyiron.app.data.entity.ExerciseEntity
import com.harmonyiron.app.data.entity.PlanDayEntity
import com.harmonyiron.app.data.entity.PlanItemEntity
import com.harmonyiron.app.data.entity.PlanSnapshotEntity
import com.harmonyiron.app.data.entity.WorkoutPlanEntity
import com.harmonyiron.app.data.entity.WorkoutSessionEntity
import com.harmonyiron.app.data.entity.WorkoutSetLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY isCustom ASC, id ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY id ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long
}

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY isActive DESC, createdAt DESC")
    fun observePlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans ORDER BY id ASC")
    suspend fun getAllPlans(): List<WorkoutPlanEntity>

    @Query("SELECT * FROM plan_days WHERE planId = :planId ORDER BY dayOrder ASC")
    suspend fun getDaysForPlan(planId: Long): List<PlanDayEntity>

    @Query("SELECT * FROM plan_items WHERE planDayId = :planDayId ORDER BY sortOrder ASC")
    suspend fun getItemsForDay(planDayId: Long): List<PlanItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanDay(day: PlanDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItems(items: List<PlanItemEntity>)

    @Update
    suspend fun updatePlanItem(item: PlanItemEntity)

    @Query("DELETE FROM plan_items WHERE id = :itemId")
    suspend fun deletePlanItemById(itemId: Long)

    @Query("DELETE FROM plan_items WHERE planDayId = :planDayId")
    suspend fun deleteItemsForDay(planDayId: Long)

    @Transaction
    suspend fun replaceItemsForDay(planDayId: Long, newItems: List<PlanItemEntity>) {
        deleteItemsForDay(planDayId)
        insertPlanItems(newItems)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PlanSnapshotEntity): Long

    @Query("SELECT * FROM plan_snapshots WHERE planId = :planId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSnapshot(planId: Long): PlanSnapshotEntity?
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_set_logs WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    suspend fun getLogsForSession(sessionId: Long): List<WorkoutSetLogEntity>

    @Query("SELECT * FROM workout_set_logs WHERE exerciseId = :exerciseId ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLatestLogForExercise(exerciseId: Long): WorkoutSetLogEntity?

    @Query("SELECT * FROM workout_set_logs WHERE exerciseId = :exerciseId ORDER BY completedAt ASC")
    suspend fun getHistoryForExercise(exerciseId: Long): List<WorkoutSetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(log: WorkoutSetLogEntity): Long

    @Query("UPDATE workout_sessions SET finishedAt = :finishedAt, totalVolumeKg = :totalVolume WHERE id = :sessionId")
    suspend fun finishSession(sessionId: Long, finishedAt: Long, totalVolume: Double)
}

@Dao
interface CoachProfileDao {
    @Query("SELECT * FROM coach_profile WHERE id = 1")
    suspend fun getProfile(): CoachProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CoachProfileEntity)
}
