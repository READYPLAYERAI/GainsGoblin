package com.readyplayerai.gainsgoblin.data.backup

import com.readyplayerai.gainsgoblin.data.persistenceJson
import com.readyplayerai.gainsgoblin.model.Exercise
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class JsonBackupV2(
    val schemaVersion: Int = 2,
    val exportedAt: String,
    val exercises: List<JsonExercise>,
    val sets: List<JsonSet>
)

@Serializable
data class JsonExercise(
    val name: String,
    val bodyPart: String? = null,
    val favorite: Boolean = false
)

@Serializable
data class JsonSet(
    val date: String,
    val exercise: String,
    val category: String? = null,
    val weight: Double,
    val reps: Double,
    val comment: String? = null
)

object JsonBackup {

    fun encode(data: BackupData): String {
        val backup = JsonBackupV2(
            exportedAt = utcTimestamp(),
            exercises = data.exercises.map {
                JsonExercise(
                    name = requireNotNull(it.Name),
                    bodyPart = it.BodyPart,
                    favorite = it.favorite ?: false
                )
            },
            sets = data.sets.map {
                JsonSet(
                    date = requireNotNull(it.date),
                    exercise = requireNotNull(it.exerciseName),
                    category = it.category,
                    weight = requireNotNull(it.weight),
                    reps = requireNotNull(it.reps),
                    comment = it.comment
                )
            }
        )
        return persistenceJson.encodeToString(JsonBackupV2.serializer(), backup)
    }

    fun decode(text: String): ParsedImport {
        val backup = persistenceJson.decodeFromString(JsonBackupV2.serializer(), text)
        require(backup.schemaVersion == 2) {
            "Unsupported backup schema version: ${backup.schemaVersion}"
        }

        val sets = backup.sets.map { set ->
            val date = normalizeDate(set.date)
            val exercise = set.exercise.trim()
            require(exercise.isNotBlank()) { "Exercise name must not be blank" }
            require(set.weight.isFinite()) { "Invalid weight: ${set.weight}" }
            require(set.reps.isFinite()) { "Invalid reps: ${set.reps}" }
            ImportedSet(date, exercise, set.category, set.reps, set.weight, set.comment)
        }

        val exercises = backup.exercises.map { exercise ->
            val name = exercise.name.trim()
            require(name.isNotBlank()) { "Exercise name must not be blank" }
            Exercise(name, exercise.bodyPart).also { it.favorite = exercise.favorite }
        }

        return ParsedImport(sets, exercises)
    }

    private fun utcTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
