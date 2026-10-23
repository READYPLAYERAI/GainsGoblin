package com.readyplayerai.gainsgoblin.data

import com.readyplayerai.gainsgoblin.domain.PersonalRecords
import com.readyplayerai.gainsgoblin.domain.todayDateString
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import java.util.HashMap

/**
 * Immutable snapshot of everything the app derives from persisted workout data.
 *
 * Produced by [WorkoutRepository] from Room flows. [workoutDays] are the
 * PR-flagged copies returned by `applyPrFlags`, so the aggregate/PR flags on each
 * exercise are already populated.
 */
data class AppState(
    /** True once at least one Room emission has been processed. */
    val loaded: Boolean = false,
    /** PR-flagged workout days, in persisted order (never re-sorted on load). */
    val workoutDays: List<WorkoutDay> = emptyList(),
    /** Known exercises, with `favorite` backfilled to a non-null value. */
    val knownExercises: List<Exercise> = emptyList(),
    /** Personal-record maps computed from [workoutDays] and [knownExercises]. */
    val personalRecords: PersonalRecords = emptyPersonalRecords(),
    /** The currently selected `yyyy-MM-dd` date string. */
    val selectedDate: String = todayDateString(),
    /** Non-null when the one-time legacy migration failed and needs recovery. */
    val storageError: String? = null
)

/**
 * Returns a [PersonalRecords] instance with every map empty.
 *
 * Used as the neutral default for [AppState.personalRecords] before the first real
 * calculation has run.
 */
fun emptyPersonalRecords(): PersonalRecords = PersonalRecords(
    HashMap(),
    HashMap(),
    HashMap(),
    HashMap(),
    HashMap(),
    HashMap(),
    HashMap(),
    HashMap(),
    HashMap(),
    HashMap()
)
