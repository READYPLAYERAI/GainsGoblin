package com.readyplayerai.gainsgoblin.data.legacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test

class LegacyDecoderTest {

    private val workoutsJson = """[{"Exercises":[{"Sets":[{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0}],"Date":"2026-01-05","Exercise":"Bench","Volume":400.0,"EstimatedOneRepMax":93.33,"ActualOneRepMax":0.0,"MaxWeight":80.0,"MaxReps":5.0,"TotalReps":5.0,"TotalSets":1.0,"MaxSetVolume":400.0,"VolumePR":false,"ActualOneRepMaxPR":false,"EstimatedOneRepMaxPR":false,"MaxRepsPR":false,"MaxWeightPR":false,"HTLT":false,"Comment":"good","maxVolumeSet":{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0},"maxRepsSet":{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0},"maxWeightSet":{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0}}],"Sets":[{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0}],"DayVolume":400.0,"Date":"2026-01-05","Reps":5}]"""

    private val benchSetJson = """{"id":0,"date":"2026-01-05","exercise_name":"Bench","category":"Chest","reps":5.0,"weight":80.0,"comment":"good","user_id":0}"""
    private val squatSetJson = """{"id":0,"date":"2026-01-05","exercise_name":"Squat","category":"Legs","reps":3.0,"weight":100.0,"comment":"hard","user_id":0}"""

    @Test
    fun decodesLegacyWorkoutJson() {
        val snapshot = decodeLegacySnapshot(workoutsJson, null)

        assertEquals(1, snapshot.setCount)
        assertEquals(1, snapshot.days.size)
        val sets = snapshot.days[0].Sets!!
        assertEquals(1, sets.size)
        val set = sets[0]
        assertEquals("2026-01-05", set.date)
        assertEquals("Bench", set.exerciseName)
        assertEquals(5.0, set.reps!!, 0.001)
        assertEquals(80.0, set.weight!!, 0.001)
        assertEquals("good", set.comment)
    }

    @Test
    fun missingOrBlankKnownJsonYieldsDefaults() {
        for (rawKnown in listOf(null, "  ")) {
            val snapshot = decodeLegacySnapshot(null, rawKnown)
            assertEquals(36, snapshot.exerciseCount)
            for (exercise in snapshot.knownExercises) {
                assertFalse(exercise.favorite!!)
            }
        }
    }

    @Test
    fun emptyKnownJsonListYieldsDefaults() {
        val snapshot = decodeLegacySnapshot(null, "[]")
        assertEquals(36, snapshot.exerciseCount)
    }

    @Test
    fun nullFavoriteBecomesFalse() {
        val snapshot = decodeLegacySnapshot(null, """[{"Name":"Bench","BodyPart":"Chest"}]""")

        assertEquals(1, snapshot.exerciseCount)
        assertFalse(snapshot.knownExercises[0].favorite!!)
    }

    @Test
    fun fingerprintIsStableAndSensitiveToSetChanges() {
        val first = decodeLegacySnapshot(workoutsJson, null)
        val second = decodeLegacySnapshot(workoutsJson, null)
        assertEquals(first.fingerprint, second.fingerprint)

        val changedWeight = decodeLegacySnapshot(
            workoutsJson.replace("\"weight\":80.0", "\"weight\":81.0"),
            null
        )
        assertNotEquals(first.fingerprint, changedWeight.fingerprint)

        val ordered = """[{"Sets":[$benchSetJson,$squatSetJson]}]"""
        val reordered = """[{"Sets":[$squatSetJson,$benchSetJson]}]"""
        val orderedSnapshot = decodeLegacySnapshot(ordered, null)
        val reorderedSnapshot = decodeLegacySnapshot(reordered, null)
        assertNotEquals(orderedSnapshot.fingerprint, reorderedSnapshot.fingerprint)
    }

    @Test
    fun malformedWorkoutsJsonThrows() {
        try {
            decodeLegacySnapshot("{not json", null)
            fail("Expected malformed workouts JSON to throw")
        } catch (expected: Exception) {
            // Expected.
        }
    }

    @Test
    fun malformedKnownJsonThrows() {
        try {
            decodeLegacySnapshot(null, "{not json")
            fail("Expected malformed known JSON to throw")
        } catch (expected: Exception) {
            // Expected.
        }
    }
}
