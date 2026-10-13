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
import android.widget.Toast

import com.readyplayerai.gainsgoblin.adapters.DiaryAdapter
import com.readyplayerai.gainsgoblin.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class DiaryActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    // Helper Data Structures
    var recyclerView: RecyclerView? = null
    var diaryAdapter: DiaryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary)

        System.out.println("DiaryActivity::OnCreate()")
        System.out.println("Size: " + MainActivity.dataStorage.workoutDays.size)

        initActivity()
    }

    override fun onRestart() {
        super.onRestart()

        System.out.println("DiaryActivity::OnRestart()")
        initActivity()
    }

    fun initActivity() {
        System.out.println("DiaryActivity::initActivity()")

        // From Day Activity
        val intent = getIntent()
        val date_clicked = intent.getStringExtra("date")
        // String date_clicked = MainActivity.date_selected;

        // If day exists scroll to it otherwise scroll to the last day
        var position = -1

        if (date_clicked != null) {
            position = MainActivity.dataStorage.getDayPosition(date_clicked)
        }

        System.out.println("Date is " + date_clicked + " and position is: " + position)
        System.out.println("MainActivity.Workout_Days.size(): " + MainActivity.dataStorage.workoutDays.size)

        // Bottom Navigation Bar Intents
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setSelectedItemId(R.id.diary)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        // Find Recycler View Object
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        // Notify User in case of empty diary
        if (MainActivity.dataStorage.workoutDays.isEmpty()) {
            Toast.makeText(this, "Empty Diary", Toast.LENGTH_SHORT).show()
        } else {
            // So the adapter has correct information
            MainActivity.dataStorage.calculatePersonalRecords()

            // Crash Otherwise
            diaryAdapter = DiaryAdapter(this, MainActivity.dataStorage.workoutDays)
            recyclerView!!.setAdapter(diaryAdapter)

            val lm = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true) // last argument (true) is flag for reverse layout
            lm.setReverseLayout(true)
            lm.setStackFromEnd(true)
            recyclerView!!.setLayoutManager(lm)

            // BUG HERE FIX IT SER
            if (position >= 0) {
                // Scroll to the selected date
                System.out.println("Scroll to position")
//                recyclerView.scrollToPosition(position)
            } else {
                // Scroll to the bottom
                System.out.println("Scroll to the bottom")
//                recyclerView.scrollToPosition(MainActivity.Workout_Days.size()-1)
            }
        }
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
        inflater.inflate(R.menu.diary_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}