package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.domain.todayDateString
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the per-exercise progress detail screen. */
data class ExerciseProgressUiState(
    val loaded: Boolean = false,
    val exerciseName: String = "",
    val selectedDate: String = "",
    val days: List<WorkoutDay> = emptyList(),
    val knownExercises: List<Exercise> = emptyList()
)

/**
 * Backs the per-exercise progress detail destination: the exercise name comes
 * from the `"exercise"` nav argument via [SavedStateHandle], and the workout days
 * and known exercises are mapped from [WorkoutRepository.appState].
 */
@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseName: String = savedStateHandle.get<String>("exercise").orEmpty()

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<ExerciseProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    suspend fun setFavorite(favorite: Boolean) {
        repository.setFavorite(exerciseName, favorite)
    }

    /**
     * Renames the current exercise and optionally changes its body part. Blank names
     * are rejected; returns true when a rename/update was issued.
     */
    suspend fun rename(newName: String, newCategory: String?): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return false
        repository.editExercise(exerciseName, trimmed, newCategory)
        return true
    }

    /** Deletes the current exercise; logged sets keep their history. */
    suspend fun deleteExercise() {
        repository.deleteExercise(exerciseName)
    }

    suspend fun addSet(weight: Double, reps: Double) {
        val state = _uiState.value
        val date = state.selectedDate.ifBlank { todayDateString() }
        val category = state.knownExercises.firstOrNull { it.Name == exerciseName }?.BodyPart
        repository.addSet(WorkoutSet(date, exerciseName, category, reps, weight))
    }

    suspend fun updateSet(
        date: String,
        exerciseSetIndex: Int,
        weight: Double,
        reps: Double,
    ) {
        val day = repository.currentAppState().workoutDays.firstOrNull { it.Date == date } ?: return
        val rawSets = day.Sets ?: return
        var seen = 0
        var rawIndex = -1
        for (index in rawSets.indices) {
            if (rawSets[index].exerciseName == exerciseName) {
                if (seen == exerciseSetIndex) {
                    rawIndex = index
                    break
                }
                seen++
            }
        }
        if (rawIndex < 0) return
        repository.updateSet(date, rawIndex, reps, weight)
    }

    private fun mapAppState(state: AppState): ExerciseProgressUiState = ExerciseProgressUiState(
        loaded = state.loaded,
        exerciseName = exerciseName,
        selectedDate = state.selectedDate,
        days = state.workoutDays,
        knownExercises = state.knownExercises
    )
}
