package com.readyplayerai.gainsgoblin.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class FitNotesCsvTest {

    private val header = "Date,Exercise,Category,Weight (kgs),Reps,Distance,Distance Unit,Time"

    @Test
    fun parsesHeaderAndRowIgnoringDistanceAndTime() {
        val result = FitNotesCsv.decode(
            "$header\n2018-10-29,Seated Dumbbell Press,Shoulders,18.0,10,,,\n"
        )

        assertEquals(1, result.sets.size)
        val set = result.sets[0]
        assertEquals("2018-10-29", set.date)
        assertEquals("Seated Dumbbell Press", set.exerciseName)
        assertEquals("Shoulders", set.category)
        assertEquals(18.0, set.weight, 0.001)
        assertEquals(10.0, set.reps, 0.001)
        assertNull(set.comment)
    }

    @Test
    fun fiveColumnRowWorks() {
        val result = FitNotesCsv.decode("$header\n2018-10-29,Bench,Chest,18.0,10")
        assertEquals(1, result.sets.size)
        assertNull(result.sets[0].comment)
    }

    @Test
    fun invalidWeightThrows() {
        try {
            FitNotesCsv.decode("$header\n2018-10-29,Bench,Chest,heavy,10")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
