package com.readyplayerai.gainsgoblin.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** Room database holding workout sets, exercises, and key/value metadata. */
@Database(
    entities = [
        WorkoutSetEntity::class,
        ExerciseEntity::class,
        MetaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GainsGoblinDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
