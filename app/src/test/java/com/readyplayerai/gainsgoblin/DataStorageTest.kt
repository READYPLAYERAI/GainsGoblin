package com.readyplayerai.gainsgoblin

import com.readyplayerai.gainsgoblin.model.*
import org.junit.Test
import org.junit.Assert.*

class DataStorageTest {

    @Test
    fun initKnownExercises_hasEntries() {
        val ds = DataStorage()
        ds.initKnownExercises()
        assertTrue(ds.knownExercises.size > 30)
        val first = ds.knownExercises[0]
        assertEquals("Flat Barbell Bench Press", first.Name)
        assertEquals("Chest", first.BodyPart)
        // Every exercise from initKnownExercises has favorite = Boolean.FALSE
        assertFalse(first.favorite!!)
        for (e in ds.knownExercises) {
            assertFalse(e.favorite!!)
        }
    }

    @Test
    fun csvToSets_convertsRows() {
        val ds = DataStorage()

        // Build the raw list matching the CSV column order:
        // Date, Exercise, Category, Weight (kg), Reps, Comment
        val csvList = mutableListOf(
            arrayOf("Date", "Exercise", "Category", "Weight (kg)", "Reps", "Comment"),
            // 5-column row (no comment)
            arrayOf("2026-01-05", "Bench Press", "Chest", "80.0", "5"),
            // 6-column row (with comment)
            arrayOf("2026-01-05", "Squat", "Legs", "100.0", "5", "tough")
        )

        ds.csvToSets(csvList)

        assertEquals(2, ds.sets.size)

        // First row: 5 columns -> comment defaults to ""
        val first = ds.sets[0]
        assertEquals("2026-01-05", first.date)
        assertEquals("Bench Press", first.exerciseName)
        assertEquals("Chest", first.category)
        assertEquals(5.0, first.reps!!, 0.001)
        assertEquals(80.0, first.weight!!, 0.001)
        assertEquals("", first.comment)

        // Second row: 6 columns -> comment is parsed
        val second = ds.sets[1]
        assertEquals("2026-01-05", second.date)
        assertEquals("Squat", second.exerciseName)
        assertEquals("Legs", second.category)
        assertEquals(5.0, second.reps!!, 0.001)
        assertEquals(100.0, second.weight!!, 0.001)
        assertEquals("tough", second.comment)
    }

    @Test
    fun setsToEverything_groupsByDate() {
        val ds = DataStorage()
        ds.sets.add(WorkoutSet("2026-01-04", "Bench", "Chest", 5.0, 80.0))
        ds.sets.add(WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0))
        ds.sets.add(WorkoutSet("2026-01-05", "Bench", "Chest", 3.0, 80.0))

        ds.setsToEverything()

        // days is a TreeSet with both dates in ascending order
        assertEquals(2, ds.days.size)
        assertTrue(ds.days.contains("2026-01-04"))
        assertTrue(ds.days.contains("2026-01-05"))

        // workoutDays should have 2 entries
        assertEquals(2, ds.workoutDays.size)

        // First day (2026-01-04) has only Bench
        val day0 = ds.workoutDays[0]
        assertEquals("2026-01-04", day0.Date)
        assertEquals(1, day0.Sets!!.size)
        assertEquals("Bench", day0.Sets!![0].exerciseName)

        // Second day (2026-01-05) has Squat and Bench
        val day1 = ds.workoutDays[1]
        assertEquals("2026-01-05", day1.Date)
        assertEquals(2, day1.Sets!!.size)

        // Each day groups only its own date's sets
        for (day in ds.workoutDays) {
            for (s in day.Sets!!) {
                assertEquals(day.Date, s.date)
            }
        }
    }

    @Test
    fun getDayPosition_foundAndNotFound() {
        val ds = DataStorage()
        ds.sets.add(WorkoutSet("2026-01-04", "Bench", "Chest", 5.0, 80.0))
        ds.sets.add(WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0))
        ds.setsToEverything()

        assertEquals(0, ds.getDayPosition("2026-01-04"))
        assertEquals(1, ds.getDayPosition("2026-01-05"))
        assertEquals(-1, ds.getDayPosition("2026-01-06"))
    }

    @Test
    fun getExercisePosition_foundAndNotFound() {
        val ds = DataStorage()
        ds.sets.add(WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0))
        ds.sets.add(WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0))
        ds.setsToEverything()

        assertEquals(0, ds.getExercisePosition("2026-01-05", "Bench"))
        assertEquals(1, ds.getExercisePosition("2026-01-05", "Squat"))
        assertEquals(-1, ds.getExercisePosition("2026-01-05", "NonExistent"))
        assertEquals(-1, ds.getExercisePosition("2026-01-06", "Bench"))
    }

    @Test
    fun modifySets_truncatesTimestamp() {
        val ds = DataStorage()
        val s = WorkoutSet("2026-01-05T21:32:00", "Bench", "Chest", 5.0, 80.0, "")
        ds.sets.add(s)
        ds.modifySets()
        assertEquals("2026-01-05", s.date)
    }

    @Test
    fun csvToKnownExercises_addsNewAndSkipsExisting() {
        val ds = DataStorage()
        ds.initKnownExercises()
        val baselineSize = ds.knownExercises.size

        // Add a day with an exercise NOT in the hardcoded list and one that IS
        val s1 = WorkoutSet("2026-01-05", "My Custom Row", "Back", 10.0, 80.0)
        val s2 = WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0)
        ds.sets.add(s1)
        ds.sets.add(s2)
        ds.setsToEverything()

        ds.csvToKnownExercises()

        // Should have exactly one more exercise than baseline (My Custom Row added, Deadlift already present)
        assertEquals(baselineSize + 1, ds.knownExercises.size)

        // "My Custom Row" should be present exactly once
        var customCount = 0
        for (e in ds.knownExercises) {
            if (e.Name == "My Custom Row") {
                customCount++
                assertEquals("Back", e.BodyPart)
            }
        }
        assertEquals(1, customCount)
    }

    @Test
    fun setFavoriteExercise_and_isExerciseFavorite() {
        val ds = DataStorage()
        ds.initKnownExercises()

        // Set favorite to true
        ds.setFavoriteExercise("Deadlift", true)
        assertTrue(ds.isExerciseFavorite("Deadlift")!!)

        // Set favorite back to false
        ds.setFavoriteExercise("Deadlift", false)
        assertFalse(ds.isExerciseFavorite("Deadlift")!!)

        // Unknown exercise returns false
        assertFalse(ds.isExerciseFavorite("NonExistent")!!)
    }

    @Test
    fun doesExerciseExist_knownAndUnknown() {
        val ds = DataStorage()
        ds.initKnownExercises()

        assertTrue(ds.doesExerciseExist("Deadlift"))
        assertFalse(ds.doesExerciseExist("NonExistent"))
    }

    @Test
    fun getExerciseCategory_knownAndUnknown() {
        val ds = DataStorage()
        ds.initKnownExercises()

        assertEquals("Back", ds.getExerciseCategory("Deadlift"))
        assertEquals("", ds.getExerciseCategory("NonExistent"))
    }

    @Test
    fun deleteExercise_onlyExercise_removesDay() {
        val ds = DataStorage()
        val s = WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0)
        ds.sets.add(s)
        ds.initKnownExercises()
        ds.setsToEverything()

        assertFalse(ds.workoutDays.isEmpty())

        ds.deleteExercise("Deadlift")

        // Day had only Deadlift sets -> day is removed
        assertTrue(ds.workoutDays.isEmpty())
        // Deadlift removed from knownExercises
        assertFalse(ds.doesExerciseExist("Deadlift"))
    }

    @Test
    fun deleteExercise_twoExercises_keepsDay() {
        val ds = DataStorage()
        val s1 = WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0)
        val s2 = WorkoutSet("2026-01-05", "Bench Press", "Chest", 5.0, 80.0)
        ds.sets.add(s1)
        ds.sets.add(s2)
        ds.initKnownExercises()
        ds.setsToEverything()
        // Add "Bench Press" to knownExercises (it's not in the hardcoded list)
        ds.csvToKnownExercises()

        assertEquals(1, ds.workoutDays.size)

        ds.deleteExercise("Deadlift")

        // Day survives with only Bench Press
        assertEquals(1, ds.workoutDays.size)
        assertEquals(1, ds.workoutDays[0].Exercises!!.size)
        assertEquals("Bench Press", ds.workoutDays[0].Exercises!![0].Exercise)

        // All remaining sets belong to Bench Press
        for (set in ds.workoutDays[0].Sets!!) {
            assertEquals("Bench Press", set.exerciseName)
        }

        // Deadlift removed from knownExercises
        assertFalse(ds.doesExerciseExist("Deadlift"))
    }

    @Test
    fun deleteExerciseGetSets_returnsMatchingSets() {
        val ds = DataStorage()
        val s1 = WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0)
        val s2 = WorkoutSet("2026-01-05", "Bench Press", "Chest", 5.0, 80.0)
        ds.sets.add(s1)
        ds.sets.add(s2)
        ds.initKnownExercises()
        ds.setsToEverything()

        val deadliftSets = ds.deleteExerciseGetSets("Deadlift")
        assertEquals(1, deadliftSets.size)
        assertEquals("Deadlift", deadliftSets[0].exerciseName)

        val benchSets = ds.deleteExerciseGetSets("Bench Press")
        assertEquals(1, benchSets.size)
        assertEquals("Bench Press", benchSets[0].exerciseName)

        val nonexistent = ds.deleteExerciseGetSets("NonExistent")
        assertTrue(nonexistent.isEmpty())
    }

    @Test
    fun editExercise_updatesNamesAndCategories() {
        val ds = DataStorage()
        ds.initKnownExercises()

        val s = WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0)
        ds.sets.add(s)
        ds.setsToEverything()

        ds.editExercise("Deadlift", "Deadlift New", "Upper Back")

        // knownExercises updated
        assertFalse(ds.doesExerciseExist("Deadlift"))
        assertTrue(ds.doesExerciseExist("Deadlift New"))
        assertEquals("Upper Back", ds.getExerciseCategory("Deadlift New"))

        // Top-level day sets updated
        for (set in ds.workoutDays[0].Sets!!) {
            assertEquals("Deadlift New", set.exerciseName)
            assertEquals("Upper Back", set.category)
        }

        // Nested WorkoutExercise entries and their sets updated
        for (ex in ds.workoutDays[0].Exercises!!) {
            assertEquals("Deadlift New", ex.Exercise)
            for (set in ex.Sets!!) {
                assertEquals("Deadlift New", set.exerciseName)
                assertEquals("Upper Back", set.category)
            }
        }
    }

    @Test
    fun editExerciseGetSets_returnsUpdatedSets() {
        val ds = DataStorage()
        ds.initKnownExercises()

        val s = WorkoutSet("2026-01-05", "Deadlift", "Back", 5.0, 100.0)
        ds.sets.add(s)
        ds.setsToEverything()

        val updatedSets = ds.editExerciseGetSets("Deadlift", "Deadlift New", "Upper Back")

        for (set in updatedSets) {
            assertEquals("Deadlift New", set.exerciseName)
            assertEquals("Upper Back", set.category)
        }
    }
}