package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayList

class ExerciseCatalogTest {

    @Test
    fun defaultKnownExercises_hasExpectedCountAndEntries() {
        val exercises = defaultKnownExercises()

        assertEquals(36, exercises.size)
        val squat = exercises.first { it.Name == "Squat" }
        assertEquals("Legs", squat.BodyPart)
        assertEquals(false, squat.favorite)
        assertTrue(exercises.any { it.Name == "Hammstring Leg Curl" })
        assertTrue(exercises.all { it.favorite == false })
    }

    @Test
    fun dedupeKnownExercises_skipsDuplicateNamesAndAppendsNewOnesInSetOrder() {
        val benchSet = WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0)
        val squatSet = WorkoutSet("2026-01-01", "Squat", "Legs", 5.0, 100.0)
        val benchAgain = WorkoutSet("2026-01-02", "Bench", "Chest", 6.0, 85.0)
        val curlSet = WorkoutSet("2026-01-02", "Curl", "Biceps", 8.0, 20.0)

        val firstDay = WorkoutDay()
        firstDay.Sets = arrayListOf(benchSet, squatSet)
        val secondDay = WorkoutDay()
        secondDay.Sets = arrayListOf(benchAgain, curlSet)

        val known = arrayListOf(Exercise("Squat", "Legs"))
        val result = dedupeKnownExercises(listOf(firstDay, secondDay), known)

        assertEquals(3, result.size)
        assertEquals(listOf("Squat", "Bench", "Curl"), result.map { it.Name })
        assertEquals("Chest", result[1].BodyPart)
        assertEquals("Biceps", result[2].BodyPart)

        // Input list is untouched.
        assertEquals(1, known.size)
        assertEquals("Squat", known[0].Name)
    }

    @Test
    fun categoryFor_returnsFirstMatchBodyPartOrEmptyString() {
        val known = listOf(
            Exercise("Squat", "Legs"),
            Exercise("Bench", "Chest")
        )

        assertEquals("Legs", categoryFor(known, "Squat"))
        assertEquals("Chest", categoryFor(known, "Bench"))
        assertEquals("", categoryFor(known, "Missing"))
        assertEquals("", categoryFor(known, null))
    }

    @Test
    fun isFavorite_returnsFirstMatchFavoriteOrFalse() {
        val favorite = Exercise("Squat", "Legs")
        favorite.favorite = true
        val known = listOf(
            favorite,
            Exercise("Bench", "Chest")
        )

        assertEquals(true, isFavorite(known, "Squat"))
        assertEquals(false, isFavorite(known, "Bench"))
        assertEquals(false, isFavorite(known, "Missing"))
    }

    @Test
    fun exists_matchesByNameOnly() {
        val known = listOf(Exercise("Squat", "Legs"))

        assertTrue(exists(known, "Squat"))
        assertFalse(exists(known, "squat"))
        assertFalse(exists(known, "Bench"))
        assertFalse(exists(known, null))
    }

    @Test
    fun dedupeKnownExercises_doesNotMutateDaySets() {
        val workoutSet = WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0)
        val day = WorkoutDay()
        day.Sets = arrayListOf(workoutSet)

        dedupeKnownExercises(listOf(day), ArrayList())

        assertEquals(1, day.Sets!!.size)
        assertEquals("Bench", day.Sets!![0].exerciseName)
        assertEquals("", day.Sets!![0].comment)
    }
}
