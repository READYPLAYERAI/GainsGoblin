package com.readyplayerai.gainsgoblin.ui

import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.ui.charts.BarChartView
import com.readyplayerai.gainsgoblin.ui.charts.PieChartView
import com.readyplayerai.gainsgoblin.ui.charts.PieSlice
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap

@AndroidEntryPoint
class ChartsActivity : AppCompatActivity() {

    private val viewModel: ChartsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        setupBottomNavBar(BottomNavTab.CHARTS)

        // Basic Initialization
        onCreateStuff()
    }

    override fun onRestart() {
        super.onRestart()
        onCreateStuff()
    }

    fun onCreateStuff() {
        lifecycleScope.launch { render(viewModel.freshUiState()) }
    }

    private fun render(state: ChartsUiState) {
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        if (state.days.isEmpty()) {
            scrollView.setVisibility(View.GONE)
        } else {
            scrollView.setVisibility(View.VISIBLE)
        }

        barChartVolumes(state.days)

        pieChartWorkouts(state.days)

        pieChartBodyparts(state.days)

        piChartExercises(state.days)
    }

    fun barChartVolumes(days: List<WorkoutDay>) {
        // Bar Chart
        val barChart = findViewById<BarChartView>(R.id.barChart)

        // Add volumes
        val workouts = ArrayList<Float>()

        for (i in 0 until days.size) {
            workouts.add(days[i].DayVolume!!.toFloat())
        }

        // Make it invisible because otherwise it looks like shit
        if (workouts.size == 0) {
            barChart.setVisibility(View.INVISIBLE)
        }

        // Show last X workouts only
        val last_workouts = 5
        var counter = 0
        val volumes = ArrayList<Float>()

        for (i in 0 until workouts.size) {
            counter++
            if (counter > workouts.size - last_workouts) {
                volumes.add(workouts[i])
            }
        }

        barChart.setData(volumes)
    }

    fun pieChartWorkouts(days: List<WorkoutDay>) {
        // Find Workout Years
        val Years = HashSet<String>()

        for (i in 0 until days.size) {
            val date = days[i].Date
            val year = date!!.substring(0, 4)
            Years.add(year)
        }

        // Workout years and number of workouts per year
        val Years_Workouts = HashMap<String, Int>()

        // Iterate set years
        Years.forEach { year ->
            Years_Workouts.put(year, 0)
        }

        // Calculate number of workouts per year
        for (i in 0 until days.size) {
            val date = days[i].Date
            val year = date!!.substring(0, 4)

            var workouts = Years_Workouts[year]!!
            Years_Workouts.put(year, workouts + 1)
        }

        // Initialize Pie Chart
        val pieChart = findViewById<PieChartView>(R.id.pieChart)

        val slices = ArrayList<PieSlice>()

        for (entry in Years_Workouts) {
            val count = entry.value
            val year = entry.key

            slices.add(PieSlice(label = year, value = count.toFloat()))
        }

        pieChart.setNoDataText("No Workouts")
        pieChart.setData(slices)
    }

    fun pieChartBodyparts(days: List<WorkoutDay>) {
        // Find Workout Years
        val Bodyparts = HashSet<String>()

        for (i in 0 until days.size) {
            for (j in 0 until days[i].Sets!!.size) {
                val Exercise = days[i].Sets!![j].category
                Bodyparts.add(Exercise!!)
            }
        }

        // Workout years and number of workouts per year
        val Number_Bodyparts = HashMap<String, Int>()

        // Iterate set years
        Bodyparts.forEach { exercise ->
            Number_Bodyparts.put(exercise, 0)
        }

        // Calculate number of workouts per year
        for (i in 0 until days.size) {
            for (j in 0 until days[i].Sets!!.size) {
                val Exercise = days[i].Sets!![j].category!!

                var Exercise_Workouts = Number_Bodyparts[Exercise]!!
                Number_Bodyparts.put(Exercise, Exercise_Workouts + 1)
            }
        }

        // Initialize Pie Chart
        val pieChart = findViewById<PieChartView>(R.id.pieChartBodyparts)

        val slices = ArrayList<PieSlice>()

        for (entry in Number_Bodyparts) {
            val count = entry.value
            val name = entry.key

            slices.add(PieSlice(label = name, value = count.toFloat()))
        }

        pieChart.setNoDataText("No Workouts")
        pieChart.setData(slices)
    }

    fun piChartExercises(days: List<WorkoutDay>) {
        // Find Workout Years
        val Exercises = HashSet<String>()

        for (i in 0 until days.size) {
            for (j in 0 until days[i].Sets!!.size) {
                val Exercise = days[i].Sets!![j].exerciseName
                Exercises.add(Exercise!!)
            }
        }

        // Workout years and number of workouts per year
        val Number_Exercises = HashMap<String, Int>()

        // Iterate set years
        Exercises.forEach { exercise ->
            Number_Exercises.put(exercise, 0)
        }

        // Calculate number of workouts per year
        for (i in 0 until days.size) {
            for (j in 0 until days[i].Sets!!.size) {
                val Exercise = days[i].Sets!![j].exerciseName!!

                var Exercise_Workouts = Number_Exercises[Exercise]!!
                Number_Exercises.put(Exercise, Exercise_Workouts + 1)
            }
        }

        val Number_Exercises_Sorted = sortValues(Number_Exercises)

        System.out.println(Number_Exercises_Sorted.size)

        val Number_Exercises_Sorted_Pruned = HashMap<String, Int>()

        val top_exercises = 5
        var counter = 0

        for (entry in Number_Exercises_Sorted) {
            counter++
            if (counter > Number_Exercises_Sorted.size - top_exercises) {
                Number_Exercises_Sorted_Pruned.put(entry.key, entry.value)
                System.out.println(entry.key + " = " + entry.value)
            }
        }

        // Initialize Pie Chart
        val pieChart = findViewById<PieChartView>(R.id.pieChartExercises)

        val slices = ArrayList<PieSlice>()

        for (entry in Number_Exercises_Sorted_Pruned) {
            val count = entry.value
            val name = entry.key

            slices.add(PieSlice(label = name, value = count.toFloat()))
        }

        pieChart.setNoDataText("No Workouts")
        pieChart.setData(slices)
    }

    private fun sortValues(map: HashMap<String, Int>): HashMap<String, Int> {
        val entries = ArrayList<kotlin.collections.MutableMap.MutableEntry<String, Int>>()
        for (e in map) {
            entries.add(e)
        }
        //Custom Comparator
        Collections.sort(entries, Comparator<kotlin.collections.MutableMap.MutableEntry<String, Int>> { o1, o2 ->
            o1.value.compareTo(o2.value)
        })
        //copying the sorted list in HashMap to preserve the iteration order
        val sortedHashMap = LinkedHashMap<String, Int>()
        for (e in entries) {
            sortedHashMap.put(e.key, e.value)
        }
        return sortedHashMap
    }

    // Menu Stuff
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.charts_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } else if (item.itemId == R.id.personal_records) {
            val intent = Intent(this, PersonalRecordsActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}