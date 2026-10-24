package com.readyplayerai.gainsgoblin.ui

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

/** UI state for the Progress screen. */
data class ProgressUiState(
    val loaded: Boolean = false,
    val days: List<WorkoutDay> = emptyList(),
    val knownExercises: List<Exercise> = emptyList()
)

/**
 * Backs the Progress tab.
 *
 * Maps [WorkoutRepository.appState]'s workout days and known exercises. The
 * Compose screen collects [uiState] directly.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    private fun mapAppState(state: AppState): ProgressUiState = ProgressUiState(
        loaded = state.loaded,
        days = state.workoutDays,
        knownExercises = state.knownExercises
    )
}
