package com.readyplayerai.gainsgoblin.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    val name: String,
    @ColumnInfo(name = "body_part")
    val bodyPart: String?,
    val favorite: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int
)
