package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date

class CalendarTest {

    @Test
    fun buildInfiniteDays_endIsExclusiveAndDaysAreEmpty() {
        val format = SimpleDateFormat("yyyy-MM-dd")
        val start = format.parse("2026-01-01")!!
        val end = format.parse("2026-01-05")!!

        val days = buildInfiniteDays(start, end)

        assertEquals(4, days.size)
        assertEquals("2026-01-01", days[0].Date)
        assertEquals("2026-01-02", days[1].Date)
        assertEquals("2026-01-03", days[2].Date)
        assertEquals("2026-01-04", days[3].Date)

        for (day in days) {
            assertTrue(day.Sets!!.isEmpty())
            assertTrue(day.Exercises!!.isEmpty())
            assertEquals(0.0, day.DayVolume!!, 0.001)
            assertEquals(0, day.Reps)
        }
    }

    @Test
    fun todayDateString_matchesCanonicalFormat() {
        val expected = SimpleDateFormat("yyyy-MM-dd").format(Date())
        assertEquals(expected, todayDateString())
    }

    @Test
    fun sortedByDate_sortsNewListWithoutMutatingInput() {
        val day3 = WorkoutDay()
        day3.Date = "2026-01-03"
        val day1 = WorkoutDay()
        day1.Date = "2026-01-01"
        val day2 = WorkoutDay()
        day2.Date = "2026-01-02"

        val input = listOf(day3, day1, day2)
        val sorted = sortedByDate(input)

        assertEquals(listOf("2026-01-01", "2026-01-02", "2026-01-03"), sorted.map { it.Date })
        assertEquals(listOf("2026-01-03", "2026-01-01", "2026-01-02"), input.map { it.Date })
        assertTrue(sorted !== input)
    }

    @Test
    fun sortedByDate_unparseableDatesFallBackAndStayStable() {
        val invalid1 = WorkoutDay()
        invalid1.Date = "not-a-date"
        val invalid2 = WorkoutDay()
        invalid2.Date = "also-not-a-date"

        val sorted = sortedByDate(listOf(invalid1, invalid2))

        assertEquals(2, sorted.size)
        assertTrue(sorted[0] === invalid1)
        assertTrue(sorted[1] === invalid2)
    }

    @Test
    fun sortedByDate_equalDatesKeepInputOrder() {
        val first = WorkoutDay()
        first.Date = "2026-01-01"
        first.Reps = 1
        val second = WorkoutDay()
        second.Date = "2026-01-01"
        second.Reps = 2

        val sorted = sortedByDate(listOf(first, second))

        assertTrue(sorted[0] === first)
        assertTrue(sorted[1] === second)
    }
}
