package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.ExercisePersonalStats
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.domain.categoryFor
import com.readyplayerai.gainsgoblin.domain.isFavorite
import com.readyplayerai.gainsgoblin.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the Personal Records screen. */
data class PersonalRecordsUiState(
    val loaded: Boolean = false,
    val stats: List<ExercisePersonalStats> = emptyList(),
    val knownExercises: List<Exercise> = emptyList()
)

/**
 * Backs [PersonalRecordsActivity].
 *
 * Ports the legacy `calculatePersonalRecords()` companion into [mapAppState]. The
 * Activity renders on create and restart only (legacy parity), so it does not
 * collect this flow for re-rendering mid-session.
 */
@HiltViewModel
class PersonalRecordsViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<PersonalRecordsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    /** Fresh, never-stale state straight from the repository. */
    suspend fun freshUiState(): PersonalRecordsUiState = mapAppState(repository.currentAppState())

    /** Toggles the favorite flag of [exerciseName] using the current known list. */
    suspend fun setFavorite(exerciseName: String) {
        val favorite = isFavorite(repository.appState.value.knownExercises, exerciseName)
        repository.setFavorite(exerciseName, favorite != true)
    }

    private fun mapAppState(state: AppState): PersonalRecordsUiState {
        val personalRecords = state.personalRecords

        val stats = ArrayList<ExercisePersonalStats>()

        // Legacy iterate order: the PR maps are insertion-ordered by known exercise.
        for (entry in personalRecords.volumePRs) {
            val currentExerciseName = entry.key

            // Legacy "not even performed" skip.
            if (personalRecords.volumePRs[currentExerciseName] == 0.0) {
                continue
            }

            val exercisePersonalStats = ExercisePersonalStats()
            exercisePersonalStats.exerciseName = currentExerciseName
            exercisePersonalStats.exerciseCategory = categoryFor(state.knownExercises, currentExerciseName)
            exercisePersonalStats.maxVolume = personalRecords.volumePRs[currentExerciseName]

            val setVolume = personalRecords.setVolumePRs[currentExerciseName]
            exercisePersonalStats.maxSetVolume = setVolume!!.first * setVolume.second
            exercisePersonalStats.maxSetVolumeReps = setVolume.first // First = Reps
            exercisePersonalStats.MaxSetVolumeWeight = setVolume.second // Second = Weight

            exercisePersonalStats.maxReps = personalRecords.maxRepsPRs[currentExerciseName]
            exercisePersonalStats.maxWeight = personalRecords.maxWeightPRs[currentExerciseName]
            exercisePersonalStats.actual1RM = personalRecords.actualOneRepMaxPRs[currentExerciseName]
            exercisePersonalStats.estimated1RM = personalRecords.estimatedOneRMPRs[currentExerciseName]
            exercisePersonalStats.favorite = isFavorite(state.knownExercises, currentExerciseName)

            exercisePersonalStats.maxWeightSet = personalRecords.maxWeightSetPRs[currentExerciseName]
            exercisePersonalStats.maxRepsSet = personalRecords.maxRepsSetPRs[currentExerciseName]
            exercisePersonalStats.maxVolumeSet = personalRecords.maxVolumeSetPRs[currentExerciseName]

            stats.add(exercisePersonalStats)
        }

        // Show favorites first (stable; preserves map order within each group).
        Collections.sort(stats, Comparator<ExercisePersonalStats> { a, b ->
            java.lang.Boolean.compare(b.favorite!!, a.favorite!!)
        })

        return PersonalRecordsUiState(
            loaded = state.loaded,
            stats = stats,
            knownExercises = state.knownExercises
        )
    }
}