package com.readyplayerai.gainsgoblin.ui

import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo

import com.readyplayerai.gainsgoblin.adapters.ExerciseStatsAdapter
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.categoryFor

import java.util.ArrayList

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonalRecordsActivity : AppCompatActivity() {

    var recyclerView: RecyclerView? = null
    var exerciseStatsAdapter: ExerciseStatsAdapter? = null

    private val viewModel: PersonalRecordsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_records)

        lifecycleScope.launch { render(viewModel.freshUiState()) }
    }

    override fun onRestart() {
        super.onRestart()
        lifecycleScope.launch { render(viewModel.freshUiState()) }
    }

    private fun render(state: PersonalRecordsUiState) {
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val layoutManager = LinearLayoutManager(getApplicationContext())
        recyclerView!!.setLayoutManager(layoutManager)
        recyclerView!!.setItemAnimator(DefaultItemAnimator())

        exerciseStatsAdapter = ExerciseStatsAdapter(
            this,
            ArrayList(state.stats),
            onFavoriteToggle = { position ->
                lifecycleScope.launch {
                    val name = exerciseStatsAdapter?.nameAt(position) ?: return@launch
                    viewModel.setFavorite(name)
                    render(viewModel.freshUiState())
                }
            },
            categoryFor = { name -> categoryFor(state.knownExercises, name) }
        )
        recyclerView!!.setAdapter(exerciseStatsAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.personal_records_activity_menu, menu)

        // Search Stuff
        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.getActionView() as androidx.appcompat.widget.SearchView

        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                exerciseStatsAdapter!!.getFilter().filter(s)
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}