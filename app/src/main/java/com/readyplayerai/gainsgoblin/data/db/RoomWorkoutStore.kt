package com.readyplayerai.gainsgoblin.data.db

import androidx.room.withTransaction
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed read/mutation facade for workouts and known exercises.
 *
 * Mirrors the mutation semantics of the legacy [com.readyplayerai.gainsgoblin.data.WorkoutRepository]
 * while treating the database as the single source of truth. Day and exercise
 * aggregates are derived from the raw sets by the domain layer, so no derived
 * rows are persisted here.
 */
@Singleton
class RoomWorkoutStore @Inject constructor(
    private val database: GainsGoblinDatabase,
    private val dao: WorkoutDao
) {
    val sets: Flow<List<WorkoutSet>> = dao.observeSets().map { list -> list.map { it.toModel() } }
    val exercises: Flow<List<Exercise>> = dao.observeExercises().map { list -> list.map { it.toModel() } }

    suspend fun snapshot(): Pair<List<WorkoutSet>, List<Exercise>> =
        dao.getAllSets().map { it.toModel() } to dao.getAllExercises().map { it.toModel() }

    suspend fun addSet(set: WorkoutSet) {
        dao.insertSets(listOf(set.toEntity(createdAt = System.currentTimeMillis())))
    }

    suspend fun updateSet(date: String, setIndex: Int, reps: Double, weight: Double) {
        val sets = dao.getSetsForDate(date)
        if (setIndex in sets.indices) {
            val set = sets[setIndex]
            dao.updateSet(set.copy(reps = reps, weight = weight))
        }
    }

    suspend fun updateSetNote(date: String, setIndex: Int, comment: String?) {
        val sets = dao.getSetsForDate(date)
        if (setIndex in sets.indices) {
            dao.updateSet(sets[setIndex].copy(comment = comment))
        }
    }

    suspend fun removeSet(date: String, setIndex: Int) {
        val sets = dao.getSetsForDate(date)
        if (setIndex in sets.indices) {
            dao.deleteSetById(sets[setIndex].id)
        }
    }

    suspend fun deleteDay(date: String) {
        dao.deleteSetsForDate(date)
    }

    suspend fun updateExerciseComment(date: String, exerciseName: String, comment: String?) {
        dao.updateSetComment(date, exerciseName, comment)
    }

    suspend fun deleteExercise(exerciseName: String) {
        database.withTransaction {
            dao.deleteExerciseEverywhere(exerciseName)
        }
    }

    suspend fun editExercise(oldName: String, newName: String, newBodyPart: String?) {
        database.withTransaction {
            val oldExercise = dao.getExercise(oldName)
            dao.renameExerciseEverywhere(oldName, newName, newBodyPart)
            if (oldExercise != null) {
                dao.deleteExercise(oldName)
                // If newName is already a known exercise, REPLACE overwrites that
                // row with the old exercise's favorite/sortOrder. Acceptable edge case.
                dao.upsertExercises(
                    listOf(
                        ExerciseEntity(
                            name = newName,
                            bodyPart = newBodyPart,
                            favorite = oldExercise.favorite,
                            sortOrder = oldExercise.sortOrder
                        )
                    )
                )
            }
        }
    }

    suspend fun setFavorite(exerciseName: String, favorite: Boolean?) {
        dao.setFavorite(exerciseName, favorite ?: false)
    }

    suspend fun addExercise(name: String, category: String?): Boolean {
        var added = false
        database.withTransaction {
            if (dao.getExercise(name) != null) {
                added = false
            } else {
                val nextOrder = (dao.getAllExercises().maxOfOrNull { it.sortOrder } ?: -1) + 1
                dao.upsertExercises(listOf(ExerciseEntity(name, category, false, nextOrder)))
                added = true
            }
        }
        return added
    }

    suspend fun clearWorkouts() {
        dao.deleteAllSets()
    }

    suspend fun clearAll() {
        database.withTransaction {
            dao.deleteAllSets()
            dao.deleteAllExercises()
        }
    }

    suspend fun insertImportedSets(sets: List<WorkoutSet>, now: Long = System.currentTimeMillis()) {
        if (sets.isEmpty()) {
            return
        }
        val entities = sets.mapIndexed { index, set -> set.toEntity(createdAt = now + index) }
        dao.insertSets(entities)
    }

    suspend fun replaceWorkoutSets(sets: List<WorkoutSet>, now: Long = System.currentTimeMillis()) {
        database.withTransaction {
            dao.deleteAllSets()
            if (sets.isEmpty()) {
                return@withTransaction
            }
            val entities = sets.mapIndexed { index, set -> set.toEntity(createdAt = now + index) }
            dao.insertSets(entities)
        }
    }

    suspend fun upsertImportedExercises(exercises: List<Exercise>) {
        database.withTransaction {
            val existing = dao.getAllExercises().associateBy { it.name }
            var nextOrder = (existing.values.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val entities = exercises.map { exercise ->
                val name = requireNotNull(exercise.Name) { "Exercise Name is null" }
                val current = existing[name]
                if (current != null) {
                    ExerciseEntity(
                        name = name,
                        bodyPart = exercise.BodyPart ?: current.bodyPart,
                        favorite = exercise.favorite ?: current.favorite,
                        sortOrder = current.sortOrder
                    )
                } else {
                    ExerciseEntity(
                        name = name,
                        bodyPart = exercise.BodyPart,
                        favorite = exercise.favorite ?: false,
                        sortOrder = nextOrder++
                    )
                }
            }
            dao.upsertExercises(entities)
        }
    }

    suspend fun allSetsForFingerprint(): List<WorkoutSet> =
        dao.getAllSets().map { it.toModel() }

    suspend fun allExercisesForFingerprint(): List<Exercise> =
        dao.getAllExercises().map { it.toModel() }

    suspend fun meta(key: String): String? = dao.getMeta(key)?.value

    suspend fun putMeta(key: String, value: String) {
        dao.putMeta(MetaEntity(key, value))
    }

    suspend fun applyImport(
        sets: List<WorkoutSet>,
        exercises: List<Exercise>,
        replaceSets: Boolean,
        replaceExercises: Boolean,
        now: Long = System.currentTimeMillis(),
        lastImportBackupName: String? = null
    ) {
        database.withTransaction {
            if (replaceSets) {
                dao.deleteAllSets()
            }
            if (sets.isNotEmpty()) {
                dao.insertSets(sets.mapIndexed { index, set -> set.toEntity(createdAt = now + index) })
            }
            if (replaceExercises) {
                dao.deleteAllExercises()
            }

            val existing = dao.getAllExercises().associateBy { it.name }
            var nextOrder = (existing.values.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val entities = exercises.map { exercise ->
                val name = requireNotNull(exercise.Name) { "Exercise Name is null" }
                val current = existing[name]
                if (current != null) {
                    ExerciseEntity(
                        name = name,
                        bodyPart = exercise.BodyPart ?: current.bodyPart,
                        favorite = exercise.favorite ?: current.favorite,
                        sortOrder = current.sortOrder
                    )
                } else {
                    ExerciseEntity(
                        name = name,
                        bodyPart = exercise.BodyPart,
                        favorite = exercise.favorite ?: false,
                        sortOrder = nextOrder++
                    )
                }
            }
            if (entities.isNotEmpty()) {
                dao.upsertExercises(entities)
            }

            if (lastImportBackupName != null) {
                dao.putMeta(MetaEntity("last_import_backup", lastImportBackupName))
            }
            dao.putMeta(MetaEntity("last_import_at", now.toString()))
        }
    }
}
