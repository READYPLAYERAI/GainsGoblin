package com.readyplayerai.gainsgoblin.ui

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.inputmethod.EditorInfo

import com.readyplayerai.gainsgoblin.ExercisePersonalStats
import com.readyplayerai.gainsgoblin.adapters.ExerciseStatsAdapter
import com.readyplayerai.gainsgoblin.R

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

class PersonalRecordsActivity : AppCompatActivity() {

    companion object {
        var exerciseStats = ArrayList<ExercisePersonalStats>()
        var recyclerView: RecyclerView? = null
        var exerciseStatsAdapter: ExerciseStatsAdapter? = null

        @JvmStatic
        fun calculatePersonalRecords() {
            // Calculate PRs before parsing Hashmaps
            MainActivity.dataStorage.calculatePersonalRecords()

            PersonalRecordsActivity.exerciseStats.clear()

            for (entry in MainActivity.dataStorage.volumePRs) {
                val currentExerciseName = entry.key
                val exerciseCategory = MainActivity.dataStorage.getExerciseCategory(currentExerciseName)

                val exercisePersonalStats = ExercisePersonalStats()
                exercisePersonalStats.exerciseName = currentExerciseName
                exercisePersonalStats.exerciseCategory = exerciseCategory
                exercisePersonalStats.maxVolume = MainActivity.dataStorage.volumePRs[currentExerciseName]
                exercisePersonalStats.maxSetVolume = MainActivity.dataStorage.setVolumePRs[currentExerciseName]!!.first * MainActivity.dataStorage.setVolumePRs[currentExerciseName]!!.second
                exercisePersonalStats.maxSetVolumeReps = MainActivity.dataStorage.setVolumePRs[currentExerciseName]!!.first // First = Reps
                exercisePersonalStats.MaxSetVolumeWeight = MainActivity.dataStorage.setVolumePRs[currentExerciseName]!!.second // Second = Weight
                exercisePersonalStats.maxReps = MainActivity.dataStorage.maxRepsPRs[currentExerciseName]
                exercisePersonalStats.maxWeight = MainActivity.dataStorage.maxWeightPRs[currentExerciseName]
                exercisePersonalStats.actual1RM = MainActivity.dataStorage.actualOneRepMaxPRs[currentExerciseName]
                exercisePersonalStats.estimated1RM = MainActivity.dataStorage.estimatedOneRMPRs[currentExerciseName]
                exercisePersonalStats.favorite = MainActivity.dataStorage.isExerciseFavorite(currentExerciseName)

                exercisePersonalStats.maxWeightSet = MainActivity.dataStorage.maxWeightSetPRs[currentExerciseName]
                exercisePersonalStats.maxRepsSet = MainActivity.dataStorage.maxRepsSetPRs[currentExerciseName]
                exercisePersonalStats.maxVolumeSet = MainActivity.dataStorage.maxVolumeSetPRs[currentExerciseName]

                if (MainActivity.dataStorage.volumePRs[currentExerciseName] == 0.0) {
                    // Skip this exercise, it was not even performed
                } else {
                    PersonalRecordsActivity.exerciseStats.add(exercisePersonalStats)
                }

                // Show favorites first
                Collections.sort(PersonalRecordsActivity.exerciseStats, Comparator<ExercisePersonalStats> { exerciseStats1, exerciseStats2 ->
                    java.lang.Boolean.compare(exerciseStats2!!.favorite!!, exerciseStats1!!.favorite!!)
                })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_records)

        PersonalRecordsActivity.calculatePersonalRecords()

        PersonalRecordsActivity.recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val layoutManager = LinearLayoutManager(getApplicationContext())
        PersonalRecordsActivity.recyclerView!!.setLayoutManager(layoutManager)
        PersonalRecordsActivity.recyclerView!!.setItemAnimator(DefaultItemAnimator())

        PersonalRecordsActivity.exerciseStatsAdapter = ExerciseStatsAdapter(this, PersonalRecordsActivity.exerciseStats)
        PersonalRecordsActivity.recyclerView!!.setAdapter(PersonalRecordsActivity.exerciseStatsAdapter)
    }

    override fun onStop() {
        super.onStop()
        MainActivity.dataStorage.saveKnownExerciseData(this)
    }

    override fun onRestart() {
        super.onRestart()
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
                PersonalRecordsActivity.exerciseStatsAdapter!!.getFilter().filter(s)
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