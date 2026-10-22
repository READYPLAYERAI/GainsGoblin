package com.readyplayerai.gainsgoblin.ui

import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import com.readyplayerai.gainsgoblin.adapters.DiaryAdapter
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.categoryFor as exerciseCategoryFor
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTopBar
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar
import com.readyplayerai.gainsgoblin.ui.compose.setupTopBar

import java.util.ArrayList

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiaryActivity : AppCompatActivity() {

    // Helper Data Structures
    var recyclerView: RecyclerView? = null
    var diaryAdapter: DiaryAdapter? = null

    private val viewModel: DiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_diary)

        setupTopBar { GainsGoblinTopBar(title = "Diary") }

        System.out.println("DiaryActivity::OnCreate()")

        initActivity()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()

        System.out.println("DiaryActivity::OnRestart()")
        lifecycleScope.launch { render(viewModel.freshUiState()) }
    }

    fun initActivity() {
        System.out.println("DiaryActivity::initActivity()")

        setupBottomNavBar(BottomNavTab.DIARY)

        // Find Recycler View Object
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
    }

    private fun render(state: DiaryUiState) {
        // From Day Activity
        val date_clicked = intent.getStringExtra("date")

        System.out.println("Date is " + date_clicked + " and position is: " + state.initialPosition)
        System.out.println("Size: " + state.days.size)
        System.out.println("MainActivity.Workout_Days.size(): " + state.days.size)

        // Notify User in case of empty diary
        if (state.days.isEmpty()) {
            Toast.makeText(this, "Empty Diary", Toast.LENGTH_SHORT).show()
        } else {
            // Crash Otherwise
            diaryAdapter = DiaryAdapter(
                this,
                ArrayList(state.days),
                onDayClick = { position ->
                    val day = state.days[position]
                    day.Date?.let { viewModel.selectDate(it) }
                    val startIntent = Intent(this, DayActivity::class.java)
                    startIntent.putExtra("date", day.Date)
                    startActivity(startIntent)
                },
                onExerciseClick = { day, position ->
                    val ex = day.Exercises!![position]
                    ex.Date?.let { viewModel.selectDate(it) }
                    System.out.println(ex.Exercise)
                    val startIntent = Intent(this, AddExerciseActivity::class.java)
                    startIntent.putExtra("exercise", ex.Exercise)
                    startActivity(startIntent)
                },
                categoryFor = { name -> exerciseCategoryFor(state.knownExercises, name) }
            )
            recyclerView!!.setAdapter(diaryAdapter)

            val lm = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true) // last argument (true) is flag for reverse layout
            lm.setReverseLayout(true)
            lm.setStackFromEnd(true)
            recyclerView!!.setLayoutManager(lm)

            // BUG HERE FIX IT SER
            if (state.initialPosition >= 0) {
                // Scroll to the selected date
                System.out.println("Scroll to position")
//                recyclerView.scrollToPosition(state.initialPosition)
            } else {
                // Scroll to the bottom
                System.out.println("Scroll to the bottom")
//                recyclerView.scrollToPosition(state.days.size - 1)
            }
        }
    }

}
