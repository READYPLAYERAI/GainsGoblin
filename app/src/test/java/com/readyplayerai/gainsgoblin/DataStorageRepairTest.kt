package com.readyplayerai.gainsgoblin

import com.readyplayerai.gainsgoblin.model.*
import java.util.ArrayList
import org.junit.Test
import org.junit.Assert.*

class DataStorageRepairTest {

    @Test
    fun repairBackfillsNullFieldsFromRawSets() {
        // Three sets of "Bench Press"
        val set1 = WorkoutSet("2026-01-05", "Bench Press", "Chest", 8.0, 60.0)
        val set2 = WorkoutSet("2026-01-05", "Bench Press", "Chest", 7.0, 62.5)
        val set3 = WorkoutSet("2026-01-05", "Bench Press", "Chest", 6.0, 65.0)

        val day = WorkoutDay()
        day.Sets = ArrayList()
        day.Sets!!.add(set1)
        day.Sets!!.add(set2)
        day.Sets!!.add(set3)

        val ex = WorkoutExercise()
        ex.Exercise = "Bench Press"
        // All aggregation fields left null

        day.Exercises = ArrayList()
        day.Exercises!!.add(ex)

        val ds = DataStorage()
        ds.workoutDays = ArrayList()
        ds.workoutDays!!.add(day)

        ds.repairWorkoutData()

        val repaired = day.Exercises!![0]

        // Volume = 8*60 + 7*62.5 + 6*65 = 480 + 437.5 + 390 = 1307.5
        assertEquals(1307.5, repaired.Volume!!, 0.001)
        assertEquals(21.0, repaired.TotalReps!!, 0.001)
        assertEquals(3.0, repaired.TotalSets!!, 0.001)
        assertEquals(65.0, repaired.MaxWeight!!, 0.001)
        assertEquals(8.0, repaired.MaxReps!!, 0.001)
        // MaxSetVolume = max(480, 437.5, 390) = 480.0
        assertEquals(480.0, repaired.MaxSetVolume!!, 0.001)
        // EstimatedOneRepMax = max(60*38/30, 62.5*37/30, 65*36/30) = max(76.0, 77.0833..., 78.0)
        assertEquals(78.0, repaired.EstimatedOneRepMax!!, 0.001)
        assertEquals(0.0, repaired.ActualOneRepMax!!, 0.001)
        assertEquals("2026-01-05", repaired.Date)

        assertNotNull(repaired.maxWeightSet)
        assertEquals(65.0, repaired.maxWeightSet!!.weight!!, 0.001)

        assertNotNull(repaired.maxRepsSet)
        assertEquals(8.0, repaired.maxRepsSet!!.reps!!, 0.001)

        assertNotNull(repaired.maxVolumeSet)
        assertEquals(480.0, repaired.maxVolumeSet!!.reps!! * repaired.maxVolumeSet!!.weight!!, 0.001)
    }

    @Test
    fun repairLeavesPopulatedFieldsUntouched() {
        val set1 = WorkoutSet("2026-01-05", "Bench Press", "Chest", 8.0, 60.0)
        val set2 = WorkoutSet("2026-01-05", "Bench Press", "Chest", 6.0, 65.0)

        val day = WorkoutDay()
        day.Sets = ArrayList()
        day.Sets!!.add(set1)
        day.Sets!!.add(set2)

        val sentinelSet = WorkoutSet("2026-01-05", "Sentinel", "Chest", 99.0, 999.0)

        val ex = WorkoutExercise()
        ex.Exercise = "Bench Press"
        ex.Volume = 1.0          // sentinel — must survive
        ex.MaxWeight = 2.0       // sentinel — must survive
        ex.maxRepsSet = sentinelSet  // sentinel — must survive
        // Other fields left null

        day.Exercises = ArrayList()
        day.Exercises!!.add(ex)

        val ds = DataStorage()
        ds.workoutDays = ArrayList()
        ds.workoutDays!!.add(day)

        ds.repairWorkoutData()

        val repaired = day.Exercises!![0]

        // Sentinel values survive unchanged
        assertEquals(1.0, repaired.Volume!!, 0.001)
        assertEquals(2.0, repaired.MaxWeight!!, 0.001)
        assertSame(sentinelSet, repaired.maxRepsSet)

        // Null fields still get backfilled
        assertEquals(14.0, repaired.TotalReps!!, 0.001)
        assertEquals(2.0, repaired.TotalSets!!, 0.001)
        assertEquals(8.0, repaired.MaxReps!!, 0.001)
        assertEquals(480.0, repaired.MaxSetVolume!!, 0.001)
    }

    @Test
    fun repairSkipsDaysWithNullSetsAndNoMatchingExercises() {
        val ds = DataStorage()
        ds.workoutDays = ArrayList()

        // Day with Sets == null and an exercise whose name matches no sets
        val day = WorkoutDay()
        day.Sets = null
        day.Exercises = ArrayList()
        val ex = WorkoutExercise()
        ex.Exercise = "NonMatching"
        day.Exercises!!.add(ex)
        ds.workoutDays!!.add(day)

        // Should not throw
        ds.repairWorkoutData()

        // Fields remain null
        assertNull(day.Exercises!![0].Volume)
        assertNull(day.Exercises!![0].TotalReps)
    }

    @Test
    fun repairHandlesActualOneRepMaxFromOneRepSet() {
        val ds = DataStorage()
        ds.workoutDays = ArrayList()

        // Day 1: has a 1-rep set with weight 100.0
        val day1 = WorkoutDay()
        day1.Sets = ArrayList()
        day1.Sets!!.add(WorkoutSet("2026-01-05", "Bench Press", "Chest", 1.0, 100.0))
        day1.Sets!!.add(WorkoutSet("2026-01-05", "Bench Press", "Chest", 5.0, 60.0))
        val ex1 = WorkoutExercise()
        ex1.Exercise = "Bench Press"
        day1.Exercises = ArrayList()
        day1.Exercises!!.add(ex1)
        ds.workoutDays!!.add(day1)

        // Day 2: no 1-rep sets
        val day2 = WorkoutDay()
        day2.Sets = ArrayList()
        day2.Sets!!.add(WorkoutSet("2026-01-06", "Squat", "Legs", 5.0, 80.0))
        day2.Sets!!.add(WorkoutSet("2026-01-06", "Squat", "Legs", 3.0, 90.0))
        val ex2 = WorkoutExercise()
        ex2.Exercise = "Squat"
        day2.Exercises = ArrayList()
        day2.Exercises!!.add(ex2)
        ds.workoutDays!!.add(day2)

        ds.repairWorkoutData()

        // Day 1: ActualOneRepMax should be 100.0 (from the 1-rep set)
        assertEquals(100.0, day1.Exercises!![0].ActualOneRepMax!!, 0.001)

        // Day 2: ActualOneRepMax should be 0.0 (no 1-rep sets)
        assertEquals(0.0, day2.Exercises!![0].ActualOneRepMax!!, 0.001)
    }
}