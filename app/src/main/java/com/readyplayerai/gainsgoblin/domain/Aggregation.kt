package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import java.util.ArrayList
import java.util.TreeSet

/**
 * Recomputes all derived day and exercise aggregates from [WorkoutDay.Sets].
 *
 * This is a pure port of the legacy in-place [WorkoutDay.UpdateData]. The input
 * [day] is never modified; a fresh [WorkoutDay] is returned instead. The result
 * reuses the input set list instance (without mutating it), matching the brief's
 * allowance to avoid copying the set objects.
 *
 * Legacy quirks preserved:
 * - An empty day resets `Date` to `"0000-00-00"` and `Reps` to `0`.
 * - The day date comes from the FIRST set, while each exercise date/comment comes
 *   from the LAST matching set in list order.
 * - Exercises are ordered alphabetically by exercise name (`TreeSet<String>`).
 * - A day with only zero-valued sets leaves the exercise's max*Set fields pointing
 *   at fresh empty [WorkoutSet] instances, because legacy only replaces them when a
 *   strictly greater value is found.
 * - Ties use the legacy strict `>` comparisons, so the first set wins.
 */
fun updateDay(day: WorkoutDay): WorkoutDay {
    val sourceSets: ArrayList<WorkoutSet> = day.Sets ?: ArrayList()
    val result = WorkoutDay()
    result.Sets = sourceSets

    if (sourceSets.isEmpty()) {
        result.Sets = ArrayList()
        result.Exercises = ArrayList()
        result.DayVolume = 0.0
        result.Date = "0000-00-00"
        result.Reps = 0
        return result
    }

    // Legacy uses the first set's date, including null if the set has no date.
    result.Date = sourceSets[0].date

    val exerciseNames = TreeSet<String>()
    var dayVolume = 0.0
    for (workoutSet in sourceSets) {
        dayVolume += setVolume(workoutSet)
        val name = workoutSet.exerciseName ?: continue
        exerciseNames.add(name)
    }

    val dayExercises = ArrayList<WorkoutExercise>()
    for (exerciseName in exerciseNames) {
        val dayExercise = WorkoutExercise()
        dayExercise.Exercise = exerciseName

        var exerciseVolume = 0.0
        var exerciseEstimatedOneRepMax = 0.0
        var exerciseActualOneRepMax = 0.0
        var exerciseMaxReps = 0.0
        var exerciseMaxWeight = 0.0
        var exerciseTotalReps = 0.0
        var exerciseTotalSets = 0.0
        var exerciseMaxSetVolume = 0.0
        var exerciseDate: String? = ""
        var exerciseComment: String? = ""

        var exerciseMaxVolumeSet = WorkoutSet()
        var exerciseMaxRepsSet = WorkoutSet()
        var exerciseMaxWeightSet = WorkoutSet()
        val exerciseSets = ArrayList<WorkoutSet>()

        for (workoutSet in sourceSets) {
            if (workoutSet.exerciseName != exerciseName) continue

            val reps = workoutSet.reps ?: 0.0
            val weight = workoutSet.weight ?: 0.0
            val volume = reps * weight
            val estimatedOneRepMax = weight * (1 + reps / 30.0)

            exerciseVolume += volume
            exerciseTotalReps += reps
            exerciseTotalSets += 1.0
            exerciseSets.add(workoutSet)
            exerciseDate = workoutSet.date
            exerciseComment = workoutSet.comment

            if (estimatedOneRepMax > exerciseEstimatedOneRepMax) {
                exerciseEstimatedOneRepMax = estimatedOneRepMax
            }
            if (reps == 1.0 && weight > exerciseActualOneRepMax) {
                exerciseActualOneRepMax = weight
            }
            if (reps > exerciseMaxReps) {
                exerciseMaxReps = reps
                exerciseMaxRepsSet = workoutSet
            }
            if (weight > exerciseMaxWeight) {
                exerciseMaxWeight = weight
                exerciseMaxWeightSet = workoutSet
            }
            if (volume > exerciseMaxSetVolume) {
                exerciseMaxSetVolume = volume
                exerciseMaxVolumeSet = workoutSet
            }
        }

        dayExercise.Volume = exerciseVolume
        dayExercise.EstimatedOneRepMax = exerciseEstimatedOneRepMax
        dayExercise.MaxReps = exerciseMaxReps
        dayExercise.MaxWeight = exerciseMaxWeight
        dayExercise.TotalReps = exerciseTotalReps
        dayExercise.TotalSets = exerciseTotalSets
        dayExercise.Sets = exerciseSets
        dayExercise.Date = exerciseDate
        dayExercise.MaxSetVolume = exerciseMaxSetVolume
        dayExercise.ActualOneRepMax = exerciseActualOneRepMax
        dayExercise.Comment = exerciseComment
        dayExercise.maxVolumeSet = exerciseMaxVolumeSet
        dayExercise.maxWeightSet = exerciseMaxWeightSet
        dayExercise.maxRepsSet = exerciseMaxRepsSet

        dayExercises.add(dayExercise)
    }

    result.Exercises = dayExercises
    result.DayVolume = dayVolume

    var dayReps = 0
    for (exercise in dayExercises) {
        dayReps += Math.round(exercise.TotalReps ?: 0.0).toInt()
    }
    result.Reps = dayReps

    return result
}

/**
 * Groups [sets] into one [WorkoutDay] per distinct date.
 *
 * Pure port of the legacy `setsToEverything()`. Dates are discovered and returned
 * in `TreeSet<String>` order (chronological for `yyyy-MM-dd`). Within each day the
 * sets keep their original list order. Every day is passed through [updateDay] so
 * the result matches the legacy `addSet` + `UpdateData` chain's final state.
 */
fun daysFromSets(sets: List<WorkoutSet>): List<WorkoutDay> {
    val dates = TreeSet<String>()
    for (workoutSet in sets) {
        val date = workoutSet.date ?: continue
        dates.add(date)
    }

    val days = ArrayList<WorkoutDay>()
    for (date in dates) {
        val daySets = ArrayList<WorkoutSet>()
        for (workoutSet in sets) {
            if (workoutSet.date == date) {
                daySets.add(workoutSet)
            }
        }
        val day = WorkoutDay()
        day.Sets = daySets
        days.add(updateDay(day))
    }
    return days
}

/**
 * Backfills null aggregate fields on each exercise from the day's raw sets.
 *
 * Pure port of the legacy `repairWorkoutData()`. Returns a NEW [WorkoutDay] whose
 * exercises are fresh copies; the input is never mutated. Only null exercise fields
 * are filled, and only when at least one matching raw set exists.
 *
 * Legacy quirks preserved:
 * - An exercise with no matching sets is copied exactly as-is, including null fields.
 * - Non-null fields are never overwritten.
 * - Day-level fields (`Date`, `DayVolume`, `Reps`) are copied unchanged; the legacy
 *   repair only touched exercise aggregates.
 * - The repaired max*Set fields reference the original [WorkoutSet] instances chosen
 *   by `maxByOrNull`, matching legacy.
 */
fun backfillExerciseFields(day: WorkoutDay): WorkoutDay {
    val result = WorkoutDay()
    result.Sets = day.Sets
    result.Date = day.Date
    result.DayVolume = day.DayVolume
    result.Reps = day.Reps

    val sourceExercises = day.Exercises
    if (sourceExercises == null) {
        result.Exercises = null
        return result
    }

    val sourceSets = day.Sets ?: ArrayList()
    val repairedExercises = ArrayList<WorkoutExercise>()
    for (sourceExercise in sourceExercises) {
        val exercise = copyExercise(sourceExercise)
        val exerciseSets = sourceSets.filter { it.exerciseName == sourceExercise.Exercise }
        if (exerciseSets.isEmpty()) {
            repairedExercises.add(exercise)
            continue
        }

        if (exercise.Volume == null) {
            exercise.Volume = exerciseSets.sumOf { (it.reps ?: 0.0) * (it.weight ?: 0.0) }
        }
        if (exercise.TotalReps == null) {
            exercise.TotalReps = exerciseSets.sumOf { it.reps ?: 0.0 }
        }
        if (exercise.TotalSets == null) {
            exercise.TotalSets = exerciseSets.size.toDouble()
        }
        if (exercise.MaxWeight == null) {
            exercise.MaxWeight = exerciseSets.maxOf { it.weight ?: 0.0 }
        }
        if (exercise.MaxReps == null) {
            exercise.MaxReps = exerciseSets.maxOf { it.reps ?: 0.0 }
        }
        if (exercise.MaxSetVolume == null) {
            exercise.MaxSetVolume = exerciseSets.maxOf { (it.reps ?: 0.0) * (it.weight ?: 0.0) }
        }
        if (exercise.EstimatedOneRepMax == null) {
            exercise.EstimatedOneRepMax = exerciseSets.maxOf {
                (it.weight ?: 0.0) * (1 + (it.reps ?: 0.0) / 30.0)
            }
        }
        if (exercise.ActualOneRepMax == null) {
            exercise.ActualOneRepMax = exerciseSets
                .filter { it.reps == 1.0 }
                .maxOfOrNull { it.weight ?: 0.0 } ?: 0.0
        }
        if (exercise.Date == null) {
            exercise.Date = exerciseSets.first().date
        }
        if (exercise.maxWeightSet == null) {
            exercise.maxWeightSet = exerciseSets.maxByOrNull { it.weight ?: 0.0 }
        }
        if (exercise.maxRepsSet == null) {
            exercise.maxRepsSet = exerciseSets.maxByOrNull { it.reps ?: 0.0 }
        }
        if (exercise.maxVolumeSet == null) {
            exercise.maxVolumeSet = exerciseSets.maxByOrNull {
                (it.reps ?: 0.0) * (it.weight ?: 0.0)
            }
        }

        repairedExercises.add(exercise)
    }

    result.Exercises = repairedExercises
    return result
}

private fun setVolume(workoutSet: WorkoutSet): Double {
    return (workoutSet.reps ?: 0.0) * (workoutSet.weight ?: 0.0)
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
    copy.VolumePR = source.VolumePR
    copy.ActualOneRepMaxPR = source.ActualOneRepMaxPR
    copy.EstimatedOneRepMaxPR = source.EstimatedOneRepMaxPR
    copy.MaxRepsPR = source.MaxRepsPR
    copy.MaxWeightPR = source.MaxWeightPR
    copy.HTLT = source.HTLT
    copy.Comment = source.Comment
    copy.maxVolumeSet = source.maxVolumeSet
    copy.maxRepsSet = source.maxRepsSet
    copy.maxWeightSet = source.maxWeightSet
    return copy
}
