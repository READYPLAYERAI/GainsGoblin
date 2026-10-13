package com.readyplayerai.gainsgoblin.ui

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast

import com.readyplayerai.gainsgoblin.adapters.DayExerciseAdapter
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutExercise
import com.google.android.material.floatingactionbutton.FloatingActionButton

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

class DayActivity : AppCompatActivity() {

    var recyclerView: RecyclerView? = null
    var workoutExerciseAdapter: DayExerciseAdapter? = null
    var date_clicked: String? = null

    var fab: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day)

        // Self Explanatory I guess
        initActivity()
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

        // From Main Activity
        val mIntent = getIntent()
        val extras = mIntent.getExtras()
        date_clicked = extras!!.getString("date")

        //Date Stuff
        val parser = SimpleDateFormat("yyyy-MM-dd")
        var date: Date? = null

        try {
            date = parser.parse(date_clicked)
            val formatter = SimpleDateFormat("EEEE, MMM dd YYYY")
            val formated_date = formatter.format(date)
            supportActionBar!!.setTitle(formated_date)

            var Today_Exercises = ArrayList<WorkoutExercise>()

            for (i in 0 until MainActivity.dataStorage.workoutDays.size) {
                if (date_clicked == MainActivity.dataStorage.workoutDays[i].Date) {
                    Today_Exercises = MainActivity.dataStorage.workoutDays[i].Exercises!!
                }
            }

            // Set Recycler View
            workoutExerciseAdapter = DayExerciseAdapter(this, Today_Exercises)
            recyclerView!!.setAdapter(workoutExerciseAdapter)
            recyclerView!!.setLayoutManager(LinearLayoutManager(this))

            // Notify User
            if (Today_Exercises.isEmpty()) {
                Toast.makeText(getApplicationContext(), "No Logged Exercises", Toast.LENGTH_SHORT).show()
            }

        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    // When back button is pressed by another app
    override fun onRestart() {
        super.onRestart()

        // Self Explanatory I guess
        initActivity()
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