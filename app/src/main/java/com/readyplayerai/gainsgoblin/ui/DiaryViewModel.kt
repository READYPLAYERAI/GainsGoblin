package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Diary screen.
 *
 * [initialPosition] mirrors the legacy `getDayPosition(date)` lookup: -1 when the
 * `"date"` intent extra is absent or no day matches. Legacy never scrolled to it,
 * but the value still drives the `System.out.println` branch.
 */
data class DiaryUiState(
    val loaded: Boolean = false,
    val days: List<WorkoutDay> = emptyList(),
    val knownExercises: List<Exercise> = emptyList(),
    val initialPosition: Int = -1 // position of the "date" intent extra's day, -1 when absent/missing
)

/**
 * Backs [DiaryActivity].
 *
 * Maps [WorkoutRepository.appState] into [DiaryUiState]. Days keep the repository's
 * persisted order (already date-ascending in practice).
 */
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val date: String? = savedStateHandle.get<String>("date")

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    /** Fresh, never-stale state straight from the repository (re-runs the state pipeline). */
    suspend fun freshUiState(): DiaryUiState = mapAppState(repository.currentAppState())

    /** Persists the selected date through the repository. */
    fun selectDate(date: String) {
        viewModelScope.launch { repository.selectDate(date) }
    }

    private fun mapAppState(state: AppState): DiaryUiState = DiaryUiState(
        loaded = state.loaded,
        days = state.workoutDays,
        knownExercises = state.knownExercises,
        initialPosition = initialPositionIn(state)
    )

    /**
     * Index scan identical to the legacy `getDayPosition`.
     *
     * Computed on every emission (rather than once at init) so a not-yet-loaded
     * repository does not bake in a wrong -1.
     */
    private fun initialPositionIn(state: AppState): Int {
        if (date == null) {
            return -1
        }
        val days = state.workoutDays
        for (i in days.indices) {
            if (days[i].Date == date) {
                return i
            }
        }
        return -1
    }
}
