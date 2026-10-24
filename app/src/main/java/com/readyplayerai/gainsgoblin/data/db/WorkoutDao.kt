package com.readyplayerai.gainsgoblin.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sets ORDER BY id ASC")
    fun observeSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM exercises ORDER BY sort_order ASC, name ASC")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM workout_sets ORDER BY id ASC")
    suspend fun getAllSets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM exercises ORDER BY sort_order ASC, name ASC")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM workout_sets")
    suspend fun countSets(): Int

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSetById(id: Long)

    @Query("DELETE FROM workout_sets WHERE date = :date")
    suspend fun deleteSetsForDate(date: String)

    @Query("DELETE FROM workout_sets WHERE exercise_name = :name")
    suspend fun deleteSetsForExercise(name: String)

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAllSets()

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    @Query("UPDATE workout_sets SET exercise_name = :newName, category = :category WHERE exercise_name = :oldName")
    suspend fun renameExerciseEverywhere(oldName: String, newName: String, category: String?)

    @Query("UPDATE workout_sets SET comment = :comment WHERE date = :date AND exercise_name = :exerciseName")
    suspend fun updateSetComment(date: String, exerciseName: String, comment: String?)

    @Query("SELECT * FROM workout_sets WHERE date = :date ORDER BY id ASC")
    suspend fun getSetsForDate(date: String): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE name = :name")
    suspend fun deleteExercise(name: String)

    @Query("UPDATE exercises SET favorite = :favorite WHERE name = :name")
    suspend fun setFavorite(name: String, favorite: Boolean)

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getExercise(name: String): ExerciseEntity?

    @Query("SELECT * FROM meta WHERE `key` = :key LIMIT 1")
    suspend fun getMeta(key: String): MetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: MetaEntity)

    @Transaction
    suspend fun deleteExerciseEverywhere(name: String) {
        deleteSetsForExercise(name)
        deleteExercise(name)
    }
}
