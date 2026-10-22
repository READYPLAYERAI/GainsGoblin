package com.readyplayerai.gainsgoblin.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.categoryFor
import com.readyplayerai.gainsgoblin.model.WorkoutSet
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme
import com.readyplayerai.gainsgoblin.ui.compose.addexercise.AddExerciseScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Thin Compose host for [AddExerciseScreen]: collects [AddExerciseViewModel]'s
 * UI state and forwards the screen's add/update/delete actions to it. The whole
 * M3 Expressive layout lives in the composable.
 */
@AndroidEntryPoint
class AddExerciseActivity : AppCompatActivity() {

    private val viewModel: AddExerciseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_exercise)

        findViewById<ComposeView>(R.id.add_exercise_view).setContent {
            GainsGoblinTheme {
                val uiState by viewModel.uiState.collectAsState()
                AddExerciseScreen(
                    uiState = uiState,
                    onAddSet = { weight, reps ->
                        lifecycleScope.launch {
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
                        lifecycleScope.launch { viewModel.updateSet(date, setIndex, reps, weight) }
                    },
                    onDeleteSet = { set ->
                        lifecycleScope.launch { viewModel.deleteSetAt(set) }
                    },
                )
            }
        }
    }
}
