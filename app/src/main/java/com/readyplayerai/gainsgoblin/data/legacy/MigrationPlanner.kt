package com.readyplayerai.gainsgoblin.data.legacy

import com.readyplayerai.gainsgoblin.data.db.ExerciseEntity
import com.readyplayerai.gainsgoblin.data.db.WorkoutSetEntity
import com.readyplayerai.gainsgoblin.data.db.toEntity

data class PlannedMigration(
    val sets: List<WorkoutSetEntity>,
    val exercises: List<ExerciseEntity>
)

object MigrationPlanner {
    fun plan(snapshot: LegacySnapshot, now: Long = System.currentTimeMillis()): PlannedMigration {
        val sets = ArrayList<WorkoutSetEntity>()
        var index = 0
        for (day in snapshot.days) {
            val daySets = day.Sets ?: continue
            for (set in daySets) {
                sets.add(set.toEntity(createdAt = now + index))
                index++
            }
        }

        val exercises = ArrayList<ExerciseEntity>(snapshot.knownExercises.size)
        for (i in snapshot.knownExercises.indices) {
            exercises.add(snapshot.knownExercises[i].toEntity(sortOrder = i))
        }

        return PlannedMigration(sets, exercises)
    }
}
