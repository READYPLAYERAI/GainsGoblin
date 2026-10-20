package com.readyplayerai.gainsgoblin.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Day screen.
 *
 * A null [title] means the raw intent date failed to parse; in that case the
 * legacy screen rendered nothing (no title, no adapter, no toast), so the
 * Activity skips rendering entirely when [title] is null.
 */
data class DayUiState(
    val loaded: Boolean = false,
    val date: String? = null,
    val title: String? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val knownExercises: List<Exercise> = emptyList()
)

/**
 * Backs [DayActivity].
 *
 * Maps [WorkoutRepository.appState] for the day identified by the `"date"` intent
 * extra (populated into [SavedStateHandle] by Hilt).
 */
@HiltViewModel
class DayViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val date: String? = savedStateHandle.get<String>("date")

    private val _uiState = MutableStateFlow(mapAppState(repository.appState.value))

    val uiState: StateFlow<DayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    /** Fresh, never-stale state straight from the repository (re-runs the state pipeline). */
    suspend fun freshUiState(): DayUiState = mapAppState(repository.currentAppState())

    private fun mapAppState(state: AppState): DayUiState {
        // Legacy parsed inside a single try block: on failure nothing was rendered.
        val title = try {
            val parsed: Date = SimpleDateFormat("yyyy-MM-dd").parse(date ?: "")!!
            SimpleDateFormat("EEEE, MMM dd YYYY").format(parsed)
        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }

        if (title == null) {
            return DayUiState(
                loaded = state.loaded,
                date = date,
                title = null,
                exercises = emptyList(),
                knownExercises = state.knownExercises
            )
        }

        var exercises: List<WorkoutExercise> = emptyList()
        for (day in state.workoutDays) {
            if (day.Date == date) {
                exercises = day.Exercises ?: emptyList()
            }
        }

        return DayUiState(
            loaded = state.loaded,
            date = date,
            title = title,
            exercises = exercises,
            knownExercises = state.knownExercises
        )
    }
}
