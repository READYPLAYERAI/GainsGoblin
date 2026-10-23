package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.TreeMap

/**
 * Time range used by [periodStats].
 */
enum class PeriodRange {
    /** The current Monday-start calendar week. */
    WEEK,

    /** The current calendar month. */
    MONTH,

    /** Every valid day in the input, with no start threshold. */
    ALL
}

/**
 * Aggregated totals for one period, plus an optional comparison against the
 * elapsed part of the preceding period.
 */
data class PeriodStats(
    val volume: Double,
    val sets: Int,
    val workouts: Int,
    val prCount: Int,
    val deltaPercent: Double?,
    val sinceDate: String?
)

/**
 * One labelled volume total, used for weekly and monthly bar charts.
 */
data class VolumeBucket(val label: String, val value: Float)

/**
 * Number of sets performed for a single exercise category.
 */
data class CategorySetCount(val category: String, val sets: Int)

/**
 * One day cell of the consistency heatmap, with an intensity [level] from 0 to 3.
 */
data class ConsistencyCell(val date: String, val level: Int)

/**
 * Which metric improved during a progressive-overload comparison.
 */
enum class OverloadKind {
    /** Top weight increased. */
    WEIGHT,

    /** Top reps increased while weight stayed the same. */
    REPS,

    /** Session volume increased without a top-weight or top-reps increase. */
    VOLUME
}

/**
 * A recorded progressive-overload improvement and its magnitude.
 */
data class OverloadDelta(val kind: OverloadKind, val amount: Double)

/**
 * A single per-day session summary for one exercise.
 */
data class RecordSession(
    val date: String,
    val topWeight: Double,
    val topReps: Double,
    val estOneRep: Double,
    val volume: Double
)

/**
 * All-time records and session history for one known exercise.
 */
data class ExerciseRecord(
    val name: String,
    val category: String,
    val favorite: Boolean,
    val maxWeight: Double,
    val maxWeightReps: Double,
    val maxWeightDate: String?,
    val maxReps: Double,
    val maxRepsWeight: Double,
    val maxRepsDate: String?,
    val bestSetVolume: Double,
    val bestSetReps: Double,
    val bestSetWeight: Double,
    val bestSetDate: String?,
    val estOneRep: Double,
    val estOneRepDate: String?,
    val actualOneRep: Double,
    val sessions: List<RecordSession>
)

/**
 * Aggregates volume, set, workout and PR totals over [days] for the given [range],
 * comparing [range]'s volume to the elapsed part of the previous period.
 */
fun periodStats(days: List<WorkoutDay>, range: PeriodRange, today: Date = Date()): PeriodStats {
    val startMillis: Long? = when (range) {
        PeriodRange.WEEK -> startOfWeekMillis(today)
        PeriodRange.MONTH -> startOfMonthMillis(today)
        PeriodRange.ALL -> null
    }

    var volume = 0.0
    var sets = 0
    var workouts = 0
    var prCount = 0
    var sinceMillis: Long? = null
    var sinceDate: String? = null

    for (day in days) {
        val parsed = parseDayDate(day.Date) ?: continue
        if (range != PeriodRange.ALL && parsed.time < startMillis!!) continue

        volume += day.DayVolume ?: 0.0
        val daySets = day.Sets
        val setCount = daySets?.size ?: 0
        sets += setCount
        if (setCount > 0) workouts += 1

        val exercises = day.Exercises
        if (exercises != null) {
            for (exercise in exercises) {
                if (exercise.VolumePR ||
                    exercise.MaxWeightPR ||
                    exercise.MaxRepsPR ||
                    exercise.EstimatedOneRepMaxPR ||
                    exercise.ActualOneRepMaxPR
                ) {
                    prCount += 1
                }
            }
        }

        if (sinceMillis == null || parsed.time < sinceMillis) {
            sinceMillis = parsed.time
            sinceDate = day.Date
        }
    }

    val deltaPercent: Double? = if (range == PeriodRange.ALL) {
        null
    } else {
        val elapsed = Math.max(0L, today.time - startMillis!!)
        val shift: Long = when (range) {
            PeriodRange.WEEK -> 7L * 86_400_000L
            PeriodRange.MONTH -> daysInPreviousMonth(today).toLong() * 86_400_000L
            PeriodRange.ALL -> 0L
        }
        val previousStart = startMillis - shift
        // Clamp the previous window so it never overlaps the current period.
        val previousEnd = Math.min(startMillis, startMillis - shift + elapsed)
        var previousVolume = 0.0
        for (day in days) {
            val parsed = parseDayDate(day.Date) ?: continue
            if (parsed.time >= previousStart && parsed.time < previousEnd) {
                previousVolume += day.DayVolume ?: 0.0
            }
        }
        if (previousVolume > 0.0) {
            (volume - previousVolume) / previousVolume * 100.0
        } else {
            null
        }
    }

    return PeriodStats(
        volume = volume,
        sets = sets,
        workouts = workouts,
        prCount = prCount,
        deltaPercent = deltaPercent,
        sinceDate = sinceDate
    )
}

/**
 * Builds one volume bucket per Monday-start week that contains a valid day,
 * ascending by week and labelled with that week's Monday.
 */
fun weeklyVolumeBuckets(days: List<WorkoutDay>, today: Date = Date()): List<VolumeBucket> {
    val format = SimpleDateFormat("yyyy-MM-dd")
    val totals = TreeMap<Long, Double>()
    for (day in days) {
        val parsed = parseDayDate(day.Date) ?: continue
        val monday = startOfWeekMillis(parsed)
        totals[monday] = (totals[monday] ?: 0.0) + (day.DayVolume ?: 0.0)
    }

    val result = ArrayList<VolumeBucket>()
    val calendar = Calendar.getInstance()
    for ((monday, total) in totals) {
        calendar.timeInMillis = monday
        result.add(VolumeBucket(format.format(calendar.time), total.toFloat()))
    }
    return result
}

/**
 * Builds one volume bucket per calendar month that contains a valid day,
 * ascending by month and labelled `yyyy-MM`.
 */
fun monthlyVolumeBuckets(days: List<WorkoutDay>): List<VolumeBucket> {
    val format = SimpleDateFormat("yyyy-MM")
    val totals = TreeMap<String, Double>()
    for (day in days) {
        val parsed = parseDayDate(day.Date) ?: continue
        val label = format.format(parsed)
        totals[label] = (totals[label] ?: 0.0) + (day.DayVolume ?: 0.0)
    }

    val result = ArrayList<VolumeBucket>()
    for ((label, total) in totals) {
        result.add(VolumeBucket(label, total.toFloat()))
    }
    return result
}

/**
 * Counts sets per category, skipping null and blank categories, sorted by count
 * descending and then category name ascending.
 */
fun setsPerCategory(days: List<WorkoutDay>): List<CategorySetCount> {
    val counts = LinkedHashMap<String, Int>()
    for (day in days) {
        val sets = day.Sets ?: continue
        for (workoutSet in sets) {
            val category = workoutSet.category
            if (category == null || category.isBlank()) continue
            counts[category] = (counts[category] ?: 0) + 1
        }
    }

    val result = ArrayList<CategorySetCount>()
    for ((category, count) in counts) {
        result.add(CategorySetCount(category, count))
    }
    result.sortWith(
        compareByDescending<CategorySetCount> { it.sets }.thenBy { it.category }
    )
    return result
}

/**
 * Builds the last [weeks] Monday-start weeks as an ascending list of exactly
 * `weeks * 7` daily heatmap cells, ending with the week containing [today].
 */
fun consistencyCells(
    days: List<WorkoutDay>,
    weeks: Int = 10,
    today: Date = Date()
): List<ConsistencyCell> {
    val currentMonday = startOfWeekMillis(today)

    val oldestCalendar = Calendar.getInstance()
    oldestCalendar.timeInMillis = currentMonday
    oldestCalendar.add(Calendar.DATE, -7 * (weeks - 1))
    val windowStart = oldestCalendar.timeInMillis

    val windowEndCalendar = Calendar.getInstance()
    windowEndCalendar.timeInMillis = currentMonday
    windowEndCalendar.add(Calendar.DATE, 7)
    val windowEnd = windowEndCalendar.timeInMillis

    val volumesByDate = LinkedHashMap<String, Double>()
    for (day in days) {
        val parsed = parseDayDate(day.Date) ?: continue
        if (parsed.time < windowStart || parsed.time >= windowEnd) continue
        val sets = day.Sets ?: continue
        if (sets.isEmpty()) continue
        var volume = 0.0
        for (workoutSet in sets) {
            volume += (workoutSet.reps ?: 0.0) * (workoutSet.weight ?: 0.0)
        }
        val dateKey = day.Date!!
        volumesByDate[dateKey] = (volumesByDate[dateKey] ?: 0.0) + volume
    }

    val sortedVolumes = ArrayList(volumesByDate.values)
    sortedVolumes.sort()
    val firstThreshold = if (sortedVolumes.isEmpty()) {
        0.0
    } else {
        sortedVolumes[Math.floor(0.33 * (sortedVolumes.size - 1)).toInt()]
    }
    val secondThreshold = if (sortedVolumes.isEmpty()) {
        0.0
    } else {
        sortedVolumes[Math.floor(0.66 * (sortedVolumes.size - 1)).toInt()]
    }

    val cells = ArrayList<ConsistencyCell>()
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = windowStart
    val format = SimpleDateFormat("yyyy-MM-dd")
    val cellCount = weeks * 7
    for (index in 0 until cellCount) {
        val dateString = format.format(calendar.time)
        val volume = volumesByDate[dateString]
        val level = when {
            volume == null -> 0
            sortedVolumes.size < 3 -> 2
            volume <= firstThreshold -> 1
            volume <= secondThreshold -> 2
            else -> 3
        }
        cells.add(ConsistencyCell(dateString, level))
        calendar.add(Calendar.DATE, 1)
    }
    return cells
}

/**
 * Maps each exercise appearance that improved on its previous appearance to a
 * progressive-overload delta, keyed `"<date>|<exerciseName>"`.
 */
fun overloadDeltas(days: List<WorkoutDay>): Map<String, OverloadDelta> {
    val sorted = sortedByDate(days)
    val previousByName = LinkedHashMap<String, WorkoutExercise>()
    val result = LinkedHashMap<String, OverloadDelta>()

    for (day in sorted) {
        val exercises = day.Exercises ?: continue
        for (exercise in exercises) {
            val name = exercise.Exercise ?: continue
            val previous = previousByName[name]
            if (previous != null) {
                val currentWeight = exercise.MaxWeight ?: 0.0
                val previousWeight = previous.MaxWeight ?: 0.0
                val currentReps = exercise.MaxReps ?: 0.0
                val previousReps = previous.MaxReps ?: 0.0
                val currentVolume = exercise.Volume ?: 0.0
                val previousVolume = previous.Volume ?: 0.0

                val delta = when {
                    currentWeight > previousWeight ->
                        OverloadDelta(OverloadKind.WEIGHT, currentWeight - previousWeight)

                    currentReps > previousReps ->
                        OverloadDelta(OverloadKind.REPS, currentReps - previousReps)

                    currentVolume > previousVolume ->
                        OverloadDelta(OverloadKind.VOLUME, 0.0)

                    else -> null
                }
                if (delta != null) {
                    result["${day.Date}|$name"] = delta
                }
            }
            previousByName[name] = exercise
        }
    }
    return result
}

/**
 * Builds all-time records and session history for every known exercise that was
 * actually performed in a valid day, favourites first and then by name ascending.
 */
fun exerciseRecords(
    days: List<WorkoutDay>,
    knownExercises: List<Exercise>
): List<ExerciseRecord> {
    val result = ArrayList<ExerciseRecord>()

    for (knownExercise in knownExercises) {
        val name = knownExercise.Name ?: continue

        val appearances = ArrayList<Pair<String, WorkoutExercise>>()
        for (day in days) {
            if (parseDayDate(day.Date) == null) continue
            val exercises = day.Exercises ?: continue
            for (exercise in exercises) {
                if (exercise.Exercise == name) {
                    appearances.add(Pair(day.Date ?: "", exercise))
                }
            }
        }
        if (appearances.isEmpty()) continue

        appearances.sortBy { parseDayDate(it.first)?.time ?: 0L }

        var maxWeight = 0.0
        var maxWeightReps = 0.0
        var maxWeightDate: String? = null
        var maxReps = 0.0
        var maxRepsWeight = 0.0
        var maxRepsDate: String? = null
        var bestSetVolume = 0.0
        var bestSetReps = 0.0
        var bestSetWeight = 0.0
        var bestSetDate: String? = null
        var estOneRep = 0.0
        var estOneRepDate: String? = null
        var actualOneRep = 0.0

        val sessions = ArrayList<RecordSession>()
        for ((date, exercise) in appearances) {
            val topWeight = exercise.MaxWeight ?: 0.0
            val topReps = exercise.MaxReps ?: 0.0
            val estimated = exercise.EstimatedOneRepMax ?: 0.0
            val sessionVolume = exercise.Volume ?: 0.0

            sessions.add(
                RecordSession(
                    date = date,
                    topWeight = topWeight,
                    topReps = topReps,
                    estOneRep = estimated,
                    volume = sessionVolume
                )
            )

            if (maxWeightDate == null || topWeight > maxWeight) {
                maxWeight = topWeight
                maxWeightReps = exercise.maxWeightSet?.reps ?: topReps
                maxWeightDate = date
            }

            if (maxRepsDate == null || topReps > maxReps) {
                maxReps = topReps
                maxRepsWeight = exercise.maxRepsSet?.weight ?: topWeight
                maxRepsDate = date
            }

            val setVolume = exercise.MaxSetVolume ?: 0.0
            if (bestSetDate == null || setVolume > bestSetVolume) {
                bestSetVolume = setVolume
                bestSetReps = exercise.maxVolumeSet?.reps ?: topReps
                bestSetWeight = exercise.maxVolumeSet?.weight ?: topWeight
                bestSetDate = date
            }

            if (estOneRepDate == null || estimated > estOneRep) {
                estOneRep = estimated
                estOneRepDate = date
            }

            val actual = exercise.ActualOneRepMax ?: 0.0
            if (actual > actualOneRep) {
                actualOneRep = actual
            }
        }

        result.add(
            ExerciseRecord(
                name = name,
                category = categoryFor(knownExercises, name) ?: "",
                favorite = isFavorite(knownExercises, name) == true,
                maxWeight = maxWeight,
                maxWeightReps = maxWeightReps,
                maxWeightDate = maxWeightDate,
                maxReps = maxReps,
                maxRepsWeight = maxRepsWeight,
                maxRepsDate = maxRepsDate,
                bestSetVolume = bestSetVolume,
                bestSetReps = bestSetReps,
                bestSetWeight = bestSetWeight,
                bestSetDate = bestSetDate,
                estOneRep = estOneRep,
                estOneRepDate = estOneRepDate,
                actualOneRep = actualOneRep,
                sessions = sessions
            )
        )
    }

    result.sortWith(
        compareByDescending<ExerciseRecord> { it.favorite }.thenBy { it.name }
    )
    return result
}

/**
 * Parses an app date string, returning null for blank, unparsable or sentinel dates.
 */
private fun parseDayDate(value: String?): Date? {
    if (value == null || value.isBlank() || value == "0000-00-00") return null
    return try {
        SimpleDateFormat("yyyy-MM-dd").parse(value)
    } catch (exception: Exception) {
        null
    }
}

/**
 * Returns the millis of Monday 00:00 for the week containing [date].
 */
private fun startOfWeekMillis(date: Date): Long {
    val calendar = Calendar.getInstance()
    calendar.time = date
    val offset = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    calendar.add(Calendar.DATE, -offset)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Returns the millis of day 1 00:00 for the month containing [date].
 */
private fun startOfMonthMillis(date: Date): Long {
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/**
 * Returns the number of days in the calendar month immediately before [date]'s month.
 */
private fun daysInPreviousMonth(date: Date): Int {
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.add(Calendar.DATE, -1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}
