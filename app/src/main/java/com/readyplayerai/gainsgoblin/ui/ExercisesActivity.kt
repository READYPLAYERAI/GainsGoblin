package com.readyplayerai.gainsgoblin.ui

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.os.Bundle

import com.readyplayerai.gainsgoblin.adapters.ExerciseAdapter
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.ExercisesTopBar
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar
import com.readyplayerai.gainsgoblin.ui.compose.setupTopBar

import java.util.ArrayList

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExercisesActivity : AppCompatActivity() {

    var recyclerView: RecyclerView? = null
    var exerciseAdapter: ExerciseAdapter? = null
    var date_clicked: String? = null

    private val viewModel: ExercisesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercises)

        setupTopBar {
            ExercisesTopBar(
                title = "Exercises",
                onQueryChange = { query -> exerciseAdapter?.getFilter()?.filter(query) },
                onAddClick = { startActivity(Intent(this@ExercisesActivity, CustomExerciseActivity::class.java)) },
            )
        }

        setupBottomNavBar(BottomNavTab.EXERCISES)

        onCreateStuff()
    }

    override fun onRestart() {
        super.onRestart()
        onCreateStuff()
    }

    fun onCreateStuff() {
        // Legacy loaded synchronously; render fresh state asynchronously instead.
        lifecycleScope.launch { render(viewModel.freshUiState()) }
    }

    private fun render(state: ExercisesUiState) {
        // Intent from DayActivity (unused legacy field, kept for parity)
        date_clicked = state.date

        // Find Recycler View Object
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_exercises)
        exerciseAdapter = ExerciseAdapter(
            this,
            ArrayList(state.knownExercises),
            onRowClick = { position ->
                // Resolve from the adapter so the correct (possibly filtered) row is used.
                val name = exerciseAdapter?.exerciseNameAt(position)
                val intent = Intent(this, AddExerciseActivity::class.java)
                intent.putExtra("exercise", name)
                startActivity(intent)
            },
            onEdit = { _, oldName, newName, newCategory ->
                lifecycleScope.launch { viewModel.editExercise(oldName, newName, newCategory) }
            },
            onDelete = { _, name ->
                lifecycleScope.launch { viewModel.deleteExercise(name) }
            }
        )
        recyclerView!!.setAdapter(exerciseAdapter)
        recyclerView!!.setLayoutManager(LinearLayoutManager(this))
    }

}