package com.readyplayerai.gainsgoblin

import com.readyplayerai.gainsgoblin.model.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import org.junit.Test
import org.junit.Assert.*

import java.lang.reflect.Type
import java.util.ArrayList

class SerializationGoldenTest {

    // ---- WorkoutSet ----

    @Test
    fun workoutSet_keySet() {
        val gson = Gson()
        val set = WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0, "good")
        set.id = 7
        set.user_id = 9

        val json = gson.toJson(set)
        val obj = JsonParser.parseString(json).asJsonObject

        val expected = setOf(
            "id", "date", "exercise_name", "category", "reps", "weight", "comment", "user_id"
        )
        assertEquals(expected, obj.keySet())
        assertEquals(7, obj["id"]!!.asInt)
        assertEquals(9, obj["user_id"]!!.asInt)
    }

    @Test
    fun workoutSet_partialDeserialization() {
        val gson = Gson()
        val partialJson = "{\"date\":\"2026-01-01\",\"exercise_name\":\"Bench\",\"category\":\"Chest\",\"reps\":5.0,\"weight\":80.0}"
        val set = gson.fromJson(partialJson, WorkoutSet::class.java)

        assertEquals("2026-01-01", set.date)
        assertEquals("Bench", set.exerciseName)
        assertEquals("Chest", set.category)
        assertEquals(5.0, set.reps!!, 0.001)
        assertEquals(80.0, set.weight!!, 0.001)
        // Missing fields: comment null, id 0, user_id 0
        assertNull(set.comment)
        assertEquals(0, set.id)
        assertEquals(0, set.user_id)
    }

    @Test
    fun workoutSet_roundTrip() {
        val gson = Gson()
        val original = WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0, "good")
        original.id = 7
        original.user_id = 9

        val json = gson.toJson(original)
        val round = gson.fromJson(json, WorkoutSet::class.java)

        assertEquals(original.date, round.date)
        assertEquals(original.exerciseName, round.exerciseName)
        assertEquals(original.category, round.category)
        assertEquals(original.reps!!, round.reps!!, 0.001)
        assertEquals(original.weight!!, round.weight!!, 0.001)
        assertEquals(original.comment, round.comment)
        assertEquals(original.id, round.id)
        assertEquals(original.user_id, round.user_id)
    }

    // ---- Exercise ----

    @Test
    fun exercise_keySet() {
        val gson = Gson()
        val ex = Exercise("Deadlift", "Back")

        val json = gson.toJson(ex)
        val obj = JsonParser.parseString(json).asJsonObject

        val expected = setOf("Name", "BodyPart", "favorite")
        assertEquals(expected, obj.keySet())
        assertFalse(obj["favorite"]!!.asBoolean)
    }

    @Test
    fun exercise_partialDeserialization() {
        val gson = Gson()
        // Legacy JSON without "favorite"
        val partialJson = "{\"Name\":\"Deadlift\",\"BodyPart\":\"Back\"}"
        val ex = gson.fromJson(partialJson, Exercise::class.java)

        assertEquals("Deadlift", ex.Name)
        assertEquals("Back", ex.BodyPart)
        assertNull(ex.favorite)
    }

    @Test
    fun exercise_roundTrip() {
        val gson = Gson()
        val original = Exercise("Deadlift", "Back")
        original.favorite = true

        val json = gson.toJson(original)
        val round = gson.fromJson(json, Exercise::class.java)

        assertEquals(original.Name, round.Name)
        assertEquals(original.BodyPart, round.BodyPart)
        assertEquals(original.favorite, round.favorite)
    }

    // ---- WorkoutDay + nested WorkoutExercise ----

    @Test
    fun workoutDay_nested_keySets() {
        val gson = Gson()

        val day = WorkoutDay()
        val s1 = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good")
        val s2 = WorkoutSet("2026-01-05", "Bench", "Chest", 3.0, 100.0, "")
        val s3 = WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0, "")
        val s4 = WorkoutSet("2026-01-05", "Squat", "Legs", 2.0, 120.0, "hard")
        day.addSet(s1)
        day.addSet(s2)
        day.addSet(s3)
        day.addSet(s4)

        // Set PR flags on the WorkoutExercise entries (after UpdateData)
        for (ex in day.Exercises!!) {
            ex.VolumePR = true
            ex.HTLT = true
        }
        day.Exercises!![0].ActualOneRepMaxPR = true
        day.Exercises!![1].MaxRepsPR = true

        // Serialize as ArrayList<WorkoutDay>, mirroring production
        val list = ArrayList<WorkoutDay>()
        list.add(day)
        val json = gson.toJson(list)

        val arr = JsonParser.parseString(json).asJsonArray
        assertEquals(1, arr.size())
        val dayObj = arr[0].asJsonObject

        // WorkoutDay top-level key set
        val dayExpected = setOf(
            "Exercises", "Sets", "DayVolume", "Date", "Reps"
        )
        assertEquals(dayExpected, dayObj.keySet())

        // Nested WorkoutExercise key set
        val exercises = dayObj["Exercises"]!!.asJsonArray
        assertEquals(2, exercises.size())

        val weExpected = setOf(
            "Sets", "Date", "Exercise", "Volume", "EstimatedOneRepMax",
            "ActualOneRepMax", "MaxWeight", "MaxReps", "TotalReps", "TotalSets",
            "MaxSetVolume", "VolumePR", "ActualOneRepMaxPR", "EstimatedOneRepMaxPR",
            "MaxRepsPR", "MaxWeightPR", "HTLT", "Comment",
            "maxVolumeSet", "maxRepsSet", "maxWeightSet"
        )

        for (i in 0 until 2) {
            val weObj = exercises[i].asJsonObject
            assertEquals(weExpected, weObj.keySet())
            // PR booleans we set serialize as true
            assertTrue(weObj["VolumePR"]!!.asBoolean)
            assertTrue(weObj["HTLT"]!!.asBoolean)
        }
    }

    @Test
    fun workoutDay_roundTrip() {
        val gson = Gson()

        val day = WorkoutDay()
        val s1 = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good")
        val s2 = WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0, "")
        day.addSet(s1)
        day.addSet(s2)

        val originalList = ArrayList<WorkoutDay>()
        originalList.add(day)

        // Serialize using plain Gson (no TypeToken needed for serialization)
        val json = gson.toJson(originalList)

        // Deserialize using TypeToken, exactly like production's loadWorkoutData
        val type: Type = object : TypeToken<ArrayList<WorkoutDay>>() {}.type
        val roundList: ArrayList<WorkoutDay> = gson.fromJson(json, type)

        assertEquals(1, roundList.size)
        val roundDay = roundList[0]

        // Day-level fields
        assertEquals(day.Date, roundDay.Date)
        assertEquals(day.DayVolume!!, roundDay.DayVolume!!, 0.001)
        assertEquals(day.Reps, roundDay.Reps)
        assertEquals(day.Sets!!.size, roundDay.Sets!!.size)
        assertEquals(day.Exercises!!.size, roundDay.Exercises!!.size)

        // Compare sets field by field
        for (i in 0 until day.Sets!!.size) {
            val origSet = day.Sets!![i]
            val roundSet = roundDay.Sets!![i]
            assertEquals(origSet.date, roundSet.date)
            assertEquals(origSet.exerciseName, roundSet.exerciseName)
            assertEquals(origSet.category, roundSet.category)
            assertEquals(origSet.reps!!, roundSet.reps!!, 0.001)
            assertEquals(origSet.weight!!, roundSet.weight!!, 0.001)
            assertEquals(origSet.comment, roundSet.comment)
        }

        // Compare exercise fields field by field
        for (i in 0 until day.Exercises!!.size) {
            val origEx = day.Exercises!![i]
            val roundEx = roundDay.Exercises!![i]
            assertEquals(origEx.Exercise, roundEx.Exercise)
            assertEquals(origEx.Date, roundEx.Date)
            assertEquals(origEx.Volume!!, roundEx.Volume!!, 0.001)
            assertEquals(origEx.EstimatedOneRepMax!!, roundEx.EstimatedOneRepMax!!, 0.001)
            assertEquals(origEx.ActualOneRepMax!!, roundEx.ActualOneRepMax!!, 0.001)
            assertEquals(origEx.MaxWeight!!, roundEx.MaxWeight!!, 0.001)
            assertEquals(origEx.MaxReps!!, roundEx.MaxReps!!, 0.001)
            assertEquals(origEx.TotalReps!!, roundEx.TotalReps!!, 0.001)
            assertEquals(origEx.TotalSets!!, roundEx.TotalSets!!, 0.001)
            assertEquals(origEx.MaxSetVolume!!, roundEx.MaxSetVolume!!, 0.001)
            assertEquals(origEx.Comment, roundEx.Comment)
        }
    }
}