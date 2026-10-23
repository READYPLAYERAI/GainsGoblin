package com.readyplayerai.gainsgoblin.data.backup

import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet

enum class BackupFormat {
    GAINS_GOBLIN_CSV,
    FITNOTES_CSV,
    GAINS_GOBLIN_JSON
}

data class ImportedSet(
    val date: String,
    val exerciseName: String,
    val category: String?,
    val reps: Double,
    val weight: Double,
    val comment: String?
)

fun ImportedSet.toWorkoutSet(): WorkoutSet =
    WorkoutSet(date, exerciseName, category, reps, weight, comment)

data class ParsedImport(
    val sets: List<ImportedSet>,
    val exercises: List<Exercise>,
    val warnings: List<String> = emptyList()
)

data class BackupData(
    val sets: List<WorkoutSet>,
    val exercises: List<Exercise>
)

private val DATE_PATTERN = Regex("""^\d{4}-\d{2}-\d{2}$""")

fun normalizeDate(raw: String): String {
    val trimmed = raw.trim()
    val candidate = if (trimmed.length > 10) trimmed.substring(0, 10) else trimmed
    require(DATE_PATTERN.matches(candidate)) { "Invalid date: '$raw'" }
    return candidate
}

internal fun mergeExercisesForImport(
    sets: List<ImportedSet>,
    importedExercises: List<Exercise>
): List<Exercise> {
    val result = LinkedHashMap<String, Exercise>()
    importedExercises.forEach { exercise ->
        val name = exercise.Name
        if (!name.isNullOrBlank()) {
            result[name] = exercise
        }
    }
    sets.forEach { set ->
        val name = set.exerciseName
        if (!name.isNullOrBlank()) {
            val existing = result[name]
            if (existing == null) {
                result[name] = Exercise(name, set.category).also { it.favorite = null }
            } else if (existing.BodyPart == null) {
                existing.BodyPart = set.category
            }
        }
    }
    return result.values.toList()
}
