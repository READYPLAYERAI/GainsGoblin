package com.readyplayerai.gainsgoblin

import com.readyplayerai.gainsgoblin.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean

import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test
import org.junit.Assert.*

import java.util.ArrayList

class SerializationGoldenTest {

    private val kotlinxJson = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---- WorkoutSet ----

    @Test
    fun workoutSet_keySet() {
        val set = WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0, "good")
        set.id = 7
        set.user_id = 9

        val json = kotlinxJson.encodeToString(set)
        val obj = kotlinxJson.parseToJsonElement(json).jsonObject

        val expected = setOf(
            "id", "date", "exercise_name", "category", "reps", "weight", "comment", "user_id"
        )
        assertEquals(expected, obj.keys)
        assertEquals(7, obj["id"]!!.jsonPrimitive.int)
        assertEquals(9, obj["user_id"]!!.jsonPrimitive.int)
        assertEquals("2026-01-01", obj["date"]!!.jsonPrimitive.content)
        assertEquals("Bench", obj["exercise_name"]!!.jsonPrimitive.content)
        assertEquals("Chest", obj["category"]!!.jsonPrimitive.content)
        assertEquals(5.0, obj["reps"]!!.jsonPrimitive.double, 0.001)
        assertEquals(80.0, obj["weight"]!!.jsonPrimitive.double, 0.001)
        assertEquals("good", obj["comment"]!!.jsonPrimitive.content)
    }

    @Test
    fun workoutSet_partialDeserialization() {
        val partialJson = "{\"date\":\"2026-01-01\",\"exercise_name\":\"Bench\",\"category\":\"Chest\",\"reps\":5.0,\"weight\":80.0}"
        val set = kotlinxJson.decodeFromString<WorkoutSet>(partialJson)

        assertEquals("2026-01-01", set.date)
        assertEquals("Bench", set.exerciseName)
        assertEquals("Chest", set.category)
        assertEquals(5.0, set.reps!!, 0.001)
        assertEquals(80.0, set.weight!!, 0.001)
        // Missing fields use their defaults: comment null, id 0, user_id 0
        assertNull(set.comment)
        assertEquals(0, set.id)
        assertEquals(0, set.user_id)
    }

    @Test
    fun workoutSet_roundTrip() {
        val original = WorkoutSet("2026-01-01", "Bench", "Chest", 5.0, 80.0, "good")
        original.id = 7
        original.user_id = 9

        val json = kotlinxJson.encodeToString(original)
        val round = kotlinxJson.decodeFromString<WorkoutSet>(json)

        assertEquals(original.date, round.date)
        assertEquals(original.exerciseName, round.exerciseName)
        assertEquals(original.category, round.category)
        assertEquals(original.reps!!, round.reps!!, 0.001)
        assertEquals(original.weight!!, round.weight!!, 0.001)
        assertEquals(original.comment, round.comment)
        assertEquals(original.id, round.id)
        assertEquals(original.user_id, round.user_id)
    }

    @Test
    fun workoutSet_decodesLegacyJson() {
        // Legacy JSON: null fields omitted (no "comment"),
        // but id/user_id written even when 0.
        val legacyJson = "{\"id\":0,\"date\":\"2026-01-02\",\"exercise_name\":\"Squat\",\"category\":\"Legs\",\"reps\":3.0,\"weight\":100.0,\"user_id\":0}"
        val set = kotlinxJson.decodeFromString<WorkoutSet>(legacyJson)

        assertEquals(0, set.id)
        assertEquals("2026-01-02", set.date)
        assertEquals("Squat", set.exerciseName)
        assertEquals("Legs", set.category)
        assertEquals(3.0, set.reps!!, 0.001)
        assertEquals(100.0, set.weight!!, 0.001)
        assertNull(set.comment)
        assertEquals(0, set.user_id)
    }

    // ---- Exercise ----

    @Test
    fun exercise_keySet() {
        val ex = Exercise("Deadlift", "Back")

        val json = kotlinxJson.encodeToString(ex)
        val obj = kotlinxJson.parseToJsonElement(json).jsonObject

        val expected = setOf("Name", "BodyPart", "favorite")
        assertEquals(expected, obj.keys)
        assertFalse(obj["favorite"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun exercise_partialDeserialization() {
        // Legacy JSON without "favorite"
        val partialJson = "{\"Name\":\"Deadlift\",\"BodyPart\":\"Back\"}"
        val ex = kotlinxJson.decodeFromString<Exercise>(partialJson)

        assertEquals("Deadlift", ex.Name)
        assertEquals("Back", ex.BodyPart)
        // kotlinx decodes the missing key to the initializer default false.
        // Production load normalizes null -> false anyway.
        assertEquals(false, ex.favorite)
    }

    @Test
    fun exercise_roundTrip() {
        val original = Exercise("Deadlift", "Back")
        original.favorite = true

        val json = kotlinxJson.encodeToString(original)
        val round = kotlinxJson.decodeFromString<Exercise>(json)

        assertEquals(original.Name, round.Name)
        assertEquals(original.BodyPart, round.BodyPart)
        assertEquals(original.favorite, round.favorite)
    }

    // ---- WorkoutDay + nested WorkoutExercise ----

    @Test
    fun workoutDay_nested_keySets() {
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
        val listSerializer = ListSerializer(WorkoutDay.serializer())
        val json = kotlinxJson.encodeToString(listSerializer, list)

        val arr = kotlinxJson.parseToJsonElement(json).jsonArray
        assertEquals(1, arr.size)
        val dayObj = arr[0].jsonObject

        // WorkoutDay top-level key set
        val dayExpected = setOf(
            "Exercises", "Sets", "DayVolume", "Date", "Reps"
        )
        assertEquals(dayExpected, dayObj.keys)

        // Nested WorkoutExercise key set
        val exercises = dayObj["Exercises"]!!.jsonArray
        assertEquals(2, exercises.size)

        val weExpected = setOf(
            "Sets", "Date", "Exercise", "Volume", "EstimatedOneRepMax",
            "ActualOneRepMax", "MaxWeight", "MaxReps", "TotalReps", "TotalSets",
            "MaxSetVolume", "VolumePR", "ActualOneRepMaxPR", "EstimatedOneRepMaxPR",
            "MaxRepsPR", "MaxWeightPR", "HTLT", "Comment",
            "maxVolumeSet", "maxRepsSet", "maxWeightSet"
        )

        for (i in 0 until 2) {
            val weObj = exercises[i].jsonObject
            assertEquals(weExpected, weObj.keys)
            // PR booleans we set serialize as true
            assertTrue(weObj["VolumePR"]!!.jsonPrimitive.boolean)
            assertTrue(weObj["HTLT"]!!.jsonPrimitive.boolean)
        }
    }

    @Test
    fun workoutDay_decodesLegacyJson() {
        // Legacy JSON from older app version — null fields (Comment where null)
        // are absent, ActualOneRepMax written since non-null.
        val legacyJson = """[{"Exercises":[{"Sets":[{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0}],"Date":"2026-01-05","Exercise":"Bench","Volume":400.0,"EstimatedOneRepMax":93.33,"ActualOneRepMax":0.0,"MaxWeight":80.0,"MaxReps":5.0,"TotalReps":5.0,"TotalSets":1.0,"MaxSetVolume":400.0,"VolumePR":false,"ActualOneRepMaxPR":false,"EstimatedOneRepMaxPR":false,"MaxRepsPR":false,"MaxWeightPR":false,"HTLT":false,"Comment":"good","maxVolumeSet":{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0},"maxRepsSet":{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0},"maxWeightSet":{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0}}],"Sets":[{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0}],"DayVolume":400.0,"Date":"2026-01-05","Reps":5}]"""

        val listSerializer = ListSerializer(WorkoutDay.serializer())
        val roundList = kotlinxJson.decodeFromString(listSerializer, legacyJson)

        assertEquals(1, roundList.size)
        val day = roundList[0]

        // Day-level fields
        assertEquals("2026-01-05", day.Date)
        assertEquals(400.0, day.DayVolume!!, 0.001)
        assertEquals(5, day.Reps)

        // One exercise with expected fields
        assertEquals(1, day.Exercises!!.size)
        val exercise = day.Exercises!![0]
        assertEquals("Bench", exercise.Exercise)
        assertEquals(400.0, exercise.Volume!!, 0.001)
        assertEquals("good", exercise.Comment)

        // Sets list size 1 with correct values
        assertEquals(1, day.Sets!!.size)
        val set = day.Sets!![0]
        assertEquals("2026-01-05", set.date)
        assertEquals("Bench", set.exerciseName)
        assertEquals("Chest", set.category)
        assertEquals(5.0, set.reps!!, 0.001)
        assertEquals(80.0, set.weight!!, 0.001)
        assertEquals("good", set.comment)
        assertEquals(0, set.id)
        assertEquals(0, set.user_id)
    }

    @Test
    fun workoutDay_roundTrip() {
        val day = WorkoutDay()
        val s1 = WorkoutSet("2026-01-05", "Bench", "Chest", 5.0, 80.0, "good")
        val s2 = WorkoutSet("2026-01-05", "Squat", "Legs", 5.0, 100.0, "")
        day.addSet(s1)
        day.addSet(s2)

        val originalList = ArrayList<WorkoutDay>()
        originalList.add(day)

        val listSerializer = ListSerializer(WorkoutDay.serializer())
        val jsonStr = kotlinxJson.encodeToString(listSerializer, originalList)
        val roundList = kotlinxJson.decodeFromString(listSerializer, jsonStr)

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

        // Compare exercises field by field
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