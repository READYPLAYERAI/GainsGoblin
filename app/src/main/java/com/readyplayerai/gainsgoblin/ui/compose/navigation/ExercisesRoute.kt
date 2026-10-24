package com.readyplayerai.gainsgoblin.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.readyplayerai.gainsgoblin.ui.ExercisesViewModel
import com.readyplayerai.gainsgoblin.ui.compose.exercises.ExercisesScreen
import kotlinx.coroutines.launch

/** Exercises tab: collects [ExercisesViewModel] and renders [ExercisesScreen]. */
@Composable
fun ExercisesRoute(onExerciseClick: (String) -> Unit) {
    val viewModel: ExercisesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ExercisesScreen(
        uiState = state,
        onExerciseClick = onExerciseClick,
        onLogSet = { name, weight, reps ->
            scope.launch { viewModel.addSet(name, weight, reps) }
        },
        onAddExercise = { name, category, onResult ->
            scope.launch { onResult(viewModel.addExercise(name, category)) }
        },
        onToggleFavorite = { name, favorite ->
            scope.launch { viewModel.setFavorite(name, favorite) }
        },
        onEditExercise = { oldName, newName, newCategory ->
            scope.launch { viewModel.editExercise(oldName, newName, newCategory) }
        },
        onDeleteExercise = { exercise ->
            scope.launch { viewModel.deleteExercise(exercise.Name.orEmpty()) }
        },
        onRestoreExercise = { exercise ->
            scope.launch { viewModel.restoreExercise(exercise) }
        },
    )
}
