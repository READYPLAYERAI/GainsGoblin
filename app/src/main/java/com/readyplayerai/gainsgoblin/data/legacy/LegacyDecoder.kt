package com.readyplayerai.gainsgoblin.data.legacy

import com.readyplayerai.gainsgoblin.data.persistenceJson
import com.readyplayerai.gainsgoblin.domain.backfillExerciseFields
import com.readyplayerai.gainsgoblin.domain.defaultKnownExercises
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import java.security.MessageDigest
import kotlinx.serialization.builtins.ListSerializer

data class LegacySnapshot(
    val rawWorkouts: String?,
    val rawKnown: String?,
    val days: List<WorkoutDay>,
    val knownExercises: List<Exercise>,
    val setCount: Int,
    val exerciseCount: Int,
    val fingerprint: String
)

fun decodeLegacySnapshot(rawWorkouts: String?, rawKnown: String?): LegacySnapshot {
    val days = if (rawWorkouts.isNullOrBlank()) {
        emptyList()
    } else {
        persistenceJson.decodeFromString(ListSerializer(WorkoutDay.serializer()), rawWorkouts)
            .map { backfillExerciseFields(it) }
    }

    val knownExercises = if (rawKnown.isNullOrBlank()) {
        defaultKnownExercises()
    } else {
        val decoded = persistenceJson.decodeFromString(ListSerializer(Exercise.serializer()), rawKnown)
        if (decoded.isEmpty()) defaultKnownExercises() else decoded
    }
    for (exercise in knownExercises) {
        exercise.favorite = exercise.favorite ?: false
    }

    val dedupedExercises = LinkedHashMap<String, Exercise>()
    for (exercise in knownExercises) {
        val name = exercise.Name
        if (!name.isNullOrBlank()) {
            dedupedExercises.putIfAbsent(name, exercise)
        }
    }

    val sets = ArrayList<WorkoutSet>()
    for (day in days) {
        val daySets = day.Sets ?: continue
        sets.addAll(daySets)
    }

    val fingerprint = fingerprintWorkoutData(sets, dedupedExercises.values.toList())

    return LegacySnapshot(
        rawWorkouts = rawWorkouts,
        rawKnown = rawKnown,
        days = days,
        knownExercises = dedupedExercises.values.toList(),
        setCount = sets.size,
        exerciseCount = dedupedExercises.size,
        fingerprint = fingerprint
    )
}

private const val FIELD_SEPARATOR = '\u001F'
private const val SET_SEPARATOR = '\u001E'
private const val SECTION_SEPARATOR = '\u001D'

fun fingerprintWorkoutData(sets: List<WorkoutSet>, knownExercises: List<Exercise>): String {
    val canonical = StringBuilder()
    for (set in sets) {
        canonical.appendField(set.date)
        canonical.append(FIELD_SEPARATOR)
        canonical.appendField(set.exerciseName)
        canonical.append(FIELD_SEPARATOR)
        canonical.appendField(set.category)
        canonical.append(FIELD_SEPARATOR)
        canonical.appendField(set.reps)
        canonical.append(FIELD_SEPARATOR)
        canonical.appendField(set.weight)
        canonical.append(FIELD_SEPARATOR)
        canonical.appendField(set.comment)
        canonical.append(SET_SEPARATOR)
    }
    canonical.append(SECTION_SEPARATOR)
    for (exercise in knownExercises) {
        canonical.appendField(exercise.Name)
        canonical.append(FIELD_SEPARATOR)
        canonical.appendField(exercise.BodyPart)
        canonical.append(FIELD_SEPARATOR)
        canonical.appendField(exercise.favorite)
        canonical.append(SET_SEPARATOR)
    }

    val digest = MessageDigest.getInstance("SHA-256")
        .digest(canonical.toString().toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private fun StringBuilder.appendField(value: Any?) {
    append(value?.toString() ?: "<null>")
}
