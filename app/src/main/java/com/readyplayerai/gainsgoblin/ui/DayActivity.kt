package com.readyplayerai.gainsgoblin.ui

import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast

import com.readyplayerai.gainsgoblin.adapters.DayExerciseAdapter
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.categoryFor as exerciseCategoryFor
import com.google.android.material.floatingactionbutton.FloatingActionButton

import java.util.ArrayList

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DayActivity : AppCompatActivity() {

    var recyclerView: RecyclerView? = null
    var workoutExerciseAdapter: DayExerciseAdapter? = null
    var date_clicked: String? = null

    var fab: FloatingActionButton? = null

    private val viewModel: DayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day)

        // Self Explanatory I guess
        initActivity()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    // Haven't we said that already?
    fun initActivity() {
        fab = findViewById<FloatingActionButton>(R.id.floatingActionButton)

        fab!!.setOnClickListener { view ->
            val intent = Intent(this, ExercisesActivity::class.java)
            startActivity(intent)
        }

        // Recycler View Stuff
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_day)
    }

    private fun render(state: DayUiState) {
        // A null title means the legacy date parse failed: render nothing (parity).
        if (state.title == null) {
            return
        }

        date_clicked = state.date
        supportActionBar!!.setTitle(state.title)

        // Set Recycler View
        workoutExerciseAdapter = DayExerciseAdapter(
            this,
            ArrayList(state.exercises),
            onEditClick = { position ->
                val ex = state.exercises[position]
                val intent = Intent(this, AddExerciseActivity::class.java)
                intent.putExtra("exercise", ex.Exercise)
                startActivity(intent)
            },
            categoryFor = { name -> exerciseCategoryFor(state.knownExercises, name) }
        )
        recyclerView!!.setAdapter(workoutExerciseAdapter)
        recyclerView!!.setLayoutManager(LinearLayoutManager(this))

        // Notify User
        if (state.exercises.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No Logged Exercises", Toast.LENGTH_SHORT).show()
        }
    }

    // When back button is pressed by another app
    override fun onRestart() {
        super.onRestart()

        // Self Explanatory I guess
        lifecycleScope.launch { render(viewModel.freshUiState()) }
    }

    // Menu Methods
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.day_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
