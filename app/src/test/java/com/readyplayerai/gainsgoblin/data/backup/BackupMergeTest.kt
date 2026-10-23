package com.readyplayerai.gainsgoblin.data.backup

import com.readyplayerai.gainsgoblin.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupMergeTest {

    private fun set(
        exerciseName: String,
        category: String? = null
    ): ImportedSet = ImportedSet(
        date = "2026-01-05",
        exerciseName = exerciseName,
        category = category,
        reps = 8.0,
        weight = 60.0,
        comment = null
    )

    @Test
    fun setDerivedExerciseHasNullFavorite() {
        val merged = mergeExercisesForImport(
            sets = listOf(set("Bench", "Chest")),
            importedExercises = emptyList()
        )

        assertEquals(1, merged.size)
        assertNull(merged[0].favorite)
    }

    @Test
    fun importedExerciseFavoriteIsPreserved() {
        val merged = mergeExercisesForImport(
            sets = listOf(set("Bench", "Chest")),
            importedExercises = listOf(Exercise("Bench", "Chest").also { it.favorite = true })
        )

        assertEquals(1, merged.size)
        assertEquals(true, merged[0].favorite)
    }

    @Test
    fun importedExerciseWithNullBodyPartGetsCategoryFromSet() {
        val merged = mergeExercisesForImport(
            sets = listOf(set("Bench", "Chest")),
            importedExercises = listOf(Exercise("Bench", null).also { it.favorite = true })
        )

        assertEquals(1, merged.size)
        assertEquals("Chest", merged[0].BodyPart)
        assertEquals(true, merged[0].favorite)
    }

    @Test
    fun importedExercisesComeFirstThenNewSetExercises() {
        val merged = mergeExercisesForImport(
            sets = listOf(set("Bench", "Chest"), set("Squat", "Legs")),
            importedExercises = listOf(Exercise("Squat", "Legs"))
        )

        assertEquals(2, merged.size)
        assertEquals("Squat", merged[0].Name)
        assertEquals("Bench", merged[1].Name)
    }

    @Test
    fun setDerivedExerciseMapsBodyPartToCategory() {
        val merged = mergeExercisesForImport(
            sets = listOf(set("Bench", "Chest")),
            importedExercises = emptyList()
        )

        assertEquals(1, merged.size)
        assertEquals("Chest", merged[0].BodyPart)
    }
}
