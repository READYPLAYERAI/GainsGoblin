package com.readyplayerai.gainsgoblin.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    indices = [
        Index(value = ["date"]),
        Index(value = ["exercise_name"])
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,
    val category: String?,
    val reps: Double,
    val weight: Double,
    val comment: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
