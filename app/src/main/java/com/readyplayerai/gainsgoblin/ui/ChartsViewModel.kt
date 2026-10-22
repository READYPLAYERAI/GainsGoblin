package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the Charts screen. */
data class ChartsUiState(
    val loaded: Boolean = false,
    val days: List<WorkoutDay> = emptyList()
)

/**
 * Backs [ChartsActivity].
 *
 * Maps [WorkoutRepository.appState]'s workout days. The Compose screen collects
 * [uiState] directly.
 */
@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    private fun mapAppState(state: AppState): ChartsUiState = ChartsUiState(
        loaded = state.loaded,
        days = state.workoutDays
    )
}