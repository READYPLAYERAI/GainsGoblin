package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayList

class PersonalRecordsTest {

    private fun exercise(
        name: String,
        volume: Double,
        maxSetVolume: Double,
        maxReps: Double,
        maxWeight: Double,
        actualOneRepMax: Double,
        estimatedOneRepMax: Double
    ): WorkoutExercise {
        val result = WorkoutExercise()
        result.Exercise = name
        result.Volume = volume
        result.MaxSetVolume = maxSetVolume
        result.MaxReps = maxReps
        result.MaxWeight = maxWeight
        result.ActualOneRepMax = actualOneRepMax
        result.EstimatedOneRepMax = estimatedOneRepMax
        return result
    }

    private fun day(date: String, vararg exercises: WorkoutExercise): WorkoutDay {
        val result = WorkoutDay()
        result.Date = date
        result.Sets = ArrayList()
        result.Exercises = arrayListOf(*exercises)
        return result
    }

    private fun threeDayBenchFixture(): List<WorkoutDay> {
        // These hand-built aggregates cannot be derived from real sets (day 1's e1RM
        // exceeds anything its MaxReps/MaxWeight could produce), so the ACTUAL
        // achieving sets the scan reads (maxRepsSet / maxWeightSet) are attached
        // directly, mirroring what updateDay would populate for a real day.
        val bench1 = exercise("Bench", 100.0, 100.0, 10.0, 20.0, 20.0, 100.0)
        bench1.maxRepsSet = WorkoutSet("2026-01-01", "Bench", "Chest", 10.0, 20.0, "day-1 reps")
        bench1.maxWeightSet = WorkoutSet("2026-01-01", "Bench", "Chest", 10.0, 20.0, "day-1 weight")
        val bench2 = exercise("Bench", 200.0, 400.0, 12.0, 30.0, 30.0, 150.0)
        bench2.maxRepsSet = WorkoutSet("2026-01-02", "Bench", "Chest", 12.0, 30.0, "day-2 reps")
        bench2.maxWeightSet = WorkoutSet("2026-01-02", "Bench", "Chest", 12.0, 30.0, "day-2 weight")
        val bench3 = exercise("Bench", 200.0, 360.0, 12.0, 30.0, 30.0, 150.0)
        bench3.maxRepsSet = WorkoutSet("2026-01-03", "Bench", "Chest", 12.0, 30.0, "day-3 reps")
        bench3.maxWeightSet = WorkoutSet("2026-01-03", "Bench", "Chest", 12.0, 30.0, "day-3 weight")
        return listOf(
            day("2026-01-01", bench1),
            day("2026-01-02", bench2),
            day("2026-01-03", bench3)
        )
    }

    @Test
    fun calculatePersonalRecords_runningMaximaStrictComparisonAndKnownOnlyMaps() {
        val days = threeDayBenchFixture().toMutableList()
        val unknown = exercise("Curl", 999.0, 999.0, 99.0, 99.0, 99.0, 999.0)
        days[2].Exercises!!.add(unknown)

        val known = listOf(
            Exercise("Bench", "Chest"),
            Exercise("Squat", "Legs"),
            Exercise("Deadlift", "Back")
        )

        val records = calculatePersonalRecords(days, known)

        // Strict `>` means the equal day-3 values do not replace day 2.
        assertEquals(200.0, records.volumePRs["Bench"]!!, 0.001)
        assertEquals(30.0, records.actualOneRepMaxPRs["Bench"]!!, 0.001)
        assertEquals(150.0, records.estimatedOneRMPRs["Bench"]!!, 0.001)
        assertEquals(12.0, records.maxRepsPRs["Bench"]!!, 0.001)
        assertEquals(30.0, records.maxWeightPRs["Bench"]!!, 0.001)
        assertEquals(Pair(12.0, 30.0), records.setVolumePRs["Bench"])

        // Every known exercise gets scalar entries, even without data.
        for (name in listOf("Bench", "Squat", "Deadlift")) {
            assertNotNull(records.volumePRs[name])
            assertNotNull(records.setVolumePRs[name])
            assertNotNull(records.actualOneRepMaxPRs[name])
            assertNotNull(records.estimatedOneRMPRs[name])
            assertNotNull(records.maxRepsPRs[name])
            assertNotNull(records.maxWeightPRs[name])
            assertNotNull(records.lastTimeVolume[name])
        }
        assertEquals(0.0, records.volumePRs["Squat"]!!, 0.001)
        assertEquals(Pair(0.0, 0.0), records.setVolumePRs["Squat"])

        // Unknown "Curl" is ignored entirely.
        assertFalse(records.volumePRs.containsKey("Curl"))
        assertFalse(records.maxVolumeSetPRs.containsKey("Curl"))

        // Last matching day's volume, not the maximum.
        assertEquals(200.0, records.lastTimeVolume["Bench"]!!, 0.001)

        // Corrected parity: maxRepsSetPRs / maxWeightSetPRs hold the ACTUAL achieving
        // set instances from the day exercise. Strict `>` means day 2 wins and the
        // equal day-3 values never replace it.
        val expectedRepsAchiever = days[1].Exercises!![0].maxRepsSet!!
        val maxRepsPrSet = records.maxRepsSetPRs["Bench"]!!
        assertSame(expectedRepsAchiever, maxRepsPrSet)
        assertEquals("2026-01-02", maxRepsPrSet.date)
        assertEquals("Bench", maxRepsPrSet.exerciseName)
        assertEquals("Chest", maxRepsPrSet.category)
        assertEquals(12.0, maxRepsPrSet.reps!!, 0.001)
        assertEquals(30.0, maxRepsPrSet.weight!!, 0.001)
        assertEquals("day-2 reps", maxRepsPrSet.comment)

        val expectedWeightAchiever = days[1].Exercises!![0].maxWeightSet!!
        val maxWeightPrSet = records.maxWeightSetPRs["Bench"]!!
        assertSame(expectedWeightAchiever, maxWeightPrSet)
        assertEquals("2026-01-02", maxWeightPrSet.date)
        assertEquals("Bench", maxWeightPrSet.exerciseName)
        assertEquals("Chest", maxWeightPrSet.category)
        assertEquals(12.0, maxWeightPrSet.reps!!, 0.001)
        assertEquals(30.0, maxWeightPrSet.weight!!, 0.001)
        assertEquals("day-2 weight", maxWeightPrSet.comment)

        // Legacy temp_set quirk applies ONLY to maxVolumeSetPRs: a stub carrying just
        // reps/weight, with every other field left null/default.
        val maxVolumePrSet = records.maxVolumeSetPRs["Bench"]!!
        assertEquals(12.0, maxVolumePrSet.reps!!, 0.001)
        assertEquals(30.0, maxVolumePrSet.weight!!, 0.001)
        assertNull(maxVolumePrSet.exerciseName)
        assertNull(maxVolumePrSet.date)
        assertNull(maxVolumePrSet.category)
        assertNull(maxVolumePrSet.comment)
        assertEquals(0, maxVolumePrSet.id)
        assertEquals(0, maxVolumePrSet.user_id)
    }

    @Test
    fun calculatePersonalRecords_omitsSetMapEntriesWhenAchievingSetIsNull() {
        // A hand-built exercise (never passed through updateDay) has null maxRepsSet
        // and maxWeightSet, so legacy's `?.let` leaves no entry in the set maps.
        val bench = exercise("Bench", 100.0, 100.0, 10.0, 20.0, 20.0, 100.0)
        assertNull(bench.maxRepsSet)
        assertNull(bench.maxWeightSet)

        val records = calculatePersonalRecords(
            listOf(day("2026-01-01", bench)),
            listOf(Exercise("Bench", "Chest"))
        )

        assertEquals(10.0, records.maxRepsPRs["Bench"]!!, 0.001)
        assertEquals(20.0, records.maxWeightPRs["Bench"]!!, 0.001)
        assertFalse(records.maxRepsSetPRs.containsKey("Bench"))
        assertFalse(records.maxWeightSetPRs.containsKey("Bench"))

        // The maxVolumeSetPRs temp_set stub is still produced.
        val maxVolumePrSet = records.maxVolumeSetPRs["Bench"]!!
        assertEquals(10.0, maxVolumePrSet.reps!!, 0.001)
        assertEquals(20.0, maxVolumePrSet.weight!!, 0.001)
    }

    @Test
    fun calculatePersonalRecords_estimatedOneRepMaxUsesEpleyFormula() {
        val workoutSet = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0)
        val day = WorkoutDay()
        day.Sets = arrayListOf(workoutSet)
        val aggregated = updateDay(day)

        val records = calculatePersonalRecords(
            listOf(aggregated),
            listOf(Exercise("Bench", "Chest"))
        )

        // 80 * (1 + 5/30) = 93.333...
        assertEquals(93.3333, records.estimatedOneRMPRs["Bench"]!!, 0.001)
        assertEquals(400.0, records.volumePRs["Bench"]!!, 0.001)
    }

    @Test
    fun applyPrFlags_marksFirstStrictMaximumAndHtltOnlyWhenVolumeIncreases() {
        val bench1 = exercise("Bench", 100.0, 100.0, 10.0, 20.0, 20.0, 100.0)
        val curl = exercise("Curl", 999.0, 999.0, 99.0, 99.0, 99.0, 999.0)
        val bench2 = exercise("Bench", 200.0, 400.0, 12.0, 30.0, 30.0, 150.0)
        val bench3 = exercise("Bench", 200.0, 360.0, 12.0, 30.0, 30.0, 150.0)
        bench3.VolumePR = true // must be reset by the copy/scan

        val days = listOf(
            day("2026-01-01", bench1, curl),
            day("2026-01-02", bench2),
            day("2026-01-03", bench3)
        )
        val known = listOf(Exercise("Bench", "Chest"), Exercise("Squat", "Legs"))

        val result = applyPrFlags(days, known)

        val resultBench1 = result[0].Exercises!![0]
        assertTrue(resultBench1.VolumePR)
        assertTrue(resultBench1.ActualOneRepMaxPR)
        assertTrue(resultBench1.EstimatedOneRepMaxPR)
        assertTrue(resultBench1.MaxRepsPR)
        assertTrue(resultBench1.MaxWeightPR)
        assertTrue(resultBench1.HTLT)

        val resultCurl = result[0].Exercises!![1]
        assertFalse(resultCurl.VolumePR)
        assertFalse(resultCurl.ActualOneRepMaxPR)
        assertFalse(resultCurl.EstimatedOneRepMaxPR)
        assertFalse(resultCurl.MaxRepsPR)
        assertFalse(resultCurl.MaxWeightPR)
        assertFalse(resultCurl.HTLT)

        val resultBench2 = result[1].Exercises!![0]
        assertTrue(resultBench2.VolumePR)
        assertTrue(resultBench2.ActualOneRepMaxPR)
        assertTrue(resultBench2.EstimatedOneRepMaxPR)
        assertTrue(resultBench2.MaxRepsPR)
        assertTrue(resultBench2.MaxWeightPR)
        assertTrue(resultBench2.HTLT)

        // Equal to day 2: no flag, and the stale input flag is cleared on the copy.
        val resultBench3 = result[2].Exercises!![0]
        assertFalse(resultBench3.VolumePR)
        assertFalse(resultBench3.ActualOneRepMaxPR)
        assertFalse(resultBench3.EstimatedOneRepMaxPR)
        assertFalse(resultBench3.MaxRepsPR)
        assertFalse(resultBench3.MaxWeightPR)
        assertFalse(resultBench3.HTLT)

        // Input days are untouched (fresh day/exercise instances, same set list).
        assertFalse(bench1.VolumePR)
        assertFalse(bench2.HTLT)
        assertSame(bench1, days[0].Exercises!![0])
        assertSame(bench3, days[2].Exercises!![0])
        assertTrue(bench3.VolumePR)
        assertSame(days[0].Sets, result[0].Sets)
    }
}
