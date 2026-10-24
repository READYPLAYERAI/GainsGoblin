package com.readyplayerai.gainsgoblin.domain

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date

class ProgressInsightsTest {

    private fun set(date: String, name: String, reps: Double, weight: Double): WorkoutSet =
        WorkoutSet(date, name, "Test", reps, weight, "")

    private fun day(date: String, vararg sets: WorkoutSet): WorkoutDay {
        val raw = WorkoutDay()
        raw.Date = date
        raw.Sets = ArrayList(sets.toList())
        return updateDay(raw)
    }

    private fun date(year: Int, month: Int, dayOfMonth: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(year, month - 1, dayOfMonth, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun weekdayOf(dateString: String): Int {
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat("yyyy-MM-dd").parse(dateString)!!
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    @Test
    fun periodStats_weekIncludesOnlyCurrentMondayStartWeek() {
        val currentWeek = day(
            "2026-06-08",
            set("2026-06-08", "Bench", 10.0, 10.0),
            set("2026-06-08", "Squat", 5.0, 20.0)
        )
        val eightDaysEarlier = day(
            "2026-06-02",
            set("2026-06-02", "Bench", 1.0, 1.0)
        )

        val stats = periodStats(
            listOf(currentWeek, eightDaysEarlier),
            PeriodRange.WEEK,
            date(2026, 6, 10)
        )

        assertEquals(200.0, stats.volume, 0.001)
        assertEquals(2, stats.sets)
        assertEquals(1, stats.workouts)
        assertEquals("2026-06-08", stats.sinceDate)
    }

    @Test
    fun periodStats_monthIncludesDaysFromDayOneOnly() {
        val juneFirst = day("2026-06-01", set("2026-06-01", "Bench", 5.0, 2.0))
        val juneFifteenth = day("2026-06-15", set("2026-06-15", "Bench", 10.0, 2.0))
        val mayThirstyFirst = day("2026-05-31", set("2026-05-31", "Bench", 50.0, 20.0))

        val stats = periodStats(
            listOf(mayThirstyFirst, juneFirst, juneFifteenth),
            PeriodRange.MONTH,
            date(2026, 6, 15)
        )

        assertEquals(30.0, stats.volume, 0.001)
        assertEquals(2, stats.sets)
        assertEquals(2, stats.workouts)
        assertEquals("2026-06-01", stats.sinceDate)
    }

    @Test
    fun periodStats_allSumsEverythingAndHasNoDelta() {
        val juneFifth = day("2026-06-05", set("2026-06-05", "Bench", 1.0, 10.0))
        val mayTwentieth = day("2026-05-20", set("2026-05-20", "Squat", 1.0, 5.0))
        val juneFirst = day("2026-06-01", set("2026-06-01", "Deadlift", 2.0, 10.0))

        val stats = periodStats(
            listOf(juneFifth, mayTwentieth, juneFirst),
            PeriodRange.ALL,
            date(2026, 6, 15)
        )

        assertEquals(35.0, stats.volume, 0.001)
        assertEquals(3, stats.sets)
        assertEquals(3, stats.workouts)
        assertEquals(null, stats.deltaPercent)
        assertEquals("2026-05-20", stats.sinceDate)
    }

    @Test
    fun periodStats_weekDeltaComparesOnlyElapsedPartOfPreviousWeek() {
        // today is Wednesday 2026-06-10; current week Monday 2026-06-08.
        // Previous week: 200 on Monday 06-01, 500 on Friday 06-05.
        // Elapsed window ends Wednesday, so Friday must be excluded -> -50%.
        val currentMonday = day("2026-06-08", set("2026-06-08", "Bench", 10.0, 10.0))
        val previousMonday = day("2026-06-01", set("2026-06-01", "Bench", 20.0, 10.0))
        val previousFriday = day("2026-06-05", set("2026-06-05", "Bench", 50.0, 10.0))

        val stats = periodStats(
            listOf(currentMonday, previousMonday, previousFriday),
            PeriodRange.WEEK,
            date(2026, 6, 10)
        )

        assertEquals(100.0, stats.volume, 0.001)
        assertEquals(-50.0, stats.deltaPercent!!, 0.001)
    }

    @Test
    fun periodStats_monthDeltaNeverOverlapsCurrentPeriod() {
        val previousWindow = day("2026-02-10", set("2026-02-10", "Bench", 5.0, 20.0))
        val currentFirst = day("2026-03-01", set("2026-03-01", "Bench", 5.0, 10.0))
        val currentSecond = day("2026-03-02", set("2026-03-02", "Bench", 5.0, 10.0))

        val stats = periodStats(
            listOf(previousWindow, currentFirst, currentSecond),
            PeriodRange.MONTH,
            date(2026, 3, 30)
        )

        assertEquals(100.0, stats.volume, 0.001)
        assertEquals(0.0, stats.deltaPercent!!, 0.001)
    }

    @Test
    fun periodStats_prCountCountsPrFlagsButIgnoresHtlt() {
        val prExercise = WorkoutExercise()
        prExercise.Exercise = "Bench"
        prExercise.Volume = 100.0
        prExercise.MaxWeight = 50.0
        prExercise.MaxWeightPR = true

        val htltExercise = WorkoutExercise()
        htltExercise.Exercise = "Squat"
        htltExercise.Volume = 100.0
        htltExercise.HTLT = true

        val prDay = WorkoutDay()
        prDay.Date = "2026-06-01"
        prDay.Sets = ArrayList()
        prDay.Exercises = arrayListOf(prExercise)

        val htltDay = WorkoutDay()
        htltDay.Date = "2026-06-02"
        htltDay.Sets = ArrayList()
        htltDay.Exercises = arrayListOf(htltExercise)

        val stats = periodStats(listOf(prDay, htltDay), PeriodRange.ALL)

        assertEquals(1, stats.prCount)
    }

    @Test
    fun weeklyVolumeBuckets_labelsByMondayAndAscends() {
        val juneFirst = day("2026-06-01", set("2026-06-01", "Bench", 10.0, 10.0))
        val juneThird = day("2026-06-03", set("2026-06-03", "Bench", 5.0, 10.0))
        val juneEighth = day("2026-06-08", set("2026-06-08", "Bench", 20.0, 10.0))

        val buckets = weeklyVolumeBuckets(listOf(juneEighth, juneThird, juneFirst))

        assertEquals(2, buckets.size)
        assertEquals("2026-06-01", buckets[0].label)
        assertEquals(150.0f, buckets[0].value, 0.001f)
        assertEquals("2026-06-08", buckets[1].label)
        assertEquals(200.0f, buckets[1].value, 0.001f)
    }

    @Test
    fun monthlyVolumeBuckets_labelsByMonthAndAscends() {
        val may = day("2026-05-20", set("2026-05-20", "Bench", 1.0, 10.0))
        val juneFirst = day("2026-06-01", set("2026-06-01", "Bench", 2.0, 10.0))
        val juneFifteenth = day("2026-06-15", set("2026-06-15", "Bench", 3.0, 10.0))

        val buckets = monthlyVolumeBuckets(listOf(juneFifteenth, may, juneFirst))

        assertEquals(2, buckets.size)
        assertEquals("2026-05", buckets[0].label)
        assertEquals(10.0f, buckets[0].value, 0.001f)
        assertEquals("2026-06", buckets[1].label)
        assertEquals(50.0f, buckets[1].value, 0.001f)
    }

    @Test
    fun setsPerCategory_sortsByCountDescThenNameAndSkipsBlank() {
        val raw = WorkoutDay()
        raw.Date = "2026-06-01"
        raw.Sets = arrayListOf(
            WorkoutSet("2026-06-01", "Bench", "Chest", 5.0, 10.0, ""),
            WorkoutSet("2026-06-01", "Squat", "Legs", 5.0, 10.0, ""),
            WorkoutSet("2026-06-01", "LegPress", "Legs", 5.0, 10.0, ""),
            WorkoutSet("2026-06-01", "Pull", "Back", 5.0, 10.0, ""),
            WorkoutSet("2026-06-01", "Row", "Back", 5.0, 10.0, ""),
            WorkoutSet("2026-06-01", "Mystery", null, 5.0, 10.0, ""),
            WorkoutSet("2026-06-01", "Unknown", "", 5.0, 10.0, "")
        )
        val aggregated = updateDay(raw)

        val counts = setsPerCategory(listOf(aggregated))

        assertEquals(3, counts.size)
        assertEquals("Back", counts[0].category)
        assertEquals(2, counts[0].sets)
        assertEquals("Legs", counts[1].category)
        assertEquals(2, counts[1].sets)
        assertEquals("Chest", counts[2].category)
        assertEquals(1, counts[2].sets)
    }

    @Test
    fun consistencyCells_returnsWeeksTimesSevenWithTrainedLevel() {
        val trained = day("2026-06-08", set("2026-06-08", "Bench", 10.0, 10.0))

        val cells = consistencyCells(listOf(trained), 3, date(2026, 6, 10))

        assertEquals(21, cells.size)
        assertEquals("2026-05-25", cells[0].date)
        assertEquals(Calendar.MONDAY, weekdayOf(cells[0].date))
        assertEquals("2026-06-01", cells[7].date)
        val trainedCell = cells.first { it.date == "2026-06-08" }
        assertEquals(2, trainedCell.level)
        assertTrue(cells.filter { it.date != "2026-06-08" }.all { it.level == 0 })
    }

    @Test
    fun overloadDeltas_emitsWeightWhenTopWeightRises() {
        val first = day("2026-06-01", set("2026-06-01", "Bench", 5.0, 100.0))
        val second = day("2026-06-02", set("2026-06-02", "Bench", 5.0, 110.0))

        val deltas = overloadDeltas(listOf(first, second))

        assertEquals(1, deltas.size)
        val delta = deltas.getValue("2026-06-02|Bench")
        assertEquals(OverloadKind.WEIGHT, delta.kind)
        assertEquals(10.0, delta.amount, 0.001)
    }

    @Test
    fun overloadDeltas_emitsRepsWhenOnlyRepsRise() {
        val first = day("2026-06-01", set("2026-06-01", "Bench", 5.0, 100.0))
        val second = day("2026-06-02", set("2026-06-02", "Bench", 6.0, 100.0))

        val delta = overloadDeltas(listOf(first, second)).getValue("2026-06-02|Bench")

        assertEquals(OverloadKind.REPS, delta.kind)
        assertEquals(1.0, delta.amount, 0.001)
    }

    @Test
    fun overloadDeltas_emitsVolumeWhenOnlyVolumeRises() {
        val first = day("2026-06-01", set("2026-06-01", "Squat", 5.0, 100.0))
        val second = day(
            "2026-06-02",
            set("2026-06-02", "Squat", 5.0, 100.0),
            set("2026-06-02", "Squat", 5.0, 100.0)
        )

        val delta = overloadDeltas(listOf(first, second)).getValue("2026-06-02|Squat")

        assertEquals(OverloadKind.VOLUME, delta.kind)
        assertEquals(0.0, delta.amount, 0.001)
    }

    @Test
    fun overloadDeltas_emitsNothingForFirstAppearanceOrRegression() {
        val only = day("2026-06-01", set("2026-06-01", "Bench", 5.0, 100.0))
        assertTrue(overloadDeltas(listOf(only)).isEmpty())

        val first = day("2026-06-01", set("2026-06-01", "Bench", 5.0, 100.0))
        val lower = day("2026-06-02", set("2026-06-02", "Bench", 5.0, 90.0))
        assertTrue(overloadDeltas(listOf(first, lower)).isEmpty())
    }

    @Test
    fun exerciseRecords_excludesUnperformedAndOrdersFavouritesFirst() {
        val favBench = Exercise("Bench", "Chest")
        favBench.favorite = true
        val known = listOf(favBench, Exercise("Squat", "Legs"), Exercise("Never", "Back"))

        val benchFirst = day("2026-06-01", set("2026-06-01", "Bench", 5.0, 100.0))
        val squat = day("2026-06-02", set("2026-06-02", "Squat", 5.0, 100.0))
        val benchSecond = day("2026-06-03", set("2026-06-03", "Bench", 5.0, 110.0))

        val records = exerciseRecords(listOf(benchSecond, squat, benchFirst), known)

        assertEquals(2, records.size)
        assertEquals("Bench", records[0].name)
        assertEquals("Squat", records[1].name)

        val bench = records[0]
        assertEquals("Chest", bench.category)
        assertTrue(bench.favorite)
        assertEquals(110.0, bench.maxWeight, 0.001)
        assertEquals("2026-06-03", bench.maxWeightDate)
        // Both sessions have 5 reps, so the first achiever wins the tie.
        assertEquals("2026-06-01", bench.maxRepsDate)
        assertEquals(2, bench.sessions.size)
        assertEquals("2026-06-01", bench.sessions[0].date)
        assertEquals("2026-06-03", bench.sessions[1].date)
    }
}
