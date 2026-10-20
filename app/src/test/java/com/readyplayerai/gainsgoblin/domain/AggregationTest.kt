package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayList

class AggregationTest {

    private fun set(
        date: String,
        name: String,
        reps: Double,
        weight: Double,
        comment: String? = ""
    ): WorkoutSet = WorkoutSet(date, name, "Test", reps, weight, comment)

    @Test
    fun updateDay_emptySetsResetsDerivedValuesWithoutMutatingInput() {
        val staleExercise = WorkoutExercise()
        staleExercise.Exercise = "Stale"
        val day = WorkoutDay()
        day.Sets = ArrayList()
        day.Exercises = arrayListOf(staleExercise)
        day.Date = "2026-01-01"
        day.DayVolume = 99.0
        day.Reps = 7

        val result = updateDay(day)

        assertEquals(0, result.Sets!!.size)
        assertEquals(0, result.Exercises!!.size)
        assertEquals(0.0, result.DayVolume!!, 0.001)
        assertEquals("0000-00-00", result.Date)
        assertEquals(0, result.Reps)

        // Pure: the input is untouched.
        assertEquals("2026-01-01", day.Date)
        assertEquals(99.0, day.DayVolume!!, 0.001)
        assertEquals(7, day.Reps)
        assertEquals(1, day.Exercises!!.size)
    }

    @Test
    fun updateDay_aggregatesTwoExercisesAndPreservesInput() {
        val bench1 = set("2026-01-05", "Bench", 8.0, 60.0, "bench first")
        val squat1 = set("2026-01-05", "Squat", 5.0, 100.0, "squat first")
        val bench2 = set("2026-01-05", "Bench", 7.0, 70.0, "bench middle")
        val squat2 = set("2026-01-05", "Squat", 1.0, 120.0, "squat last")
        val bench3 = set("2026-01-05", "Bench", 1.0, 75.0, "bench last")

        val day = WorkoutDay()
        day.Sets = arrayListOf(bench1, squat1, bench2, squat2, bench3)
        day.Exercises = arrayListOf()

        val result = updateDay(day)

        assertEquals("2026-01-05", result.Date)
        // Bench: 8*60 + 7*70 + 1*75 = 480 + 490 + 75 = 1045
        // Squat: 5*100 + 1*120 = 500 + 120 = 620
        assertEquals(1665.0, result.DayVolume!!, 0.001)
        // round(16) + round(6) = 22
        assertEquals(22, result.Reps)
        assertEquals(2, result.Exercises!!.size)

        val bench = result.Exercises!![0]
        val squat = result.Exercises!![1]
        assertEquals("Bench", bench.Exercise)
        assertEquals("Squat", squat.Exercise)

        assertEquals(1045.0, bench.Volume!!, 0.001)
        assertEquals(16.0, bench.TotalReps!!, 0.001)
        assertEquals(3.0, bench.TotalSets!!, 0.001)
        assertEquals(75.0, bench.MaxWeight!!, 0.001)
        assertEquals(8.0, bench.MaxReps!!, 0.001)
        assertEquals(490.0, bench.MaxSetVolume!!, 0.001)
        // max(60*(1+8/30), 70*(1+7/30), 75*(1+1/30)) = max(76, 86.333, 77.5)
        assertEquals(86.3333, bench.EstimatedOneRepMax!!, 0.001)
        // Only the reps == 1.0 set counts; 75 > 0.
        assertEquals(75.0, bench.ActualOneRepMax!!, 0.001)
        assertEquals("bench last", bench.Comment)
        assertEquals("2026-01-05", bench.Date)
        assertEquals(3, bench.Sets!!.size)
        assertSame(bench1, bench.Sets!![0])
        assertSame(bench2, bench.Sets!![1])
        assertSame(bench3, bench.Sets!![2])
        assertSame(bench2, bench.maxVolumeSet)
        assertSame(bench3, bench.maxWeightSet)

        assertEquals(620.0, squat.Volume!!, 0.001)
        assertEquals(6.0, squat.TotalReps!!, 0.001)
        assertEquals(2.0, squat.TotalSets!!, 0.001)
        assertEquals(120.0, squat.MaxWeight!!, 0.001)
        assertEquals(5.0, squat.MaxReps!!, 0.001)
        assertEquals(500.0, squat.MaxSetVolume!!, 0.001)
        // max(100*(1+5/30), 120*(1+1/30)) = max(116.667, 124) = 124
        assertEquals(124.0, squat.EstimatedOneRepMax!!, 0.001)
        assertEquals(120.0, squat.ActualOneRepMax!!, 0.001)
        assertEquals("squat last", squat.Comment)
        assertSame(squat1, squat.maxVolumeSet)
        assertSame(squat2, squat.maxWeightSet)

        // Pure: input day and sets are untouched.
        assertEquals("0000-00-00", day.Date)
        assertEquals(0.0, day.DayVolume!!, 0.001)
        assertEquals(0, day.Reps)
        assertTrue(day.Exercises!!.isEmpty())
        assertEquals(5, day.Sets!!.size)
    }

    @Test
    fun daysFromSets_groupsByDateInDateOrderAndKeepsOriginalSetOrder() {
        val d2a = set("2026-01-02", "A", 1.0, 10.0)
        val d1a = set("2026-01-01", "B", 2.0, 20.0)
        val d3a = set("2026-01-03", "C", 3.0, 30.0)
        val d2b = set("2026-01-02", "D", 4.0, 40.0)
        val d1b = set("2026-01-01", "E", 5.0, 50.0)

        val days = daysFromSets(listOf(d2a, d1a, d3a, d2b, d1b))

        assertEquals(3, days.size)
        assertEquals("2026-01-01", days[0].Date)
        assertEquals("2026-01-02", days[1].Date)
        assertEquals("2026-01-03", days[2].Date)

        assertEquals(2, days[0].Sets!!.size)
        assertSame(d1a, days[0].Sets!![0])
        assertSame(d1b, days[0].Sets!![1])

        assertEquals(2, days[1].Sets!!.size)
        assertSame(d2a, days[1].Sets!![0])
        assertSame(d2b, days[1].Sets!![1])

        assertEquals(1, days[2].Sets!!.size)
        assertSame(d3a, days[2].Sets!![0])

        // Each day is aggregated.
        assertEquals(7, days[0].Reps)
        assertEquals(3, days[2].Reps)
    }

    @Test
    fun backfillExerciseFields_fillsOnlyNullFieldsForMatchingSets() {
        val bench1 = set("2026-01-05", "Bench", 8.0, 60.0)
        val bench2 = set("2026-01-05", "Bench", 7.0, 62.5)
        val bench3 = set("2026-01-05", "Bench", 6.0, 65.0)

        val day = WorkoutDay()
        day.Sets = arrayListOf(bench1, bench2, bench3)
        day.Date = "2026-01-05"
        day.DayVolume = 123.0
        day.Reps = 21

        val bench = WorkoutExercise()
        bench.Exercise = "Bench"
        val missing = WorkoutExercise()
        missing.Exercise = "Missing"
        day.Exercises = arrayListOf(bench, missing)

        val result = backfillExerciseFields(day)

        val repaired = result.Exercises!![0]
        assertEquals(1307.5, repaired.Volume!!, 0.001)
        assertEquals(21.0, repaired.TotalReps!!, 0.001)
        assertEquals(3.0, repaired.TotalSets!!, 0.001)
        assertEquals(65.0, repaired.MaxWeight!!, 0.001)
        assertEquals(8.0, repaired.MaxReps!!, 0.001)
        assertEquals(480.0, repaired.MaxSetVolume!!, 0.001)
        assertEquals(78.0, repaired.EstimatedOneRepMax!!, 0.001)
        assertEquals(0.0, repaired.ActualOneRepMax!!, 0.001)
        assertEquals("2026-01-05", repaired.Date)
        assertEquals(65.0, repaired.maxWeightSet!!.weight!!, 0.001)
        assertEquals(8.0, repaired.maxRepsSet!!.reps!!, 0.001)
        assertEquals(
            480.0,
            repaired.maxVolumeSet!!.reps!! * repaired.maxVolumeSet!!.weight!!,
            0.001
        )

        // Quirk: an exercise with no matching sets stays exactly as-is (nulls).
        val untouched = result.Exercises!![1]
        assertEquals("Missing", untouched.Exercise)
        assertNull(untouched.Volume)
        assertNull(untouched.TotalReps)
        assertNull(untouched.MaxWeight)
        assertNull(untouched.Date)
        assertNull(untouched.maxWeightSet)

        // Day-level fields copy unchanged.
        assertEquals("2026-01-05", result.Date)
        assertEquals(123.0, result.DayVolume!!, 0.001)
        assertEquals(21, result.Reps)

        // Input exercise is untouched.
        assertNull(bench.Volume)
        assertNull(bench.maxWeightSet)
        assertSame(bench, day.Exercises!![0])
    }

    @Test
    fun backfillExerciseFields_doesNotOverwriteNonNullFields() {
        val sentinelSet = set("2026-01-05", "Sentinel", 99.0, 999.0)
        val bench1 = set("2026-01-05", "Bench", 8.0, 60.0)
        val bench2 = set("2026-01-05", "Bench", 6.0, 65.0)

        val bench = WorkoutExercise()
        bench.Exercise = "Bench"
        bench.Volume = 1.0
        bench.MaxWeight = 2.0
        bench.maxRepsSet = sentinelSet

        val day = WorkoutDay()
        day.Sets = arrayListOf(bench1, bench2)
        day.Exercises = arrayListOf(bench)

        val repaired = backfillExerciseFields(day).Exercises!![0]

        assertEquals(1.0, repaired.Volume!!, 0.001)
        assertEquals(2.0, repaired.MaxWeight!!, 0.001)
        assertSame(sentinelSet, repaired.maxRepsSet)
        // Null fields still backfilled.
        assertEquals(14.0, repaired.TotalReps!!, 0.001)
        assertEquals(2.0, repaired.TotalSets!!, 0.001)
        assertEquals(8.0, repaired.MaxReps!!, 0.001)
        assertEquals(480.0, repaired.MaxSetVolume!!, 0.001)
        assertEquals(0.0, repaired.ActualOneRepMax!!, 0.001)
    }
}
