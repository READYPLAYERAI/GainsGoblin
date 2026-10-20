package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import java.util.ArrayList

/**
 * Snapshot of every personal-record map the legacy storage layer maintained.
 *
 * Scalar maps contain an entry for every known exercise, initialized to `0.0`.
 * Set maps contain entries only after a corresponding PR was found.
 */
data class PersonalRecords(
    val volumePRs: Map<String, Double>,
    val setVolumePRs: Map<String, Pair<Double, Double>>,
    val actualOneRepMaxPRs: Map<String, Double>,
    val estimatedOneRMPRs: Map<String, Double>,
    val maxRepsPRs: Map<String, Double>,
    val maxWeightPRs: Map<String, Double>,
    val lastTimeVolume: Map<String, Double>,
    val maxVolumeSetPRs: Map<String, WorkoutSet>,
    val maxRepsSetPRs: Map<String, WorkoutSet>,
    val maxWeightSetPRs: Map<String, WorkoutSet>
)

/**
 * Calculates personal records without mutating [days] or any exercise flags.
 *
 * Pure port of the legacy `calculatePersonalRecords()`. The legacy double-loop is
 * preserved: outer over [knownExercises], then [days] in the caller-provided order,
 * then each day's exercises. Exercises whose name is not in [knownExercises] are
 * ignored entirely.
 *
 * Legacy quirks preserved:
 * - Every scalar map is initialized for every known exercise.
 * - All maxima use strict `>`; equal values never replace the existing record.
 * - `maxVolumeSetPRs` stores a fresh [WorkoutSet] with only `reps = exercise.MaxReps`
 *   and `weight = exercise.MaxWeight` populated (legacy `temp_set` quirk); all other
 *   fields stay at their no-argument-constructor defaults/null.
 * - `maxRepsSetPRs` and `maxWeightSetPRs` store the ACTUAL achieving set instances
 *   from the day exercise (`exercise.maxRepsSet` / `exercise.maxWeightSet`), including
 *   their full date/name/category/reps/weight/comment fields. A null achieving set is
 *   skipped, leaving no entry in the map.
 * - `lastTimeVolume` is updated in both the HTLT and non-HTLT branches, so its final
 *   value is simply the last matching exercise's volume for that day.
 */
fun calculatePersonalRecords(
    days: List<WorkoutDay>,
    knownExercises: List<Exercise>
): PersonalRecords {
    return scanPersonalRecords(days, knownExercises, markFlags = false)
}

/**
 * Returns copies of [days] with PR and HTLT flags set exactly as the legacy in-place
 * scan would have set them.
 *
 * Every returned [WorkoutDay] and [WorkoutExercise] is a fresh instance (the `Sets`
 * list instance is reused but never mutated). Flags are `true` only on the first
 * strictly-greater occurrence that raises a running maximum; all other flags are
 * `false`. HTLT uses the legacy "strictly greater than previous volume" rule, with
 * `lastTimeVolume` reset to the current volume in both branches.
 */
fun applyPrFlags(
    days: List<WorkoutDay>,
    knownExercises: List<Exercise>
): List<WorkoutDay> {
    val copies = ArrayList<WorkoutDay>(days.size)
    for (day in days) {
        copies.add(copyDay(day))
    }
    scanPersonalRecords(copies, knownExercises, markFlags = true)
    return copies
}

private fun scanPersonalRecords(
    days: List<WorkoutDay>,
    knownExercises: List<Exercise>,
    markFlags: Boolean
): PersonalRecords {
    val volumePRs = LinkedHashMap<String, Double>()
    val setVolumePRs = LinkedHashMap<String, Pair<Double, Double>>()
    val actualOneRepMaxPRs = LinkedHashMap<String, Double>()
    val estimatedOneRMPRs = LinkedHashMap<String, Double>()
    val maxRepsPRs = LinkedHashMap<String, Double>()
    val maxWeightPRs = LinkedHashMap<String, Double>()
    val lastTimeVolume = LinkedHashMap<String, Double>()
    val maxVolumeSetPRs = LinkedHashMap<String, WorkoutSet>()
    val maxRepsSetPRs = LinkedHashMap<String, WorkoutSet>()
    val maxWeightSetPRs = LinkedHashMap<String, WorkoutSet>()

    for (knownExercise in knownExercises) {
        val name = knownExercise.Name ?: continue
        volumePRs[name] = 0.0
        setVolumePRs[name] = Pair(0.0, 0.0)
        actualOneRepMaxPRs[name] = 0.0
        estimatedOneRMPRs[name] = 0.0
        maxRepsPRs[name] = 0.0
        maxWeightPRs[name] = 0.0
        lastTimeVolume[name] = 0.0
    }

    if (markFlags) {
        for (day in days) {
            val exercises = day.Exercises ?: continue
            for (exercise in exercises) {
                exercise.VolumePR = false
                exercise.ActualOneRepMaxPR = false
                exercise.EstimatedOneRepMaxPR = false
                exercise.MaxRepsPR = false
                exercise.MaxWeightPR = false
                exercise.HTLT = false
            }
        }
    }

    // Legacy double-loop ordering: known exercise first, then days in given order.
    for (knownExercise in knownExercises) {
        val name = knownExercise.Name ?: continue
        for (day in days) {
            val exercises = day.Exercises ?: continue
            for (exercise in exercises) {
                if (exercise.Exercise != name) continue

                val volume = exercise.Volume ?: 0.0

                // Per-exercise volume PR.
                if (volumePRs[name]!! < volume) {
                    if (markFlags) exercise.VolumePR = true
                    volumePRs[name] = volume
                }

                val setVolume = setVolumePRs[name]!!.first * setVolumePRs[name]!!.second

                // Per-set volume PR. The legacy pair uses MaxReps/MaxWeight, and the
                // stored PR set is a fresh reps/weight-only WorkoutSet.
                if (setVolume < (exercise.MaxSetVolume ?: 0.0)) {
                    val maxReps = exercise.MaxReps ?: 0.0
                    val maxWeight = exercise.MaxWeight ?: 0.0
                    setVolumePRs[name] = Pair(maxReps, maxWeight)
                    maxVolumeSetPRs[name] = repsWeightSet(exercise)
                }

                // Actual one-rep maximum PR.
                if (actualOneRepMaxPRs[name]!! < (exercise.ActualOneRepMax ?: 0.0)) {
                    if (markFlags) exercise.ActualOneRepMaxPR = true
                    actualOneRepMaxPRs[name] = exercise.ActualOneRepMax ?: 0.0
                }

                // Estimated one-rep maximum PR.
                if (estimatedOneRMPRs[name]!! < (exercise.EstimatedOneRepMax ?: 0.0)) {
                    if (markFlags) exercise.EstimatedOneRepMaxPR = true
                    estimatedOneRMPRs[name] = exercise.EstimatedOneRepMax ?: 0.0
                }

                // Max reps PR.
                if (maxRepsPRs[name]!! < (exercise.MaxReps ?: 0.0)) {
                    if (markFlags) exercise.MaxRepsPR = true
                    maxRepsPRs[name] = exercise.MaxReps ?: 0.0
                    exercise.maxRepsSet?.let { maxRepsSetPRs[name] = it }
                }

                // Max weight PR.
                if (maxWeightPRs[name]!! < (exercise.MaxWeight ?: 0.0)) {
                    if (markFlags) exercise.MaxWeightPR = true
                    maxWeightPRs[name] = exercise.MaxWeight ?: 0.0
                    exercise.maxWeightSet?.let { maxWeightSetPRs[name] = it }
                }

                // Harder than last time: legacy updates the stored volume in BOTH
                // branches, so only the strict comparison controls the flag.
                if (lastTimeVolume[name]!! < volume) {
                    if (markFlags) exercise.HTLT = true
                    lastTimeVolume[name] = volume
                } else {
                    lastTimeVolume[name] = volume
                }
            }
        }
    }

    return PersonalRecords(
        volumePRs = volumePRs,
        setVolumePRs = setVolumePRs,
        actualOneRepMaxPRs = actualOneRepMaxPRs,
        estimatedOneRMPRs = estimatedOneRMPRs,
        maxRepsPRs = maxRepsPRs,
        maxWeightPRs = maxWeightPRs,
        lastTimeVolume = lastTimeVolume,
        maxVolumeSetPRs = maxVolumeSetPRs,
        maxRepsSetPRs = maxRepsSetPRs,
        maxWeightSetPRs = maxWeightSetPRs
    )
}

/**
 * Legacy `temp_set` quirk: the PR set carries only reps and weight; every other
 * field remains at the no-argument constructor default.
 */
private fun repsWeightSet(exercise: WorkoutExercise): WorkoutSet {
    val workoutSet = WorkoutSet()
    workoutSet.reps = exercise.MaxReps ?: 0.0
    workoutSet.weight = exercise.MaxWeight ?: 0.0
    return workoutSet
}

private fun copyDay(day: WorkoutDay): WorkoutDay {
    val copy = WorkoutDay()
    copy.Sets = day.Sets
    copy.Date = day.Date
    copy.DayVolume = day.DayVolume
    copy.Reps = day.Reps

    val sourceExercises = day.Exercises
    if (sourceExercises == null) {
        copy.Exercises = null
    } else {
        val exercises = ArrayList<WorkoutExercise>(sourceExercises.size)
        for (sourceExercise in sourceExercises) {
            exercises.add(copyExercise(sourceExercise))
        }
        copy.Exercises = exercises
    }
    return copy
}

private fun copyExercise(source: WorkoutExercise): WorkoutExercise {
    val copy = WorkoutExercise()
    copy.Sets = source.Sets
    copy.Date = source.Date
    copy.Exercise = source.Exercise
    copy.Volume = source.Volume
    copy.EstimatedOneRepMax = source.EstimatedOneRepMax
    copy.ActualOneRepMax = source.ActualOneRepMax
    copy.MaxWeight = source.MaxWeight
    copy.MaxReps = source.MaxReps
    copy.TotalReps = source.TotalReps
    copy.TotalSets = source.TotalSets
    copy.MaxSetVolume = source.MaxSetVolume
    copy.VolumePR = false
    copy.ActualOneRepMaxPR = false
    copy.EstimatedOneRepMaxPR = false
    copy.MaxRepsPR = false
    copy.MaxWeightPR = false
    copy.HTLT = false
    copy.Comment = source.Comment
    copy.maxVolumeSet = source.maxVolumeSet
    copy.maxRepsSet = source.maxRepsSet
    copy.maxWeightSet = source.maxWeightSet
    return copy
}
