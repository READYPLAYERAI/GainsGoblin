package com.readyplayerai.gainsgoblin.data.legacy

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import java.util.ArrayList
import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationPlannerTest {

    private fun set(
        date: String?,
        exerciseName: String?,
        category: String?,
        reps: Double?,
        weight: Double?,
        comment: String?
    ) = WorkoutSet(date, exerciseName, category, reps, weight, comment)

    private fun day(date: String, sets: List<WorkoutSet>): WorkoutDay {
        val day = WorkoutDay()
        day.Date = date
        day.Sets = ArrayList(sets)
        return day
    }

    private fun snapshot(days: List<WorkoutDay>, known: List<Exercise>): LegacySnapshot =
        LegacySnapshot(
            rawWorkouts = null,
            rawKnown = null,
            days = days,
            knownExercises = known,
            setCount = days.sumOf { it.Sets?.size ?: 0 },
            exerciseCount = known.size,
            fingerprint = "test-fingerprint"
        )

    @Test
    fun flattensSetsInDayOrderWithMonotonicCreatedAt() {
        val now = 1_000L
        val dayA = day(
            "2026-01-05",
            listOf(
                set("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good"),
                set("2026-01-05", "Squat", "Legs", 3.0, 100.0, "hard")
            )
        )
        val dayB = day(
            "2026-01-06",
            listOf(set("2026-01-06", "Deadlift", "Back", 1.0, 150.0, null))
        )

        val planned = MigrationPlanner.plan(snapshot(listOf(dayA, dayB), emptyList()), now)

        assertEquals(3, planned.sets.size)
        assertEquals(listOf("Bench", "Squat", "Deadlift"), planned.sets.map { it.exerciseName })
        assertEquals(listOf(now, now + 1, now + 2), planned.sets.map { it.createdAt })
    }

    @Test
    fun mapsEntityFields() {
        val planned = MigrationPlanner.plan(
            snapshot(
                listOf(
                    day(
                        "2026-01-05",
                        listOf(set("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good"))
                    )
                ),
                emptyList()
            ),
            42L
        )

        val entity = planned.sets.single()
        assertEquals("2026-01-05", entity.date)
        assertEquals("Bench", entity.exerciseName)
        assertEquals("Chest", entity.category)
        assertEquals(5.0, entity.reps, 0.001)
        assertEquals(80.0, entity.weight, 0.001)
        assertEquals("good", entity.comment)
        assertEquals(42L, entity.createdAt)
    }

    @Test
    fun preservesKnownExerciseOrderAndFavorite() {
        val bench = Exercise("Bench", "Chest").also { it.favorite = true }
        val squat = Exercise("Squat", "Legs").also { it.favorite = false }

        val planned = MigrationPlanner.plan(snapshot(emptyList(), listOf(bench, squat)), 0L)

        assertEquals(listOf("Bench", "Squat"), planned.exercises.map { it.name })
        assertEquals(listOf(0, 1), planned.exercises.map { it.sortOrder })
        assertEquals(listOf(true, false), planned.exercises.map { it.favorite })
        assertEquals(listOf("Chest", "Legs"), planned.exercises.map { it.bodyPart })
    }

    @Test
    fun emptySnapshotProducesEmptyLists() {
        val planned = MigrationPlanner.plan(snapshot(emptyList(), emptyList()), 0L)

        assertEquals(0, planned.sets.size)
        assertEquals(0, planned.exercises.size)
    }
}
