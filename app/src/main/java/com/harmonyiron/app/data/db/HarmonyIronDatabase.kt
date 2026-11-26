package com.harmonyiron.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.harmonyiron.app.data.entity.CoachProfileEntity
import com.harmonyiron.app.data.entity.ExerciseEntity
import com.harmonyiron.app.data.entity.PlanDayEntity
import com.harmonyiron.app.data.entity.PlanItemEntity
import com.harmonyiron.app.data.entity.PlanSnapshotEntity
import com.harmonyiron.app.data.entity.WorkoutPlanEntity
import com.harmonyiron.app.data.entity.WorkoutSessionEntity
import com.harmonyiron.app.data.entity.WorkoutSetLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        PlanDayEntity::class,
        PlanItemEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetLogEntity::class,
        PlanSnapshotEntity::class,
        CoachProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HarmonyIronDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun coachProfileDao(): CoachProfileDao

    companion object {
        @Volatile
        private var INSTANCE: HarmonyIronDatabase? = null

        fun getInstance(context: Context): HarmonyIronDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HarmonyIronDatabase::class.java,
                    "harmony_iron.db"
                )
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    /**
     * 首次创建数据库时写入核心力量动作库与默认“推拉腿三分化”计划。
     */
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val exercises = listOf(
                        ExerciseEntity(id = 1, name = "杠铃平板卧推", muscleGroup = "胸", equipment = "杠铃"),
                        ExerciseEntity(id = 2, name = "上斜哑铃卧推", muscleGroup = "胸", equipment = "哑铃"),
                        ExerciseEntity(id = 3, name = "杠铃站姿推举", muscleGroup = "肩", equipment = "杠铃"),
                        ExerciseEntity(id = 4, name = "哑铃侧平举", muscleGroup = "肩", equipment = "哑铃"),
                        ExerciseEntity(id = 5, name = "高位下拉", muscleGroup = "背", equipment = "固定器械"),
                        ExerciseEntity(id = 6, name = "杠铃俯身划船", muscleGroup = "背", equipment = "杠铃"),
                        ExerciseEntity(id = 7, name = "杠铃深蹲", muscleGroup = "腿", equipment = "杠铃"),
                        ExerciseEntity(id = 8, name = "罗马尼亚硬拉", muscleGroup = "腿", equipment = "杠铃")
                    )
                    database.exerciseDao().insertAll(exercises)

                    val planId = database.workoutPlanDao().insertPlan(
                        WorkoutPlanEntity(
                            name = "经典推拉腿三分化（PPL）",
                            description = "适合每周 3–5 练的进阶渐进超负荷计划",
                            isActive = true
                        )
                    )
                    val pushDayId = database.workoutPlanDao().insertPlanDay(
                        PlanDayEntity(planId = planId, dayOrder = 1, title = "推力日（胸 / 肩 / 三头）")
                    )
                    val pullDayId = database.workoutPlanDao().insertPlanDay(
                        PlanDayEntity(planId = planId, dayOrder = 2, title = "拉力日（背 / 二头）")
                    )
                    val legDayId = database.workoutPlanDao().insertPlanDay(
                        PlanDayEntity(planId = planId, dayOrder = 3, title = "下肢日（股四 / 腘绳）")
                    )

                    database.workoutPlanDao().insertPlanItems(
                        listOf(
                            PlanItemEntity(planDayId = pushDayId, exerciseId = 1, sortOrder = 1, targetSets = 4, targetReps = 6, defaultRestSeconds = 120),
                            PlanItemEntity(planDayId = pushDayId, exerciseId = 2, sortOrder = 2, targetSets = 3, targetReps = 10, defaultRestSeconds = 90),
                            PlanItemEntity(planDayId = pushDayId, exerciseId = 3, sortOrder = 3, targetSets = 3, targetReps = 8, defaultRestSeconds = 90),
                            PlanItemEntity(planDayId = pushDayId, exerciseId = 4, sortOrder = 4, targetSets = 4, targetReps = 12, defaultRestSeconds = 60, setType = "DROP_SET"),
                            PlanItemEntity(planDayId = pullDayId, exerciseId = 6, sortOrder = 1, targetSets = 4, targetReps = 8, defaultRestSeconds = 120),
                            PlanItemEntity(planDayId = pullDayId, exerciseId = 5, sortOrder = 2, targetSets = 4, targetReps = 10, defaultRestSeconds = 90),
                            PlanItemEntity(planDayId = legDayId, exerciseId = 7, sortOrder = 1, targetSets = 4, targetReps = 6, defaultRestSeconds = 150),
                            PlanItemEntity(planDayId = legDayId, exerciseId = 8, sortOrder = 2, targetSets = 3, targetReps = 8, defaultRestSeconds = 120)
                        )
                    )
                }
            }
        }
    }
}
