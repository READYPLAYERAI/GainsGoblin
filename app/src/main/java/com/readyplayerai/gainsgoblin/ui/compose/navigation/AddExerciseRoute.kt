package com.readyplayerai.gainsgoblin.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.readyplayerai.gainsgoblin.domain.categoryFor
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.AddExerciseViewModel
import com.readyplayerai.gainsgoblin.ui.compose.addexercise.AddExerciseScreen
import kotlinx.coroutines.launch

/**
 * AddExercise destination. The exercise name arrives through the `exercise`
 * nav argument, which Navigation places in [AddExerciseViewModel]'s
 * SavedStateHandle.
 */
@Composable
fun AddExerciseRoute() {
    val viewModel: AddExerciseViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    AddExerciseScreen(
        uiState = uiState,
        onAddSet = { weight, reps ->
            scope.launch {
                viewModel.addSet(
                    WorkoutSet(
                        uiState.selectedDate,
                        uiState.exerciseName,
                        categoryFor(uiState.knownExercises, uiState.exerciseName),
                        reps,
                        weight,
                    ),
                )
            }
        },
        onUpdateSet = { date, setIndex, weight, reps ->
            scope.launch { viewModel.updateSet(date, setIndex, reps, weight) }
        },
        onDeleteSet = { set ->
            scope.launch { viewModel.deleteSetAt(set) }
        },
    )
}
