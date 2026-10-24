package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Add Exercise data-entry screen.
 *
 * [exerciseName] is the `"exercise"` intent extra; all data comes from
 * [WorkoutRepository.appState] so this screen no longer reads the legacy
 * storage singleton.
 */
data class AddExerciseUiState(
    val loaded: Boolean = false,
    val exerciseName: String? = null,
    val selectedDate: String = "",
    val days: List<WorkoutDay> = emptyList(),
    val knownExercises: List<Exercise> = emptyList()
)

/**
 * Backs [AddExerciseRoute].
 *
 * Maps [WorkoutRepository.appState] for the exercise named by the `"exercise"`
 * intent extra (populated into [SavedStateHandle] by Hilt), and forwards every
 * mutation to the repository.
 */
@HiltViewModel
class AddExerciseViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseName: String? = savedStateHandle.get<String>("exercise")

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<AddExerciseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    /** Fresh, never-stale state straight from the repository (re-runs the state pipeline). */
    suspend fun freshUiState(): AddExerciseUiState = mapAppState(repository.currentAppState())

    suspend fun addSet(set: WorkoutSet) {
        repository.addSet(set)
    }

    suspend fun updateSet(date: String, setIndex: Int, reps: Double, weight: Double) {
        repository.updateSet(date, setIndex, reps, weight)
    }

    suspend fun updateExerciseComment(exerciseName: String, comment: String?) {
        repository.updateExerciseComment(_uiState.value.selectedDate, exerciseName, comment)
    }

    /**
     * Port of the legacy `deleteSetLogic`.
     *
     * Finds the day containing [set] (identity first, then all-field equality since
     * the set came from a different repository snapshot), removes it, then sweeps
     * any days the removal left with zero sets (legacy behaviour).
     */
    suspend fun deleteSetAt(set: WorkoutSet) {
        val state = repository.currentAppState()

        var dayDate: String? = null
        var setIndex = -1
        for (day in state.workoutDays) {
            val sets = day.Sets ?: continue
            val index = sets.indexOfFirst { it === set || sameSet(it, set) }
            if (index >= 0) {
                dayDate = day.Date
                setIndex = index
                break
            }
        }

        if (dayDate == null || setIndex < 0) {
            return
        }

        repository.removeSet(dayDate, setIndex)

        // Cleanup potential days with 0 sets
        val after = repository.currentAppState()
        for (day in after.workoutDays) {
            if ((day.Sets ?: emptyList()).isEmpty()) {
                repository.deleteDay(day.Date!!)
            }
        }
    }

    private fun mapAppState(state: AppState): AddExerciseUiState = AddExerciseUiState(
        loaded = state.loaded,
        exerciseName = exerciseName,
        selectedDate = state.selectedDate,
        days = state.workoutDays,
        knownExercises = state.knownExercises
    )

    /** All-field equality used when snapshot object identity cannot match. */
    private fun sameSet(a: WorkoutSet, b: WorkoutSet): Boolean =
        a.date == b.date &&
            a.exerciseName == b.exerciseName &&
            a.category == b.category &&
            a.reps == b.reps &&
            a.weight == b.weight &&
            a.comment == b.comment &&
            a.id == b.id &&
            a.user_id == b.user_id
}
