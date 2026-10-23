package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExerciseStats(
    val lastTrainedDaysAgo: Int?,
    val totalSets: Int,
)

fun exerciseStats(
    days: List<WorkoutDay>,
    today: String = todayDateString(),
): Map<String, ExerciseStats> {
    val totals = HashMap<String, Int>()
    val latest = HashMap<String, String>()
    for (day in days) {
        val date = day.Date
        for (set in day.Sets.orEmpty()) {
            val name = set.exerciseName?.trim().orEmpty()
            if (name.isEmpty()) continue
            totals[name] = (totals[name] ?: 0) + 1
            if (date != null && date > (latest[name] ?: "")) latest[name] = date
        }
    }
    val todayMs = parseDate(today)?.time
    return totals.mapValues { (name, total) ->
        val latestDate = latest[name]
        val daysAgo = if (todayMs != null && latestDate != null) {
            val latestMs = parseDate(latestDate)?.time
            if (latestMs == null) null else (((todayMs - latestMs) / 86_400_000L).toInt()).coerceAtLeast(0)
        } else null
        ExerciseStats(lastTrainedDaysAgo = daysAgo, totalSets = total)
    }
}

private fun parseDate(value: String): Date? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.isLenient = false
        format.parse(value)
    } catch (e: Exception) {
        null
    }
}
