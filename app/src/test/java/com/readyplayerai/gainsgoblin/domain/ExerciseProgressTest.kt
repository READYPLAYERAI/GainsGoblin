package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExerciseProgressTest {

    private fun set(
        date: String,
        reps: Double,
        weight: Double,
        comment: String = "",
        name: String = "Bench",
    ): WorkoutSet = WorkoutSet(date, name, "Chest", reps, weight, comment)

    private fun parse(value: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.isLenient = false
        return format.parse(value)
    }

    private fun progress(
        sets: List<WorkoutSet>,
        today: String = "2026-03-15",
        name: String = "Bench",
    ): ExerciseProgress? {
        val known = listOf(Exercise("Bench", "Chest").also { it.favorite = true })
        val days = applyPrFlags(daysFromSets(sets), known)
        return exerciseProgress(days, known, name, today = parse(today))
    }

    @Test
    fun sessionsAreAscendingWithCorrectAggregates() {
        val sets = listOf(
            set("2026-03-08", 3.0, 110.0),
            set("2026-03-01", 5.0, 100.0),
            set("2026-03-08", 8.0, 100.0),
            set("2026-03-01", 10.0, 90.0),
            set("2026-03-08", 12.0, 80.0),
        )

        val result = progress(sets)!!

        assertEquals(2, result.sessions.size)
        val first = result.sessions[0]
        val second = result.sessions[1]
        assertEquals("2026-03-01", first.date)
        assertEquals("2026-03-08", second.date)

        assertEquals(1400.0, first.volume, 1e-6)
        assertEquals(15.0, first.totalReps, 1e-6)
        assertEquals(100.0, first.topWeight, 1e-6)
        assertEquals(5.0, first.topReps, 1e-6)
        assertEquals(10.0, first.maxReps, 1e-6)
        assertEquals(120.0, first.estimatedOneRepMax, 1e-6)

        assertEquals(2090.0, second.volume, 1e-6)
        assertEquals(23.0, second.totalReps, 1e-6)
        assertEquals(110.0, second.topWeight, 1e-6)
        assertEquals(3.0, second.topReps, 1e-6)
        assertEquals(12.0, second.maxReps, 1e-6)
        assertEquals(126.6666667, second.estimatedOneRepMax, 1e-6)

        assertEquals(listOf(1, 2), first.sets.map { it.index })
        assertEquals(listOf(1, 2, 3), second.sets.map { it.index })
        assertEquals(listOf(100.0, 90.0), first.sets.map { it.weight })
        assertEquals(listOf(110.0, 100.0, 80.0), second.sets.map { it.weight })
        assertEquals("2026-03-01", result.firstDate)
        assertEquals("2026-03-08", result.lastDate)
    }

    @Test
    fun sessionsInRange_filtersByCalendarMonths() {
        val sets = listOf(
            set("2026-01-10", 5.0, 100.0),
            set("2026-02-10", 5.0, 100.0),
            set("2026-02-15", 5.0, 100.0),
            set("2026-03-01", 5.0, 100.0),
            set("2025-12-15", 5.0, 100.0),
        )
        val sessions = progress(sets, today = "2026-03-15")!!.sessions
        val today = parse("2026-03-15")

        fun dates(range: ExerciseRange): List<String> =
            exerciseSessionsInRange(sessions, range, today).map { it.date }

        assertEquals(listOf("2026-02-15", "2026-03-01"), dates(ExerciseRange.ONE_MONTH))
        val threeMonths = dates(ExerciseRange.THREE_MONTHS)
        assertEquals(
            listOf("2025-12-15", "2026-01-10", "2026-02-10", "2026-02-15", "2026-03-01"),
            threeMonths
        )
        // The exact 3M cutoff date is included by the `>=` comparison.
        assertTrue(threeMonths.contains("2025-12-15"))
        assertEquals(
            listOf("2025-12-15", "2026-01-10", "2026-02-10", "2026-02-15", "2026-03-01"),
            dates(ExerciseRange.SIX_MONTHS)
        )
        assertEquals(sessions, exerciseSessionsInRange(sessions, ExerciseRange.ALL, today))
    }

    @Test
    fun recordsUseStrictlyGreaterAndKeepEarlierTies() {
        val sets = listOf(
            set("2026-03-01", 5.0, 100.0),
            set("2026-03-08", 3.0, 100.0),
            set("2026-03-15", 2.0, 105.0),
        )

        val result = progress(sets)!!

        val maxWeight = result.records.first { it.kind == ExercisePrKind.MAX_WEIGHT }
        assertEquals(105.0, maxWeight.value, 1e-6)
        assertEquals("2026-03-15", maxWeight.date)

        val maxReps = result.records.first { it.kind == ExercisePrKind.MAX_REPS }
        assertEquals(5.0, maxReps.value, 1e-6)
        assertEquals("2026-03-01", maxReps.date)

        assertEquals(
            listOf(
                ExercisePrKind.MAX_WEIGHT,
                ExercisePrKind.EST_ONE_REP,
                ExercisePrKind.SET_VOLUME,
                ExercisePrKind.MAX_REPS,
                ExercisePrKind.VOLUME,
            ),
            result.records.map { it.kind }
        )
    }

    @Test
    fun prKindsMatchApplyPrFlags() {
        val sets = listOf(
            set("2026-03-01", 5.0, 100.0),
            set("2026-03-08", 6.0, 100.0),
        )

        val result = progress(sets)!!

        // applyPrFlags flags the first appearance against its 0.0 baseline, so the
        // first session carries every aggregate flag plus the set-level SET_VOLUME.
        assertEquals(
            setOf(
                ExercisePrKind.VOLUME,
                ExercisePrKind.EST_ONE_REP,
                ExercisePrKind.MAX_REPS,
                ExercisePrKind.MAX_WEIGHT,
                ExercisePrKind.SET_VOLUME,
            ),
            result.sessions[0].prKinds
        )
        // Session 2 raises volume, e1RM and reps but ties the 100.0 kg top weight,
        // so MAX_WEIGHT is not a PR.
        assertEquals(
            setOf(
                ExercisePrKind.VOLUME,
                ExercisePrKind.EST_ONE_REP,
                ExercisePrKind.MAX_REPS,
                ExercisePrKind.SET_VOLUME,
            ),
            result.sessions[1].prKinds
        )
    }

    @Test
    fun setLevelPrFlagsMarkFirstSetExceedingRunningMaximum() {
        val sets = listOf(
            set("2026-03-01", 5.0, 100.0),
            set("2026-03-01", 5.0, 100.0),
            set("2026-03-08", 5.0, 105.0),
        )

        val result = progress(sets)!!

        assertEquals(listOf(true, false), result.sessions[0].sets.map { it.isPr })
        assertEquals(listOf(true), result.sessions[1].sets.map { it.isPr })
    }

    @Test
    fun consistencyGridHasFullWeeksLevelsAndStreak() {
        val sets = listOf(
            set("2026-02-16", 5.0, 100.0),
            set("2026-02-16", 5.0, 100.0),
            set("2026-03-05", 5.0, 100.0),
            set("2026-03-09", 5.0, 100.0),
            set("2026-03-09", 5.0, 100.0),
            set("2026-03-09", 5.0, 100.0),
            set("2026-03-10", 5.0, 100.0),
            set("2026-03-10", 5.0, 100.0),
            set("2026-03-10", 5.0, 100.0),
            set("2026-03-10", 5.0, 100.0),
            set("2026-03-11", 5.0, 100.0),
        )
        val known = listOf(Exercise("Bench", "Chest"))

        val result = exerciseProgress(
            applyPrFlags(daysFromSets(sets), known),
            known,
            "Bench",
            today = parse("2026-03-15"),
            consistencyWeeks = 4,
        )!!

        val cells = result.consistency
        assertEquals(28, cells.size)
        assertEquals("2026-02-16", cells[0].date)
        assertEquals("2026-03-15", cells[27].date)

        fun level(date: String): Int = cells.first { it.date == date }.level
        assertEquals(1, level("2026-02-16"))
        assertEquals(1, level("2026-03-05"))
        assertEquals(2, level("2026-03-09"))
        assertEquals(3, level("2026-03-10"))
        assertEquals(1, level("2026-03-11"))

        val sessionDates = setOf(
            "2026-02-16", "2026-03-05", "2026-03-09", "2026-03-10", "2026-03-11"
        )
        for (cell in cells) {
            if (cell.date !in sessionDates) assertEquals(0, cell.level)
        }
        assertEquals(2, result.streakWeeks)
    }

    @Test
    fun repBucketsCountAllSets() {
        val reps = listOf(1.0, 2.0, 3.0, 6.0, 7.0, 10.0, 11.0, 15.0, 16.0)
        val sets = reps.map { set("2026-03-10", it, 10.0) }

        val result = progress(sets)!!

        assertEquals(
            listOf("1\u20133", "4\u20136", "7\u201310", "11\u201315", "16+"),
            result.repBuckets.map { it.label }
        )
        assertEquals(listOf(3, 1, 2, 2, 1), result.repBuckets.map { it.sets })
    }

    @Test
    fun monthlySessionsCoverLastMonthsIncludingEmpty() {
        val sets = listOf(
            set("2026-01-10", 5.0, 100.0),
            set("2026-03-01", 5.0, 100.0),
            set("2026-03-08", 5.0, 100.0),
        )
        val known = listOf(Exercise("Bench", "Chest"))

        val result = exerciseProgress(
            applyPrFlags(daysFromSets(sets), known),
            known,
            "Bench",
            today = parse("2026-03-15"),
            monthCount = 3,
        )!!

        assertEquals(
            listOf("2026-01", "2026-02", "2026-03"),
            result.monthlySessions.map { it.monthKey }
        )
        assertEquals(listOf("Jan", "Feb", "Mar"), result.monthlySessions.map { it.label })
        assertEquals(listOf(1, 0, 2), result.monthlySessions.map { it.sessions })
    }

    @Test
    fun unknownExerciseReturnsNullAndKnownUntrainedReturnsEmptyProgress() {
        assertNull(
            exerciseProgress(emptyList(), emptyList(), "Squat", today = parse("2026-03-15"))
        )

        val known = listOf(Exercise("Bench", "Chest"))
        val result = exerciseProgress(emptyList(), known, "Bench", today = parse("2026-03-15"))!!

        assertTrue(result.sessions.isEmpty())
        assertTrue(result.records.isEmpty())
        assertEquals(0, result.totalSets)
        assertNull(result.firstDate)
        assertNull(result.lastDate)
        assertEquals(0, result.streakWeeks)
        assertEquals(5, result.repBuckets.size)
        assertTrue(result.repBuckets.all { it.sets == 0 })
        assertTrue(result.monthlySessions.all { it.sessions == 0 })
    }

    @Test
    fun bodyweightWeightOneStillProducesVolumeAndOneRepMax() {
        val sets = listOf(
            set("2026-03-10", 10.0, 1.0),
            set("2026-03-10", 10.0, 1.0),
        )

        val session = progress(sets)!!.sessions.single()

        assertEquals(20.0, session.volume, 1e-6)
        assertEquals(1.0, session.topWeight, 1e-6)
        assertEquals(1.3333333, session.estimatedOneRepMax, 1e-6)
    }

    @Test
    fun sessionCommentUsesLastNonBlankComment() {
        val sets = listOf(
            set("2026-03-10", 5.0, 100.0, comment = ""),
            set("2026-03-10", 5.0, 100.0, comment = "felt strong"),
            set("2026-03-10", 5.0, 100.0, comment = "   "),
        )

        val session = progress(sets)!!.sessions.single()

        assertEquals("felt strong", session.comment)
    }

    @Test
    fun weekAveragesBucketByMonday() {
        val sets = listOf(
            set("2026-03-02", 5.0, 100.0),
            set("2026-03-03", 5.0, 200.0),
            set("2026-03-09", 5.0, 300.0),
        )

        val sessions = progress(sets)!!.sessions
        val averages = exerciseWeekAverages(sessions, ExerciseMetric.TOP_SET)

        assertEquals(2, averages.size)
        assertEquals("2026-03-02", averages[0].weekStart)
        assertEquals(150.0, averages[0].value, 1e-6)
        assertEquals("2026-03-09", averages[1].weekStart)
        assertEquals(300.0, averages[1].value, 1e-6)
    }

    @Test
    fun progressDoesNotMutateInputs() {
        val sets = listOf(
            set("2026-03-01", 5.0, 100.0),
            set("2026-03-08", 6.0, 100.0),
            set("2026-03-15", 2.0, 105.0),
        )
        val known = listOf(Exercise("Bench", "Chest"))
        val days = applyPrFlags(daysFromSets(sets), known)

        fun flags(dayIndex: Int): List<List<Boolean>> =
            days[dayIndex].Exercises!!.map { exercise ->
                listOf(
                    exercise.VolumePR,
                    exercise.EstimatedOneRepMaxPR,
                    exercise.MaxWeightPR,
                    exercise.MaxRepsPR,
                    exercise.ActualOneRepMaxPR,
                )
            }

        val capturedFlags = days.indices.map { flags(it) }
        val capturedSetSizes = days.map { it.Sets!!.size }

        exerciseProgress(days, known, "Bench", today = parse("2026-03-15"))

        for (index in days.indices) {
            assertEquals(capturedFlags[index], flags(index))
            assertEquals(capturedSetSizes[index], days[index].Sets!!.size)
        }
    }

    @Test
    fun dayWithRawSetsButNullExerciseSetsStillListsSetEntries() {
        val sets = listOf(
            set("2026-03-10", 5.0, 100.0),
            set("2026-03-10", 8.0, 90.0),
        )
        val known = listOf(Exercise("Bench", "Chest"))
        val days = applyPrFlags(daysFromSets(sets), known)
        days.single().Exercises!!.single().Sets = null

        val result = exerciseProgress(days, known, "Bench", today = parse("2026-03-15"))!!

        assertEquals(listOf(1, 2), result.sessions.single().sets.map { it.index })
        assertEquals(listOf(100.0, 90.0), result.sessions.single().sets.map { it.weight })
        assertEquals(2, result.totalSets)
    }
}
