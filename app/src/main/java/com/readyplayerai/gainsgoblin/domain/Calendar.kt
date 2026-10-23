package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutDay
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Date

/**
 * Builds one empty [WorkoutDay] per calendar day from [startDate] until (but not
 * including) [endDate].
 *
 * Pure port of the 20-year loop in `JournalActivity.initViewPager()`. The caller owns
 * the range choice; the legacy caller used now minus five years through now plus
 * five years. Neither input date is mutated. Dates are formatted as `yyyy-MM-dd`.
 */
fun buildInfiniteDays(startDate: Date, endDate: Date): List<WorkoutDay> {
    val days = ArrayList<WorkoutDay>()
    val start = Calendar.getInstance()
    start.time = startDate
    val end = Calendar.getInstance()
    end.time = endDate

    while (start.before(end)) {
        val date = start.time
        val dateString = SimpleDateFormat("yyyy-MM-dd").format(date)

        val workoutDay = WorkoutDay()
        workoutDay.Sets = ArrayList()
        workoutDay.Exercises = ArrayList()
        workoutDay.DayVolume = 0.0
        workoutDay.Reps = 0
        workoutDay.Date = dateString
        days.add(workoutDay)

        start.add(Calendar.DATE, 1)
    }

    return days
}

/**
 * Returns today's date formatted as the app's canonical `yyyy-MM-dd` string.
 */
fun todayDateString(): String {
    return SimpleDateFormat("yyyy-MM-dd").format(Date())
}

/**
 * Returns a NEW list of [days] sorted by their `yyyy-MM-dd` date strings.
 *
 * Pure port of the legacy `sortWorkoutDaysDate()`. The input list and day objects
 * are untouched.
 *
 * Legacy quirk preserved: if parsing either date throws, BOTH comparands fall back
 * to the current `Date()` (the same value), so the comparison returns 0 and the
 * stable sort keeps their original relative order.
 */
fun sortedByDate(days: List<WorkoutDay>): List<WorkoutDay> {
    val result = ArrayList(days)
    val comparator = Comparator<WorkoutDay> { first, second ->
        var firstDate = Date()
        var secondDate = Date()
        try {
            val format = SimpleDateFormat("yyyy-MM-dd")
            firstDate = format.parse(first.Date!!)!!
            secondDate = format.parse(second.Date!!)!!
        } catch (e: Exception) {
            // Legacy quirk: one parse failure makes both comparands "now".
            val now = Date()
            firstDate = now
            secondDate = now
        }
        firstDate.compareTo(secondDate)
    }
    // Collections.sort is stable (TimSort), preserving input order for equal keys.
    Collections.sort(result, comparator)
    return result
}
