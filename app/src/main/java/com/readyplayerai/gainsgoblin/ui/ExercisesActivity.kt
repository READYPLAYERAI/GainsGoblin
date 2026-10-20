package com.readyplayerai.gainsgoblin.ui

import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.inputmethod.EditorInfo

import com.readyplayerai.gainsgoblin.adapters.ExerciseAdapter
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.exercises_activity_menu, menu)

        // Search Stuff
        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.getActionView() as androidx.appcompat.widget.SearchView

        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                exerciseAdapter!!.getFilter().filter(s)
                return false
            }
        })

        return true
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.add) {
            val intent = Intent(this, CustomExerciseActivity::class.java)
            startActivity(intent)
        } else if (item.itemId == R.id.settings) {
            val intent = Intent(getApplicationContext(), SettingsActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

}