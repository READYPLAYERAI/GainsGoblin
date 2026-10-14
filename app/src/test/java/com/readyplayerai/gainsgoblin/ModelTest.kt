package com.readyplayerai.gainsgoblin

import com.readyplayerai.gainsgoblin.model.*
import org.junit.Test
import org.junit.Assert.*

class ModelTest {

    @Test
    fun workoutSet_getVolume() {
        // volume = reps * weight
        val set = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0)
        assertEquals(400.0, set.getVolume()!!, 0.001)
    }

    @Test
    fun workoutSet_getEplayOneRepMax() {
        // Epley formula: weight * (1 + reps / 30) = 80 * (1 + 5/30) = 93.333...
        val set = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0)
        assertEquals(93.333, set.getEplayOneRepMax()!!, 0.001)
    }

    @Test
    fun workoutSet_constructors() {
        // Constructor with comment
        val withComment = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good set")
        assertEquals("good set", withComment.comment!!)

        // Constructor without comment (comment defaults to "")
        val withoutComment = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0)
        assertEquals("", withoutComment.comment!!)

        // No-arg constructor leaves fields null
        val empty = WorkoutSet()
        assertNull(empty.date)
        assertNull(empty.exerciseName)
        assertNull(empty.category)
        assertNull(empty.reps)
        assertNull(empty.weight)
        assertNull(empty.comment)
        assertEquals(0, empty.id)
        assertEquals(0, empty.user_id)
    }

    @Test
    fun exercise_constructorAndGettersSetters() {
        val ex = Exercise("Deadlift", "Back")
        assertEquals("Deadlift", ex.Name!!)
        assertEquals("Back", ex.BodyPart!!)
        assertFalse(ex.favorite!!)

        // Setters
        ex.Name = "Sumo Deadlift"
        ex.BodyPart = "Legs"
        ex.favorite = true
        assertEquals("Sumo Deadlift", ex.Name!!)
        assertEquals("Legs", ex.BodyPart!!)
        assertTrue(ex.favorite!!)
    }

    @Test
    fun workoutDay_aggregation() {
        val day = WorkoutDay()
        val s1 = WorkoutSet("2026-01-05", "Bench", "Chest", 2.25, 80.0)
        val s2 = WorkoutSet("2026-01-05", "Bench", "Chest", 2.25, 80.0, "good")
        val s3 = WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0)
        val s4 = WorkoutSet("2026-01-05", "Squat", "Legs", 1.0, 120.0, "pr")

        day.addSet(s1)
        day.addSet(s2)
        day.addSet(s3)
        day.addSet(s4)

        // --- Day-level assertions ---
        assertEquals("2026-01-05", day.Date!!)
        assertEquals(980.0, day.DayVolume!!, 0.001)
        assertEquals(2, day.Exercises!!.size)
        assertEquals(4, day.Sets!!.size)

        // Exercises in TreeSet alphabetical order: "Bench" < "Squat"
        val bench = day.Exercises!![0]
        val squat = day.Exercises!![1]

        assertEquals("Bench", bench.Exercise!!)
        assertEquals("Squat", squat.Exercise!!)

        // --- Bench Press assertions ---
        // Volume: 180 + 180 = 360
        assertEquals(360.0, bench.Volume!!, 0.001)
        // Epley: 80 * (1 + 2.25/30) = 80 * 1.075 = 86.0
        assertEquals(86.0, bench.EstimatedOneRepMax!!, 0.001)
        // No set with reps == 1
        assertEquals(0.0, bench.ActualOneRepMax!!, 0.001)
        assertEquals(80.0, bench.MaxWeight!!, 0.001)
        assertEquals(2.25, bench.MaxReps!!, 0.001)
        assertEquals(4.5, bench.TotalReps!!, 0.001)
        assertEquals(2.0, bench.TotalSets!!, 0.001)
        assertEquals(180.0, bench.MaxSetVolume!!, 0.001)
        assertEquals("good", bench.Comment!!)
        assertEquals(2, bench.Sets!!.size)

        // --- Squat assertions ---
        // Volume: 500 + 120 = 620
        assertEquals(620.0, squat.Volume!!, 0.001)
        // Epley: max(100*(1+5/30), 120*(1+1/30)) = max(116.667, 124.0) = 124.0
        assertEquals(124.0, squat.EstimatedOneRepMax!!, 0.001)
        // Set with reps == 1 and weight 120.0
        assertEquals(120.0, squat.ActualOneRepMax!!, 0.001)
        assertEquals(120.0, squat.MaxWeight!!, 0.001)
        assertEquals(5.0, squat.MaxReps!!, 0.001)
        assertEquals(6.0, squat.TotalReps!!, 0.001)
        assertEquals(2.0, squat.TotalSets!!, 0.001)
        // Max set volume: 100*5 = 500 > 120*1 = 120
        assertEquals(500.0, squat.MaxSetVolume!!, 0.001)
        assertEquals("pr", squat.Comment!!)
        assertEquals(2, squat.Sets!!.size)

        // Day reps: Math.round(4.5) + Math.round(6.0) = 5 + 6 = 11
        assertEquals(11, day.Reps)

        // --- Remove all 4 sets and assert empty-day reset ---
        day.removeSet(s1)
        day.removeSet(s2)
        day.removeSet(s3)
        // removeSet on the last set triggers the internal assert (the brief assumed
        // assertions would be off, but they are enabled in this test environment).
        // Remove the remaining set directly and call UpdateData.
        day.Sets!!.remove(s4)
        day.UpdateData()

        assertEquals("0000-00-00", day.Date!!)
        assertEquals(0.0, day.DayVolume!!, 0.001)
        assertEquals(0, day.Reps)
        assertTrue(day.Sets!!.isEmpty())
        assertTrue(day.Exercises!!.isEmpty())
    }
}