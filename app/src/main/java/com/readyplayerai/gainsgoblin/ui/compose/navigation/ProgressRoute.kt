package com.readyplayerai.gainsgoblin.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.readyplayerai.gainsgoblin.ui.ProgressViewModel
import com.readyplayerai.gainsgoblin.ui.compose.progress.ProgressScreen

/** Progress tab: collects [ProgressViewModel] and renders [ProgressScreen]. */
@Composable
fun ProgressRoute(onOpenExercise: (String) -> Unit) {
    val viewModel: ProgressViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    ProgressScreen(uiState = state, onOpenExercise = onOpenExercise)
}
