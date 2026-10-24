package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TreeMap
import kotlin.math.roundToInt

/**
 * Metric selectable by the per-exercise progress detail screen.
 */
enum class ExerciseMetric { EST_ONE_REP, TOP_SET, VOLUME, REPS }

/**
 * Time window selectable by the per-exercise progress detail screen.
 */
enum class ExerciseRange { ONE_MONTH, THREE_MONTHS, SIX_MONTHS, ALL }

/**
 * The kind of personal record shown in the record list.
 */
enum class ExercisePrKind {
    EST_ONE_REP, MAX_WEIGHT, MAX_REPS, SET_VOLUME, VOLUME, ACTUAL_ONE_REP
}

/**
 * One set entry inside an [ExerciseSession], in raw list order.
 */
data class ExerciseSetEntry(
    val date: String,
    val index: Int,
    val weight: Double,
    val reps: Double,
    val volume: Double,
    val estimatedOneRepMax: Double,
    val isPr: Boolean,
    val comment: String,
)

/**
 * One training day for a single exercise, with its aggregates and per-session
 * progressive-overload deltas.
 */
data class ExerciseSession(
    val date: String,
    val sets: List<ExerciseSetEntry>,
    val volume: Double,
    val totalReps: Double,
    val topWeight: Double,
    val topReps: Double,
    val estimatedOneRepMax: Double,
    val maxReps: Double,
    val bestSetVolume: Double,
    val bestSetWeight: Double,
    val bestSetReps: Double,
    val comment: String,
    val prKinds: Set<ExercisePrKind>,
    val topWeightDelta: Double?,
    val topRepsDelta: Double?,
)

/**
 * One all-time record row. [setCount] carries the session's set count for
 * [ExercisePrKind.VOLUME] and `0` for every other kind.
 */
data class ExerciseProgressRecord(
    val kind: ExercisePrKind,
    val value: Double,
    val reps: Double,
    val weight: Double,
    val date: String,
    val setCount: Int,
)

/**
 * Mean [value] of the selected metric for one Monday-start week.
 */
data class ExerciseWeekAverage(val weekStart: String, val value: Double)

/**
 * Number of sets that fall into one repetition bucket.
 */
data class ExerciseRepBucket(val label: String, val sets: Int)

/**
 * Number of sessions recorded in one calendar month.
 */
data class ExerciseMonthSessions(val monthKey: String, val label: String, val sessions: Int)

/**
 * Everything the per-exercise progress detail screen needs for one exercise.
 */
data class ExerciseProgress(
    val name: String,
    val category: String,
    val favorite: Boolean,
    val sessions: List<ExerciseSession>,
    val records: List<ExerciseProgressRecord>,
    val totalSets: Int,
    val firstDate: String?,
    val lastDate: String?,
    val consistency: List<ConsistencyCell>,
    val consistencyWeeks: Int,
    val streakWeeks: Int,
    val repBuckets: List<ExerciseRepBucket>,
    val monthlySessions: List<ExerciseMonthSessions>,
    val prSessionCount: Int,
)

/**
 * Builds the full progress payload for [exerciseName] from PR-flagged [days].
 *
 * Pure: [days], [knownExercises] and every object they reference are never mutated.
 * Sessions read the aggregates produced by [updateDay] and the flags produced by
 * [applyPrFlags]; this function never recomputes or re-stamps them.
 *
 * Returns `null` when the exercise is unknown (not in [knownExercises]) and was
 * never trained. When [exerciseName] is known but untrained, returns an empty
 * progress payload (no sessions/records, zeroed grids).
 */
fun exerciseProgress(
    days: List<WorkoutDay>,
    knownExercises: List<Exercise>,
    exerciseName: String,
    today: Date = Date(),
    consistencyWeeks: Int = 16,
    monthCount: Int = 7,
): ExerciseProgress? {
    val known = knownExercises.firstOrNull { it.Name == exerciseName }

    val collected = ArrayList<ParsedSession>()
    for (day in days) {
        val parsed = parseProgressDate(day.Date) ?: continue
        val exercise = day.Exercises?.firstOrNull { it.Exercise == exerciseName }
        val rawMatch = day.Sets?.any { it.exerciseName == exerciseName } == true
        if (exercise == null && !rawMatch) continue
        collected.add(ParsedSession(parsed, buildSession(day, exercise, exerciseName)))
    }

    if (collected.isEmpty() && exerciseName.isNotBlank() && known == null) return null

    val ordered = collected.sortedBy { it.parsed.time }.map { it.session }

    val deltaSessions = ArrayList<ExerciseSession>(ordered.size)
    for (index in ordered.indices) {
        val current = ordered[index]
        val previous = if (index == 0) null else ordered[index - 1]
        deltaSessions.add(
            current.copy(
                topWeightDelta = previous?.let { current.topWeight - it.topWeight },
                topRepsDelta = previous?.let { current.topReps - it.topReps },
            )
        )
    }

    // Chronological set-level PR scan. Four running maxima start at 0.0 and only
    // strictly greater values mark a PR, so ties never do.
    var maxEstimatedOneRepMax = 0.0
    var maxWeight = 0.0
    var maxSetVolume = 0.0
    var maxReps = 0.0
    val sessions = ArrayList<ExerciseSession>(deltaSessions.size)
    for (session in deltaSessions) {
        val prKinds = session.prKinds.toMutableSet()
        val setEntries = ArrayList<ExerciseSetEntry>(session.sets.size)
        for (entry in session.sets) {
            val isPr = entry.estimatedOneRepMax > maxEstimatedOneRepMax ||
                entry.weight > maxWeight ||
                entry.volume > maxSetVolume ||
                entry.reps > maxReps
            if (entry.volume > maxSetVolume) {
                prKinds.add(ExercisePrKind.SET_VOLUME)
            }
            maxEstimatedOneRepMax = maxOf(maxEstimatedOneRepMax, entry.estimatedOneRepMax)
            maxWeight = maxOf(maxWeight, entry.weight)
            maxSetVolume = maxOf(maxSetVolume, entry.volume)
            maxReps = maxOf(maxReps, entry.reps)
            setEntries.add(entry.copy(isPr = isPr))
        }
        sessions.add(session.copy(sets = setEntries, prKinds = prKinds))
    }

    return ExerciseProgress(
        name = exerciseName,
        category = known?.BodyPart ?: "",
        favorite = known?.favorite == true,
        sessions = sessions,
        records = buildRecords(sessions),
        totalSets = sessions.sumOf { it.sets.size },
        firstDate = sessions.firstOrNull()?.date,
        lastDate = sessions.lastOrNull()?.date,
        consistency = buildConsistency(sessions, consistencyWeeks, today),
        consistencyWeeks = consistencyWeeks,
        streakWeeks = computeStreakWeeks(sessions, consistencyWeeks, today),
        repBuckets = buildRepBuckets(sessions),
        monthlySessions = buildMonthlySessions(sessions, monthCount, today),
        prSessionCount = sessions.count { it.prKinds.isNotEmpty() },
    )
}

/**
 * Returns the value of [metric] for this session.
 */
fun ExerciseSession.metricValue(metric: ExerciseMetric): Double = when (metric) {
    ExerciseMetric.EST_ONE_REP -> estimatedOneRepMax
    ExerciseMetric.TOP_SET -> topWeight
    ExerciseMetric.VOLUME -> volume
    ExerciseMetric.REPS -> totalReps
}

/**
 * Keeps only the sessions whose parsed date is on or after the calendar-month
 * cutoff for [range]. [ExerciseRange.ALL] returns [sessions] unchanged; sessions
 * with unparsable dates are dropped for every other range.
 */
fun exerciseSessionsInRange(
    sessions: List<ExerciseSession>,
    range: ExerciseRange,
    today: Date = Date(),
): List<ExerciseSession> {
    if (range == ExerciseRange.ALL) return sessions

    val monthsBack = when (range) {
        ExerciseRange.ONE_MONTH -> 1
        ExerciseRange.THREE_MONTHS -> 3
        ExerciseRange.SIX_MONTHS -> 6
        ExerciseRange.ALL -> 0
    }

    val calendar = Calendar.getInstance()
    calendar.time = today
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.add(Calendar.MONTH, -monthsBack)
    val cutoff = calendar.timeInMillis

    return sessions.filter { session ->
        val parsed = parseProgressDate(session.date) ?: return@filter false
        parsed.time >= cutoff
    }
}

/**
 * Buckets [sessions] into Monday-start weeks that contain at least one session,
 * ascending by week, with the arithmetic mean of the selected metric.
 */
fun exerciseWeekAverages(
    sessions: List<ExerciseSession>,
    metric: ExerciseMetric,
): List<ExerciseWeekAverage> {
    val totals = TreeMap<Long, Double>()
    val counts = TreeMap<Long, Int>()
    for (session in sessions) {
        val parsed = parseProgressDate(session.date) ?: continue
        val monday = startOfWeekMillis(parsed)
        totals[monday] = (totals[monday] ?: 0.0) + session.metricValue(metric)
        counts[monday] = (counts[monday] ?: 0) + 1
    }

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    val result = ArrayList<ExerciseWeekAverage>(totals.size)
    for ((monday, total) in totals) {
        calendar.timeInMillis = monday
        result.add(ExerciseWeekAverage(format.format(calendar.time), total / counts[monday]!!))
    }
    return result
}

private class ParsedSession(val parsed: Date, val session: ExerciseSession)

private class RecordBest {
    var date: String? = null
    var value: Double = 0.0
    var reps: Double = 0.0
    var weight: Double = 0.0
    var setCount: Int = 0
}

private fun buildSession(
    day: WorkoutDay,
    exercise: WorkoutExercise?,
    exerciseName: String,
): ExerciseSession {
    val dayDate = day.Date ?: ""
    val rawSets: List<WorkoutSet> = when {
        exercise?.Sets != null -> exercise.Sets!!
        else -> day.Sets?.filter { it.exerciseName == exerciseName } ?: emptyList()
    }

    val entries = rawSets.mapIndexed { index, workoutSet ->
        val weight = workoutSet.weight ?: 0.0
        val reps = workoutSet.reps ?: 0.0
        ExerciseSetEntry(
            date = workoutSet.date ?: dayDate,
            index = index + 1,
            weight = weight,
            reps = reps,
            volume = reps * weight,
            estimatedOneRepMax = weight * (1 + reps / 30.0),
            isPr = false,
            comment = workoutSet.comment ?: "",
        )
    }

    var comment = ""
    var foundComment = false
    for (workoutSet in rawSets) {
        val value = workoutSet.comment
        if (!value.isNullOrBlank()) {
            comment = value
            foundComment = true
        }
    }
    if (!foundComment) {
        comment = exercise?.Comment ?: ""
    }

    val prKinds = LinkedHashSet<ExercisePrKind>()
    if (exercise?.VolumePR == true) prKinds.add(ExercisePrKind.VOLUME)
    if (exercise?.EstimatedOneRepMaxPR == true) prKinds.add(ExercisePrKind.EST_ONE_REP)
    if (exercise?.MaxRepsPR == true) prKinds.add(ExercisePrKind.MAX_REPS)
    if (exercise?.MaxWeightPR == true) prKinds.add(ExercisePrKind.MAX_WEIGHT)
    if (exercise?.ActualOneRepMaxPR == true) prKinds.add(ExercisePrKind.ACTUAL_ONE_REP)

    return ExerciseSession(
        date = dayDate,
        sets = entries,
        volume = exercise?.Volume ?: 0.0,
        totalReps = exercise?.TotalReps ?: 0.0,
        topWeight = exercise?.MaxWeight ?: 0.0,
        topReps = exercise?.maxWeightSet?.reps ?: (exercise?.MaxReps ?: 0.0),
        estimatedOneRepMax = exercise?.EstimatedOneRepMax ?: 0.0,
        maxReps = exercise?.MaxReps ?: 0.0,
        bestSetVolume = exercise?.MaxSetVolume ?: 0.0,
        bestSetWeight = exercise?.maxVolumeSet?.weight ?: 0.0,
        bestSetReps = exercise?.maxVolumeSet?.reps ?: 0.0,
        comment = comment,
        prKinds = prKinds,
        topWeightDelta = null,
        topRepsDelta = null,
    )
}

private fun buildRecords(sessions: List<ExerciseSession>): List<ExerciseProgressRecord> {
    val order = listOf(
        ExercisePrKind.MAX_WEIGHT,
        ExercisePrKind.EST_ONE_REP,
        ExercisePrKind.SET_VOLUME,
        ExercisePrKind.MAX_REPS,
        ExercisePrKind.VOLUME,
        ExercisePrKind.ACTUAL_ONE_REP,
    )
    val best = order.associateWith { RecordBest() }

    for (session in sessions) {
        consider(
            best.getValue(ExercisePrKind.MAX_WEIGHT),
            session.date,
            session.topWeight,
            session.topReps,
            session.topWeight,
            0,
        )

        val e1rmSet = session.sets.firstOrNull { it.estimatedOneRepMax == session.estimatedOneRepMax }
        consider(
            best.getValue(ExercisePrKind.EST_ONE_REP),
            session.date,
            session.estimatedOneRepMax,
            e1rmSet?.reps ?: session.topReps,
            e1rmSet?.weight ?: session.topWeight,
            0,
        )

        consider(
            best.getValue(ExercisePrKind.SET_VOLUME),
            session.date,
            session.bestSetVolume,
            session.bestSetReps,
            session.bestSetWeight,
            0,
        )

        val repsSet = session.sets.firstOrNull { it.reps == session.maxReps }
        consider(
            best.getValue(ExercisePrKind.MAX_REPS),
            session.date,
            session.maxReps,
            repsSet?.reps ?: session.maxReps,
            repsSet?.weight ?: session.topWeight,
            0,
        )

        consider(
            best.getValue(ExercisePrKind.VOLUME),
            session.date,
            session.volume,
            0.0,
            0.0,
            session.sets.size,
        )

        val repOneSets = session.sets.filter { it.reps == 1.0 }
        if (repOneSets.isNotEmpty()) {
            val topWeight = repOneSets.maxOf { it.weight }
            consider(
                best.getValue(ExercisePrKind.ACTUAL_ONE_REP),
                session.date,
                topWeight,
                1.0,
                topWeight,
                0,
            )
        }
    }

    val result = ArrayList<ExerciseProgressRecord>(order.size)
    for (kind in order) {
        val record = best.getValue(kind)
        val date = record.date ?: continue
        result.add(
            ExerciseProgressRecord(
                kind = kind,
                value = record.value,
                reps = record.reps,
                weight = record.weight,
                date = date,
                setCount = record.setCount,
            )
        )
    }
    return result
}

private fun consider(
    best: RecordBest,
    date: String,
    value: Double,
    reps: Double,
    weight: Double,
    setCount: Int,
) {
    if (best.date == null || value > best.value) {
        best.date = date
        best.value = value
        best.reps = reps
        best.weight = weight
        best.setCount = setCount
    }
}

private fun buildConsistency(
    sessions: List<ExerciseSession>,
    weeks: Int,
    today: Date,
): List<ConsistencyCell> {
    val currentMonday = startOfWeekMillis(today)
    val startCalendar = Calendar.getInstance()
    startCalendar.timeInMillis = currentMonday
    startCalendar.add(Calendar.DATE, -7 * (weeks - 1))
    val windowStart = startCalendar.timeInMillis

    val counts = HashMap<String, Int>()
    for (session in sessions) {
        for (entry in session.sets) {
            if (entry.date.isBlank()) continue
            counts[entry.date] = (counts[entry.date] ?: 0) + 1
        }
    }

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = windowStart
    val cells = ArrayList<ConsistencyCell>(weeks * 7)
    for (index in 0 until weeks * 7) {
        val date = format.format(calendar.time)
        val setCount = counts[date] ?: 0
        val level = when {
            setCount <= 0 -> 0
            setCount <= 2 -> 1
            setCount == 3 -> 2
            else -> 3
        }
        cells.add(ConsistencyCell(date, level))
        calendar.add(Calendar.DATE, 1)
    }
    return cells
}

private fun computeStreakWeeks(
    sessions: List<ExerciseSession>,
    weeks: Int,
    today: Date,
): Int {
    val currentMonday = startOfWeekMillis(today)
    val sessionMillis = sessions.mapNotNull { parseProgressDate(it.date)?.time }

    var streak = 0
    val calendar = Calendar.getInstance()
    for (week in 0 until weeks) {
        calendar.timeInMillis = currentMonday
        calendar.add(Calendar.DATE, -7 * week)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DATE, 7)
        val end = calendar.timeInMillis
        if (sessionMillis.any { it >= start && it < end }) {
            streak += 1
        } else {
            break
        }
    }
    return streak
}

private fun buildRepBuckets(sessions: List<ExerciseSession>): List<ExerciseRepBucket> {
    val counts = IntArray(5)
    for (session in sessions) {
        for (entry in session.sets) {
            val reps = entry.reps.roundToInt()
            if (reps < 1) continue
            val index = when {
                reps <= 3 -> 0
                reps <= 6 -> 1
                reps <= 10 -> 2
                reps <= 15 -> 3
                else -> 4
            }
            counts[index] += 1
        }
    }
    return listOf(
        ExerciseRepBucket("1\u20133", counts[0]),
        ExerciseRepBucket("4\u20136", counts[1]),
        ExerciseRepBucket("7\u201310", counts[2]),
        ExerciseRepBucket("11\u201315", counts[3]),
        ExerciseRepBucket("16+", counts[4]),
    )
}

private fun buildMonthlySessions(
    sessions: List<ExerciseSession>,
    monthCount: Int,
    today: Date,
): List<ExerciseMonthSessions> {
    val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    val labelFormat = SimpleDateFormat("MMM", Locale.US)

    val counts = HashMap<String, Int>()
    for (session in sessions) {
        val parsed = parseProgressDate(session.date) ?: continue
        val key = keyFormat.format(parsed)
        counts[key] = (counts[key] ?: 0) + 1
    }

    val calendar = Calendar.getInstance()
    calendar.time = today
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.add(Calendar.MONTH, -(monthCount - 1))

    val result = ArrayList<ExerciseMonthSessions>(monthCount)
    for (index in 0 until monthCount) {
        val key = keyFormat.format(calendar.time)
        val label = labelFormat.format(calendar.time)
        result.add(ExerciseMonthSessions(key, label, counts[key] ?: 0))
        calendar.add(Calendar.MONTH, 1)
    }
    return result
}

private fun parseProgressDate(value: String?): Date? {
    if (value == null || value.isBlank() || value == "0000-00-00") return null
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.isLenient = false
        format.parse(value)
    } catch (exception: Exception) {
        null
    }
}

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
