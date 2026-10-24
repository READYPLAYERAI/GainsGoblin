package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.domain.ExerciseStats
import com.readyplayerai.gainsgoblin.domain.exerciseStats
import com.readyplayerai.gainsgoblin.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Exercises list screen.
 *
 * [date] carries the legacy (unused) `"date"` intent extra purely for parity.
 */
data class ExercisesUiState(
    val loaded: Boolean = false,
    val knownExercises: List<Exercise> = emptyList(),
    val date: String? = null,
    val stats: Map<String, ExerciseStats> = emptyMap()
)

/**
 * Backs the Exercises tab.
 *
 * Maps [WorkoutRepository.appState]'s known exercises. The Compose screen collects
 * [uiState] continuously for re-rendering mid-session.
 */
@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val date: String? = savedStateHandle.get<String>("date")

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    private fun mapAppState(state: AppState): ExercisesUiState = ExercisesUiState(
        loaded = state.loaded,
        knownExercises = state.knownExercises,
        date = date,
        stats = exerciseStats(state.workoutDays)
    )

    suspend fun editExercise(oldName: String, newName: String, newBodyPart: String?) {
        repository.editExercise(oldName, newName, newBodyPart)
    }

    suspend fun deleteExercise(name: String) {
        repository.deleteExercise(name)
    }

    suspend fun addExercise(name: String, category: String?): Boolean =
        repository.addExercise(name, category)

    suspend fun setFavorite(name: String, favorite: Boolean) {
        repository.setFavorite(name, favorite)
    }

    suspend fun restoreExercise(exercise: Exercise) {
        val name = exercise.Name.orEmpty()
        if (name.isBlank()) return
        if (repository.addExercise(name, exercise.BodyPart)) {
            if (exercise.favorite == true) repository.setFavorite(name, true)
        }
    }
}
