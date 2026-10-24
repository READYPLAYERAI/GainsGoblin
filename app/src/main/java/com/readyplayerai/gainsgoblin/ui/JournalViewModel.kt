package com.readyplayerai.gainsgoblin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.domain.buildInfiniteDays
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the journal feed.
 *
 * [infiniteDays] is the fixed descending day window of empty days (the legacy
 * `infiniteWorkoutDays`), newest-first: `infiniteDays[0]` is today and
 * `infiniteDays.last()` is today minus five years. [realDays] are the
 * repository's PR-flagged days.
 */
data class JournalUiState(
    val loaded: Boolean = false,
    val infiniteDays: List<WorkoutDay> = emptyList(),
    val realDays: List<WorkoutDay> = emptyList(),
    val knownExercises: List<Exercise> = emptyList(),
    val selectedDate: String = "",
    val storageError: String? = null,
    val hasLegacyRecoveryData: Boolean = false
)

/**
 * Backs the Journal tab's journal feed.
 *
 * Owns the legacy "infinite" day window (built exactly once, like the legacy
 * `infiniteWorkoutDays` cache) as a descending, newest-first list
 * `[today .. today-5y]`, and maps [WorkoutRepository.appState] into a
 * [JournalUiState].
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    // Built once and kept stable across every emission, like the legacy cache.
    private val infiniteDays: List<WorkoutDay> = buildInfiniteDaysWindow()

    private val _uiState = MutableStateFlow(
        JournalUiState(
            infiniteDays = infiniteDays
        )
    )

    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    /** Fresh, never-stale state straight from the repository (re-runs the state pipeline). */
    suspend fun freshUiState(): JournalUiState = mapAppState(repository.currentAppState())

    private fun mapAppState(state: AppState): JournalUiState = JournalUiState(
        loaded = state.loaded,
        infiniteDays = infiniteDays,
        realDays = state.workoutDays,
        knownExercises = state.knownExercises,
        selectedDate = state.selectedDate,
        storageError = state.storageError,
        hasLegacyRecoveryData = repository.hasLegacyRecoveryData()
    )

    /** Persists the selected date through the repository. */
    fun selectDate(date: String) {
        viewModelScope.launch { repository.selectDate(date) }
    }

    /** Retries the one-time legacy migration; true on success. */
    suspend fun retryMigration(): Boolean = repository.retryMigration()

    /** Writes the retained raw legacy payloads to [uri]; false when there is nothing or the write fails. */
    suspend fun exportLegacyRaw(uri: Uri): Boolean = repository.exportLegacyRaw(uri)

    suspend fun addJournalSet(
        date: String,
        exerciseName: String,
        category: String?,
        reps: Double,
        weight: Double,
        comment: String?,
    ) {
        repository.addSet(WorkoutSet(date, exerciseName, category, reps, weight, comment))
    }

    suspend fun updateJournalSet(
        date: String,
        exerciseName: String,
        exerciseSetIndex: Int,
        weight: Double,
        reps: Double,
    ) {
        val rawIndex = resolveRawSetIndex(date, exerciseName, exerciseSetIndex) ?: return
        repository.updateSet(date, rawIndex, reps, weight)
    }

    suspend fun deleteJournalSet(
        date: String,
        exerciseName: String,
        exerciseSetIndex: Int,
    ) {
        val rawIndex = resolveRawSetIndex(date, exerciseName, exerciseSetIndex) ?: return
        repository.removeSet(date, rawIndex)
    }

    suspend fun updateJournalSetNote(
        date: String,
        exerciseName: String,
        exerciseSetIndex: Int,
        comment: String?,
    ) {
        val rawIndex = resolveRawSetIndex(date, exerciseName, exerciseSetIndex) ?: return
        repository.updateSetNote(date, rawIndex, comment)
    }

    suspend fun updateJournalExerciseNote(
        date: String,
        exerciseName: String,
        comment: String?,
    ) {
        val day = repository.currentAppState().workoutDays.firstOrNull { it.Date == date } ?: return
        val exercise = day.Exercises?.firstOrNull { it.Exercise == exerciseName } ?: return
        val lastIndex = (exercise.Sets?.lastIndex ?: -1)
        if (lastIndex < 0) return
        val rawIndex = rawSetIndex(day, exerciseName, lastIndex) ?: return
        repository.updateSetNote(date, rawIndex, comment)
    }

    suspend fun addExercise(name: String, category: String?): Boolean =
        repository.addExercise(name, category)

    private suspend fun resolveRawSetIndex(
        date: String,
        exerciseName: String,
        exerciseSetIndex: Int,
    ): Int? {
        val day = repository.currentAppState().workoutDays.firstOrNull { it.Date == date } ?: return null
        return rawSetIndex(day, exerciseName, exerciseSetIndex)
    }

    private fun rawSetIndex(
        day: WorkoutDay,
        exerciseName: String,
        exerciseSetIndex: Int,
    ): Int? {
        if (exerciseSetIndex < 0) return null
        val raw = day.Sets ?: return null
        var seen = 0
        for (i in raw.indices) {
            if (raw[i].exerciseName == exerciseName) {
                if (seen == exerciseSetIndex) return i
                seen++
            }
        }
        return null
    }

    /**
     * Builds the descending day window `[today .. today - 5 years]`.
     *
     * [buildInfiniteDays] is end-exclusive and ascending, so the range is
     * constructed as `today - 5 years` (inclusive) through `today + 1 day`
     * (exclusive) and then reversed; `[0]` is today and `last()` is
     * today minus five years. No future days are included.
     */
    private fun buildInfiniteDaysWindow(): List<WorkoutDay> {
        val now = Date()
        val start = Calendar.getInstance().apply {
            time = now
            add(Calendar.YEAR, -5)
        }
        val end = Calendar.getInstance().apply {
            time = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return buildInfiniteDays(start.time, end.time).reversed()
    }
}
