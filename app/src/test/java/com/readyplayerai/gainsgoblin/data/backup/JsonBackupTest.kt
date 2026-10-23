package com.readyplayerai.gainsgoblin.data.backup

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JsonBackupTest {

    @Test
    fun roundTripPreservesSetsExercisesAndFavorites() {
        val data = BackupData(
            sets = listOf(
                WorkoutSet("2026-01-05", "Bench", "Chest", 8.0, 60.0, "good"),
                WorkoutSet("2026-01-06", "Squat", "Legs", 5.0, 100.0, null)
            ),
            exercises = listOf(
                Exercise("Bench", "Chest").also { it.favorite = true },
                Exercise("Squat", "Legs")
            )
        )

        val encoded = JsonBackup.encode(data)
        val parsed = JsonBackup.decode(encoded)

        assertEquals(2, parsed.sets.size)
        val first = parsed.sets[0]
        assertEquals("2026-01-05", first.date)
        assertEquals("Bench", first.exerciseName)
        assertEquals("Chest", first.category)
        assertEquals(8.0, first.reps, 0.001)
        assertEquals(60.0, first.weight, 0.001)
        assertEquals("good", first.comment)

        assertEquals(2, parsed.exercises.size)
        assertEquals("Bench", parsed.exercises[0].Name)
        assertEquals("Chest", parsed.exercises[0].BodyPart)
        assertTrue(parsed.exercises[0].favorite == true)
        assertEquals("Squat", parsed.exercises[1].Name)
        assertFalse(parsed.exercises[1].favorite == true)
    }

    @Test
    fun unsupportedSchemaVersionThrows() {
        val json =
            """{"schemaVersion":1,"exportedAt":"2026-01-01T00:00:00Z","exercises":[],"sets":[]}"""
        try {
            JsonBackup.decode(json)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun malformedJsonThrows() {
        try {
            JsonBackup.decode("{ not valid json")
            fail("Expected exception")
        } catch (expected: Exception) {
            // expected
        }
    }

    @Test
    fun blankExerciseNameInSetThrows() {
        val json = """
            {
              "schemaVersion": 2,
              "exportedAt": "2026-01-01T00:00:00Z",
              "exercises": [],
              "sets": [
                {"date": "2026-01-05", "exercise": "   ", "weight": 10.0, "reps": 5.0}
              ]
            }
        """.trimIndent()
        try {
            JsonBackup.decode(json)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun nullExerciseNameInSetThrows() {
        val json = """
            {
              "schemaVersion": 2,
              "exportedAt": "2026-01-01T00:00:00Z",
              "exercises": [],
              "sets": [
                {"date": "2026-01-05", "exercise": null, "weight": 10.0, "reps": 5.0}
              ]
            }
        """.trimIndent()
        try {
            JsonBackup.decode(json)
            fail("Expected exception")
        } catch (expected: Exception) {
            // expected
        }
    }
}
