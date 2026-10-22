package com.readyplayerai.gainsgoblin.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme
import com.readyplayerai.gainsgoblin.ui.compose.exercises.ExercisesScreen
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExercisesActivity : AppCompatActivity() {

    private val viewModel: ExercisesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_exercises)

        setupBottomNavBar(BottomNavTab.EXERCISES)

        val activity = this
        findViewById<ComposeView>(R.id.exercises_view).setContent {
            GainsGoblinTheme {
                val state by viewModel.uiState.collectAsState()
                ExercisesScreen(
                    uiState = state,
                    onExerciseClick = { name ->
                        val intent = Intent(activity, AddExerciseActivity::class.java)
                        intent.putExtra("exercise", name)
                        activity.startActivity(intent)
                    },
                    onAddExercise = {
                        startActivity(Intent(activity, CustomExerciseActivity::class.java))
                    },
                    onEditExercise = { oldName, newName, newCategory ->
                        lifecycleScope.launch { viewModel.editExercise(oldName, newName, newCategory) }
                    },
                    onDeleteExercise = { name ->
                        lifecycleScope.launch { viewModel.deleteExercise(name) }
                    },
                )
            }
        }
    }
}
