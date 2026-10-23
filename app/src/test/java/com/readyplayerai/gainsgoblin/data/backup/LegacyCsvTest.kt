package com.readyplayerai.gainsgoblin.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LegacyCsvTest {

    private val header = "Date,Exercise,Category,Weight (kg),Reps,Comment"

    @Test
    fun mapsHeaderAndRows() {
        val result = LegacyCsv.decode("$header\n2026-01-05,Bench,Chest,8,60,good\n")

        assertEquals(1, result.sets.size)
        val set = result.sets[0]
        assertEquals("2026-01-05", set.date)
        assertEquals("Bench", set.exerciseName)
        assertEquals("Chest", set.category)
        assertEquals(8.0, set.weight, 0.001)
        assertEquals(60.0, set.reps, 0.001)
        assertEquals("good", set.comment)
    }

    @Test
    fun fiveColumnRowYieldsEmptyComment() {
        val result = LegacyCsv.decode("$header\n2026-01-05,Bench,Chest,8,60")
        assertEquals(1, result.sets.size)
        assertEquals("", result.sets[0].comment)
    }

    @Test
    fun sixColumnRowYieldsComment() {
        val result = LegacyCsv.decode("$header\n2026-01-05,Bench,Chest,8,60,good set")
        assertEquals(1, result.sets.size)
        assertEquals("good set", result.sets[0].comment)
    }

    @Test
    fun commaContainingUnquotedCommentIsJoinedBack() {
        val result = LegacyCsv.decode("$header\n2026-01-06,Squat,Legs,5,100,good,set")

        assertEquals(1, result.sets.size)
        assertEquals("good,set", result.sets[0].comment)
        assertEquals(1, result.warnings.size)
        assertEquals("Joined extra columns into the comment on 1 row(s)", result.warnings[0])
    }

    @Test
    fun quotedCommaInComment() {
        val result = LegacyCsv.decode("$header\n2026-01-06,Squat,Legs,5,100,\"good, set\"")
        assertEquals(1, result.sets.size)
        assertEquals("good, set", result.sets[0].comment)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun crlfAndBom() {
        val result = LegacyCsv.decode("\uFEFF$header\r\n2026-01-05,Bench,Chest,8,60,good\r\n")
        assertEquals(1, result.sets.size)
        assertEquals("Bench", result.sets[0].exerciseName)
    }

    @Test
    fun headerOnlyReturnsEmptySets() {
        val result = LegacyCsv.decode(header)
        assertTrue(result.sets.isEmpty())
    }

    @Test
    fun invalidWeightThrows() {
        try {
            LegacyCsv.decode("$header\n2026-01-05,Bench,Chest,heavy,60")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun invalidRepsThrows() {
        try {
            LegacyCsv.decode("$header\n2026-01-05,Bench,Chest,8,many")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun invalidDateThrows() {
        try {
            LegacyCsv.decode("$header\n2026/01/05,Bench,Chest,8,60")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun unrecognizedHeaderThrows() {
        try {
            LegacyCsv.decode("Foo,Bar,Baz,Qux,Quux\n2026-01-05,Bench,Chest,8,60")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
