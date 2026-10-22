package com.readyplayerai.gainsgoblin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTest {

    private val header = arrayOf("Date", "Exercise", "Category", "Weight", "Reps", "Comment")

    @Test
    fun setsFromCsvRows_skipsHeaderAndHandlesOptionalCommentColumn() {
        val fiveColumnRow = arrayOf("2026-01-05", "Bench", "Chest", "8", "60")
        val sixColumnRow = arrayOf("2026-01-06", "Squat", "Legs", "5", "100", "good set")

        val sets = setsFromCsvRows(listOf(header, fiveColumnRow, sixColumnRow))

        assertEquals(2, sets.size)

        val first = sets[0]
        assertEquals("2026-01-05", first.date)
        assertEquals("Bench", first.exerciseName)
        assertEquals("Chest", first.category)
        assertEquals(8.0, first.weight!!, 0.001)
        assertEquals(60.0, first.reps!!, 0.001)
        assertEquals("", first.comment)

        val second = sets[1]
        assertEquals("2026-01-06", second.date)
        assertEquals("Squat", second.exerciseName)
        assertEquals("Legs", second.category)
        assertEquals(5.0, second.weight!!, 0.001)
        assertEquals(100.0, second.reps!!, 0.001)
        assertEquals("good set", second.comment)
    }

    @Test
    fun setsFromCsvRows_mapsExportWeightRepsColumnsCorrectly() {
        // Export layout is Date,Exercise,Category,Weight (kg),Reps,Comment,
        // so column 3 is weight and column 4 is reps.
        val exportHeader =
            arrayOf("Date", "Exercise", "Category", "Weight (kg)", "Reps", "Comment")
        val row = arrayOf(
            "2026-08-31",
            "Cable Machine Single-arm Tricep Pulldown",
            "Triceps",
            "9.1",
            "10.0",
            "",
        )

        val sets = setsFromCsvRows(listOf(exportHeader, row))

        assertEquals(1, sets.size)
        assertEquals(9.1, sets[0].weight!!, 0.001)
        assertEquals(10.0, sets[0].reps!!, 0.001)
        assertEquals("Cable Machine Single-arm Tricep Pulldown", sets[0].exerciseName)
        assertEquals("Triceps", sets[0].category)
    }

    @Test
    fun setsFromCsvRows_headerOnlyReturnsEmptyList() {
        val sets = setsFromCsvRows(listOf(header))
        assertTrue(sets.isEmpty())
    }
}
