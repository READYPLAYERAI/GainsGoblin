package com.readyplayerai.gainsgoblin.ui

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView

import com.readyplayerai.gainsgoblin.R
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap

class ChartsActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        // Basic Initialization
        onCreateStuff()
    }

    override fun onRestart() {
        super.onRestart()
        onCreateStuff()
    }

    fun onCreateStuff() {
        // Bottom Navigation Bar Intents
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setSelectedItemId(R.id.charts)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        if (MainActivity.dataStorage.workoutDays.size == 0) {
            scrollView.setVisibility(View.GONE)
        } else {
            scrollView.setVisibility(View.VISIBLE)
        }

        barChartVolumes()

        pieChartWorkouts()

        pieChartBodyparts()

        piChartExercises()
    }

    fun barChartVolumes() {
        // Bar Chart Example
        val barChart = findViewById<BarChart>(R.id.barChart)
        // Interaction Customization

        // Add Data pairs in List
        val workouts = ArrayList<BarEntry>()

        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            workouts.add(BarEntry(i.toFloat(), MainActivity.dataStorage.workoutDays[i].DayVolume!!.toFloat()))
        }

        // Add Date Labels to workout
        val workoutDates = ArrayList<String>()

        // Make it invisible because otherwise it looks like shit
        if (workouts.size == 0) {
            barChart.setVisibility(View.INVISIBLE)
        }

        // Remove Legend
        val l = barChart.getLegend()
        l.setEnabled(false)

        // Show last X workouts only
        val last_workouts = 5
        var counter = 0
        val workouts_pruned = ArrayList<BarEntry>()

        for (i in 0 until workouts.size) {
            counter++
            if (counter > workouts.size - last_workouts) {
                workouts_pruned.add(workouts[i])
            }
        }

        val barDataSet = BarDataSet(workouts_pruned, "Workouts")
        barDataSet.setColors(ColorTemplate.JOYFUL_COLORS.toList())

        barChart.getXAxis().setValueFormatter(IndexAxisValueFormatter(workoutDates))

        // Profit
        val barData = BarData(barDataSet)
        barChart.setFitBars(true)
        barChart.setData(barData)
        barChart.getDescription().setText("")
        barChart.invalidate()
        barChart.animateY(500)
        barChart.setScaleMinima(1f, 1f)
    }

    fun pieChartWorkouts() {
        // Find Workout Years
        val Years = HashSet<String>()

        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            val date = MainActivity.dataStorage.workoutDays[i].Date
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
        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            val date = MainActivity.dataStorage.workoutDays[i].Date
            val year = date!!.substring(0, 4)

            var workouts = Years_Workouts[year]!!
            Years_Workouts.put(year, workouts + 1)
        }

        // Initialize Pie Chart
        val pieChart = findViewById<PieChart>(R.id.pieChart)

        pieChart.setUsePercentValues(false)
        pieChart.getDescription().setEnabled(false)
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.setDragDecelerationFrictionCoef(0.95f)
        pieChart.setDrawHoleEnabled(false)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleRadius(60f)

        val yValues = ArrayList<PieEntry>()

        for (entry in Years_Workouts) {
            val workouts = entry.value
            val year = entry.key

            yValues.add(PieEntry(workouts.toFloat(), year))
        }

        val pieDataSet = PieDataSet(yValues, "")

        pieDataSet.setSliceSpace(3f)
        pieDataSet.setSelectionShift(5f)
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS.toList())

        val data = PieData(pieDataSet)
        data.setValueTextSize(15f)
        data.setValueTextColor(Color.WHITE)

        pieChart.animateY(1000, Easing.EaseInOutCubic)
        pieChart.setNoDataText("No Workouts")
        pieChart.getLegend().setEnabled(false)
        pieChart.setData(data)
    }

    fun pieChartBodyparts() {
        // Find Workout Years
        val Bodyparts = HashSet<String>()

        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                val Exercise = MainActivity.dataStorage.workoutDays[i].Sets!![j].category
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
        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                val Exercise = MainActivity.dataStorage.workoutDays[i].Sets!![j].category!!

                var Exercise_Workouts = Number_Bodyparts[Exercise]!!
                Number_Bodyparts.put(Exercise, Exercise_Workouts + 1)
            }
        }

        // Initialize Pie Chart
        val pieChart = findViewById<PieChart>(R.id.pieChartBodyparts)

        pieChart.setUsePercentValues(false)
        pieChart.getDescription().setEnabled(false)
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.setDragDecelerationFrictionCoef(0.95f)
        pieChart.setDrawHoleEnabled(false)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleRadius(60f)

        val yValues = ArrayList<PieEntry>()

        for (entry in Number_Bodyparts) {
            val workouts = entry.value
            val year = entry.key

            yValues.add(PieEntry(workouts.toFloat(), year))
        }

        val pieDataSet = PieDataSet(yValues, "")

        pieDataSet.setSliceSpace(3f)
        pieDataSet.setSelectionShift(5f)
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS.toList())

        val data = PieData(pieDataSet)
        data.setValueTextSize(15f)
        data.setValueTextColor(Color.WHITE)

        pieChart.getLegend().setEnabled(false)
        pieChart.animateY(1000, Easing.EaseInOutCubic)
        pieChart.setNoDataText("No Workouts")
        pieChart.setData(data)
    }

    fun piChartExercises() {
        // Find Workout Years
        val Exercises = HashSet<String>()

        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                val Exercise = MainActivity.dataStorage.workoutDays[i].Sets!![j].exerciseName
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
        for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
            for (j in 0 until MainActivity.dataStorage.workoutDays[i].Sets!!.size) {
                val Exercise = MainActivity.dataStorage.workoutDays[i].Sets!![j].exerciseName!!

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
        val pieChart = findViewById<PieChart>(R.id.pieChartExercises)

        pieChart.setUsePercentValues(false)
        pieChart.getDescription().setEnabled(false)
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.setDragDecelerationFrictionCoef(0.95f)
        pieChart.setDrawHoleEnabled(false)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleRadius(60f)

        val yValues = ArrayList<PieEntry>()

        for (entry in Number_Exercises_Sorted_Pruned) {
            val workouts = entry.value
            val year = entry.key

            yValues.add(PieEntry(workouts.toFloat(), year))
        }

        val pieDataSet = PieDataSet(yValues, "")

        pieDataSet.setSliceSpace(3f)
        pieDataSet.setSelectionShift(5f)
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS.toList())

        val data = PieData(pieDataSet)
        data.setValueTextSize(15f)
        data.setValueTextColor(Color.WHITE)

        pieChart.getLegend().setEnabled(false)
        pieChart.animateY(1000, Easing.EaseInOutCubic)
        pieChart.setNoDataText("No Workouts")
        pieChart.setData(data)
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

    // Navigates to given activity based on the selected menu item
    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.home) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } else if (item.itemId == R.id.exercises) {
            val intent = Intent(this, ExercisesActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } else if (item.itemId == R.id.diary) {
            val intent = Intent(this, DiaryActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } else if (item.itemId == R.id.charts) {
            val intent = Intent(this, ChartsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } else if (item.itemId == R.id.me) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        return true
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