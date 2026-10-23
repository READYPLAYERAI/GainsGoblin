package com.readyplayerai.gainsgoblin.data.db

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet

fun WorkoutSetEntity.toModel(): WorkoutSet =
    WorkoutSet(
        date,
        exerciseName,
        category,
        reps,
        weight,
        comment
    )

fun WorkoutSet.toEntity(
    createdAt: Long = System.currentTimeMillis(),
    id: Long = 0
): WorkoutSetEntity =
    WorkoutSetEntity(
        id = id,
        date = requireNotNull(date) { "WorkoutSet date is null" },
        exerciseName = requireNotNull(exerciseName) { "WorkoutSet exerciseName is null" },
        category = category,
        reps = requireNotNull(reps) { "WorkoutSet reps is null" },
        weight = requireNotNull(weight) { "WorkoutSet weight is null" },
        comment = comment,
        createdAt = createdAt
    )

fun ExerciseEntity.toModel(): Exercise =
    Exercise(name, bodyPart).also { it.favorite = favorite }

fun Exercise.toEntity(sortOrder: Int): ExerciseEntity =
    ExerciseEntity(
        name = requireNotNull(Name) { "Exercise name is null" },
        bodyPart = BodyPart,
        favorite = favorite ?: false,
        sortOrder = sortOrder
    )
