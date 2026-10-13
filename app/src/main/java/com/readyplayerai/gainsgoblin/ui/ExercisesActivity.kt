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
import android.view.inputmethod.EditorInfo

import com.readyplayerai.gainsgoblin.adapters.ExerciseAdapter
import com.readyplayerai.gainsgoblin.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class ExercisesActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    var recyclerView: RecyclerView? = null
    var exerciseAdapter: ExerciseAdapter? = null
    var date_clicked: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercises)

        onCreateStuff()
    }

    override fun onRestart() {
        super.onRestart()
        onCreateStuff()
    }

    fun onCreateStuff() {
        // Intent from DayActivity
        val intent = getIntent()
        date_clicked = intent.getStringExtra("date")

        // Bottom Navigation Bar Intents
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setSelectedItemId(R.id.exercises)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        // Find Recycler View Object
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_exercises)
        exerciseAdapter = ExerciseAdapter(this, MainActivity.dataStorage.knownExercises)
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
}