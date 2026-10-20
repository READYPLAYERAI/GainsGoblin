package com.readyplayerai.gainsgoblin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.gainsgoblin.data.AppState
import com.readyplayerai.gainsgoblin.data.ExportResult
import com.readyplayerai.gainsgoblin.data.ImportCsvResult
import com.readyplayerai.gainsgoblin.data.WorkoutRepository
import com.readyplayerai.gainsgoblin.domain.buildInfiniteDays
import com.readyplayerai.gainsgoblin.domain.todayDateString
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Home pager screen.
 *
 * [infiniteDays] is the fixed ten-year window of empty days (the legacy
 * `infiniteWorkoutDays`); [realDays] are the repository's PR-flagged days.
 */
data class HomeUiState(
    val loaded: Boolean = false,
    val infiniteDays: List<WorkoutDay> = emptyList(),
    val realDays: List<WorkoutDay> = emptyList(),
    val knownExercises: List<Exercise> = emptyList(),
    val selectedDate: String = "",
    val todayPosition: Int = -1
)

/**
 * Backs [MainActivity]'s pager.
 *
 * Owns the legacy "infinite" ten-year day window (built exactly once, like the
 * legacy `infiniteWorkoutDays` cache) and maps [WorkoutRepository.appState] into a
 * [HomeUiState].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    // Built once and kept stable across every emission, like the legacy cache.
    private val infiniteDays: List<WorkoutDay> = buildInfiniteDaysWindow()
    private val todayPosition: Int = todayPositionIn(infiniteDays)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            infiniteDays = infiniteDays,
            todayPosition = todayPosition
        )
    )

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appState.collect { _uiState.value = mapAppState(it) }
        }
    }

    /** Fresh, never-stale state straight from the repository (re-runs the state pipeline). */
    suspend fun freshUiState(): HomeUiState = mapAppState(repository.currentAppState())

    private fun mapAppState(state: AppState): HomeUiState = HomeUiState(
        loaded = state.loaded,
        infiniteDays = infiniteDays,
        realDays = state.workoutDays,
        knownExercises = state.knownExercises,
        selectedDate = state.selectedDate,
        todayPosition = todayPosition
    )

    /** Delegates to [WorkoutRepository.importCsv]. */
    suspend fun importCsv(uri: Uri): ImportCsvResult = repository.importCsv(uri)

    /** Delegates to [WorkoutRepository.exportCsv]. */
    suspend fun exportCsv(): ExportResult = repository.exportCsv()

    /** Persists the selected date through the repository. */
    fun selectDate(date: String) {
        viewModelScope.launch { repository.selectDate(date) }
    }

    /**
     * Exact port of the legacy range computation in `MainActivity.initViewPager()`:
     * one Calendar stepped back five years then forward ten.
     */
    private fun buildInfiniteDaysWindow(): List<WorkoutDay> {
        val c = Calendar.getInstance()
        c.time = Date()
        c.add(Calendar.YEAR, -5)
        val startDate = c.time
        c.add(Calendar.YEAR, +10)
        val endDate = c.time
        return buildInfiniteDays(startDate, endDate)
    }

    /** Port of legacy `getTodayPosition()`: index of today, or -1 when missing. */
    private fun todayPositionIn(days: List<WorkoutDay>): Int {
        val today = todayDateString()
        for (i in days.indices) {
            if (days[i].Date == today) {
                return i
            }
        }
        return -1
    }
}
