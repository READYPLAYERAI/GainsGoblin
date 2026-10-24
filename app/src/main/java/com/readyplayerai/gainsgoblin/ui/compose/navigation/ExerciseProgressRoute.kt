package com.readyplayerai.gainsgoblin.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.readyplayerai.gainsgoblin.ui.ExerciseProgressViewModel
import com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress.ExerciseProgressScreen
import kotlinx.coroutines.launch

/**
 * Per-exercise progress detail: collects [ExerciseProgressViewModel], whose
 * exercise name arrives from the nav argument via `SavedStateHandle["exercise"]`,
 * and renders [ExerciseProgressScreen]. Renames and deletes update the repository
 * through the ViewModel and report back through [onOpenExercise] / [onDeleted].
 */
@Composable
fun ExerciseProgressRoute(
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel: ExerciseProgressViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ExerciseProgressScreen(
        uiState = state,
        onBack = onBack,
        onAddSet = { weight, reps ->
            scope.launch { viewModel.addSet(weight, reps) }
        },
        onUpdateSet = { date, index, weight, reps ->
            scope.launch { viewModel.updateSet(date, index, weight, reps) }
        },
        onToggleFavorite = { favorite -> scope.launch { viewModel.setFavorite(favorite) } },
        onRename = { _, newName, newCategory ->
            scope.launch {
                if (viewModel.rename(newName, newCategory)) {
                    onOpenExercise(newName.trim())
                }
            }
        },
        onDelete = {
            scope.launch {
                viewModel.deleteExercise()
                onDeleted()
            }
        },
    )
}
