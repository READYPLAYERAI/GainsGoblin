package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseStatsTest {

    private fun day(date: String?, vararg sets: WorkoutSet): WorkoutDay {
        val result = WorkoutDay()
        result.Date = date
        result.Sets = arrayListOf(*sets)
        return result
    }

    private fun set(date: String?, name: String?): WorkoutSet {
        return WorkoutSet(date, name, "Test", 5.0, 80.0)
    }

    @Test
    fun singleSetOnOneDay_countsOneAndZeroDaysAgo() {
        val days = listOf(day("2026-03-15", set("2026-03-15", "Bench")))

        val stats = exerciseStats(days, today = "2026-03-15")

        assertEquals(1, stats.getValue("Bench").totalSets)
        assertEquals(0, stats.getValue("Bench").lastTrainedDaysAgo)
    }

    @Test
    fun multipleSetsOnMultipleDays_countsTotalAndUsesGreatestDate() {
        // Deliberately out of list order: the greater date (2026-03-14) must win.
        val days = listOf(
            day("2026-03-08", set("2026-03-08", "Bench"), set("2026-03-08", "Bench")),
            day("2026-03-14", set("2026-03-14", "Bench"))
        )

        val stats = exerciseStats(days, today = "2026-03-15")

        assertEquals(3, stats.getValue("Bench").totalSets)
        assertEquals(1, stats.getValue("Bench").lastTrainedDaysAgo)
    }

    @Test
    fun nullOrBlankExerciseName_isSkipped() {
        val days = listOf(
            day(
                "2026-03-15",
                set("2026-03-15", null),
                set("2026-03-15", "   "),
                set("2026-03-15", "Bench")
            )
        )

        val stats = exerciseStats(days, today = "2026-03-15")

        assertEquals(1, stats.size)
        assertEquals(1, stats.getValue("Bench").totalSets)
    }

    @Test
    fun nullSetsList_isSkippedSafely() {
        val days = listOf(day("2026-03-15"))

        val stats = exerciseStats(days, today = "2026-03-15")

        assertEquals(0, stats.size)
    }

    @Test
    fun exerciseWithNoHistory_isAbsent() {
        val days = listOf(day("2026-03-15", set("2026-03-15", "Bench")))

        val stats = exerciseStats(days, today = "2026-03-15")

        assertFalse(stats.containsKey("Squat"))
    }

    @Test
    fun dayDifferences_areDeterministicForFixedToday() {
        val days = listOf(
            day("2026-03-15", set("2026-03-15", "Zero")),
            day("2026-03-14", set("2026-03-14", "One")),
            day("2026-03-12", set("2026-03-12", "Three")),
            day("2026-03-08", set("2026-03-08", "Seven"))
        )

        val stats = exerciseStats(days, today = "2026-03-15")

        assertEquals(0, stats.getValue("Zero").lastTrainedDaysAgo)
        assertEquals(1, stats.getValue("One").lastTrainedDaysAgo)
        assertEquals(3, stats.getValue("Three").lastTrainedDaysAgo)
        assertEquals(7, stats.getValue("Seven").lastTrainedDaysAgo)
    }

    @Test
    fun nullDayDate_stillCountsSetsButHasNoLastTrainedAge() {
        val days = listOf(day(null, set("2026-03-15", "Bench")))

        val stats = exerciseStats(days, today = "2026-03-15")

        assertEquals(1, stats.getValue("Bench").totalSets)
        assertNull(stats.getValue("Bench").lastTrainedDaysAgo)
    }

    @Test
    fun unparseableDate_yieldsNullLastTrainedAge() {
        val days = listOf(day("not-a-date", set("2026-03-15", "Bench")))

        val stats = exerciseStats(days, today = "2026-03-15")

        assertEquals(1, stats.getValue("Bench").totalSets)
        assertNull(stats.getValue("Bench").lastTrainedDaysAgo)
    }
}
