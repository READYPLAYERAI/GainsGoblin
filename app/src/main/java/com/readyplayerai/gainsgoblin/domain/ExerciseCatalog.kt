package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import java.util.ArrayList

/**
 * Returns the 36 hardcoded exercises from the legacy `initKnownExercises()`.
 *
 * Names, body parts and the misspelling "Hammstring Leg Curl" are preserved verbatim.
 * Every exercise starts with `favorite = false`.
 */
fun defaultKnownExercises(): List<Exercise> {
    val exercises = ArrayList<Exercise>()
    exercises.add(Exercise("Flat Barbell Bench Press", "Chest"))
    exercises.add(Exercise("Incline Barbell Bench Press", "Chest"))
    exercises.add(Exercise("Decline Barbell Bench Press", "Chest"))
    exercises.add(Exercise("Flat Dumbbell Bench Press", "Chest"))
    exercises.add(Exercise("Incline Dumbbell Bench Press", "Chest"))
    exercises.add(Exercise("Decline Dumbbell Bench Press", "Chest"))
    exercises.add(Exercise("Chin Up", "Back"))
    exercises.add(Exercise("Seated Dumbbell Press", "Shoulders"))
    exercises.add(Exercise("Ring Dip", "Chest"))
    exercises.add(Exercise("Lateral Cable Raise", "Shoulders"))
    exercises.add(Exercise("Lateral Dumbbell Raise", "Shoulders"))
    exercises.add(Exercise("Barbell Curl", "Biceps"))
    exercises.add(Exercise("Tricep Extension", "Triceps"))
    exercises.add(Exercise("Squat", "Legs"))
    exercises.add(Exercise("Leg Extension", "Legs"))
    exercises.add(Exercise("Hammstring Leg Curl", "Legs"))
    exercises.add(Exercise("Deadlift", "Back"))
    exercises.add(Exercise("Sumo Deadlift", "Back"))
    exercises.add(Exercise("Seated Machine Chest Press", "Chest"))
    exercises.add(Exercise("Seated Machine Shoulder Press", "Shoulders"))
    exercises.add(Exercise("Seated Calf Raise", "Legs"))
    exercises.add(Exercise("Donkey Calf Raise", "Legs"))
    exercises.add(Exercise("Standing Calf Raise", "Legs"))
    exercises.add(Exercise("Seated Machine Curl", "Biceps"))
    exercises.add(Exercise("Lat Pulldown", "Back"))
    exercises.add(Exercise("Pull Up", "Back"))
    exercises.add(Exercise("Push Up", "Chest"))
    exercises.add(Exercise("Leg Press", "Legs"))
    exercises.add(Exercise("Push Press", "Shoulders"))
    exercises.add(Exercise("Dumbbell Curl", "Biceps"))
    exercises.add(Exercise("Decline Hammer Strength Chest Press", "Chest"))
    exercises.add(Exercise("Leg Extension Machine", "Legs"))
    exercises.add(Exercise("Seated Calf Raise Machine", "Legs"))
    exercises.add(Exercise("Lying Triceps Extension", "Triceps"))
    exercises.add(Exercise("Cable Curl", "Biceps"))
    exercises.add(Exercise("Hammer Strength Shoulder Press", "Shoulders"))
    return exercises
}

/**
 * Appends exercises found in [days] that are not already known by name.
 *
 * Pure port of the legacy `csvToKnownExercises()`. Sets are visited in day/list
 * order; the returned list starts with the original [knownExercises] entries in
 * their original order, followed by new unique names in first-seen order. Input
 * lists and entries are not modified.
 */
fun dedupeKnownExercises(
    days: List<WorkoutDay>,
    knownExercises: List<Exercise>
): List<Exercise> {
    val found = ArrayList<Exercise>()
    for (day in days) {
        val sets = day.Sets ?: continue
        for (workoutSet in sets) {
            found.add(Exercise(workoutSet.exerciseName, workoutSet.category))
        }
    }

    val result = ArrayList(knownExercises)
    for (candidate in found) {
        var isFound = false
        for (existing in result) {
            // Fresh Exercise instances are compared by name; identity is irrelevant.
            if (existing.Name == candidate.Name || existing === candidate) {
                isFound = true
                break
            }
        }
        if (!isFound) {
            result.add(candidate)
        }
    }
    return result
}

/**
 * Returns the body part of the first known exercise with [name], else `""`.
 *
 * Legacy quirk preserved: a miss returns an empty string, not null.
 */
fun categoryFor(knownExercises: List<Exercise>, name: String?): String? {
    for (exercise in knownExercises) {
        if (exercise.Name == name) {
            return exercise.BodyPart
        }
    }
    return ""
}

/**
 * Returns the favorite flag of the first known exercise with [name], else `false`.
 */
fun isFavorite(knownExercises: List<Exercise>, name: String?): Boolean? {
    for (exercise in knownExercises) {
        if (exercise.Name == name) {
            return exercise.favorite
        }
    }
    return false
}

/**
 * Returns true when any known exercise has [name].
 */
fun exists(knownExercises: List<Exercise>, name: String?): Boolean {
    for (exercise in knownExercises) {
        if (exercise.Name == name) {
            return true
        }
    }
    return false
}
