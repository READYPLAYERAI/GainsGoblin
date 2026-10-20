package com.readyplayerai.gainsgoblin.ui

import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.ProgressBar
import android.widget.Toast

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.adapters.ViewPagerWorkoutDayAdapter
import com.readyplayerai.gainsgoblin.domain.categoryFor as exerciseCategoryFor
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, DatePickerDialog.OnDateSetListener {

    companion object {
        const val READ_REQUEST_CODE: Int = 42
    }

    @JvmField
    var fab: FloatingActionButton? = null

    // Selected `yyyy-MM-dd` date; defaulted to today in [onCreateStuff].
    private var dateSelected: String? = null

    // The home pager; initialized once the repository reports loaded.
    private var viewPager2: ViewPager2? = null

    private val viewModel: MainViewModel by viewModels()

    // Guards the one-time pager initialization driven by the state collector.
    private var pagerNeedsInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab = findViewById(R.id.floatingActionButton)

        fab!!.setOnClickListener { view: View ->
            val intent = Intent(this@MainActivity, ExercisesActivity::class.java)
            startActivity(intent)
        }

        // Hacky way to have the same code run in onRestart() as well
        onCreateStuff()

        // Initialize the pager exactly once, as soon as the repository reports loaded
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.loaded && pagerNeedsInit) {
                        pagerNeedsInit = false
                        initViewPager()
                    }
                }
            }
        }
    }

    // General Initialization Stuff
    fun onCreateStuff() {
        initActivity()

        // Bottom Navigation Bar Intents
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setSelectedItemId(R.id.home)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        // Date selected is by default today
        val date_clicked = Date()
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        dateSelected = dateFormat.format(date_clicked)
    }

    // When choosing date from DatePicker
    override fun onDateSet(datePicker: DatePicker?, i: Int, i1: Int, i2: Int) {
        val month = i1 + 1
        val year = i.toString()
        val monthStr: String
        val day: String

        monthStr = String.format("%02d", month)
        day = String.format("%02d", i2)

        val date_clicked = year + "-" + monthStr + "-" + day
        dateSelected = date_clicked
        viewModel.selectDate(date_clicked)

        // Start Intent
        val intent = Intent(applicationContext, DayActivity::class.java)
        val mBundle = Bundle()

        // Send Date and start activity
        mBundle.putString("date", date_clicked)
        intent.putExtras(mBundle)
        startActivity(intent)
    }

    // You guessed it!
    fun initActivity() {
        // Rename top bar to something sensible
        supportActionBar!!.setTitle(R.string.app_name)

        // From Settings Activity when importing CSV
        val intent = intent

        val whatToDo = intent.getStringExtra("doit")

        // If Intent coming from settings activity
        if (whatToDo != null) {
            if (whatToDo == "importcsv") {
                fileSearch()
            } else if (whatToDo == "exportcsv") {
                // Android 11 and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    lifecycleScope.launch {
                        val result = viewModel.exportCsv()
                        Toast.makeText(applicationContext, result.message, Toast.LENGTH_LONG).show()
                    }
                }

                // Or else nothing comes up
                lifecycleScope.launch { initViewPager() }
            }
        }
        // else (normal launch): nothing here, the state collector initializes the pager
    }

    override fun onRestart() {
        // This was already there so I am not deleting it
        super.onRestart()

        // After Loading Data Initialize ViewPager
        lifecycleScope.launch { initViewPager() }

        // Bottom Navigation Bar Intents
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setSelectedItemId(R.id.home)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    // Initialize View pager object
    private suspend fun initViewPager() {
        val state = viewModel.freshUiState()
        if (!state.loaded) {
            return
        }

        // Use View Pager with Infinite Days
        viewPager2 = findViewById(R.id.viewPager2)
        viewPager2!!.setAdapter(
            ViewPagerWorkoutDayAdapter(
                this,
                state.infiniteDays,
                state.realDays,
                onExerciseClick = { exercises, position ->
                    val exercise = exercises[position]
                    val intent = Intent(this, AddExerciseActivity::class.java)
                    intent.putExtra("exercise", exercise.Exercise)
                    dateSelected = exercise.Date
                    exercise.Date?.let { viewModel.selectDate(it) }
                    startActivity(intent)
                },
                categoryFor = { name -> exerciseCategoryFor(state.knownExercises, name) }
            )
        )
        viewPager2!!.visibility = View.VISIBLE
        if (state.todayPosition >= 0) {
            viewPager2!!.setCurrentItem(state.todayPosition)
        }

        viewPager2!!.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                val dateScrolled = state.infiniteDays[position].Date
                dateSelected = dateScrolled
                dateScrolled?.let { viewModel.selectDate(it) }
            }
        })

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.GONE
    }

    // Select a file using the build in file manager
    fun fileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/*"
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    // When File explorer stops this function runs
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                if (requestCode == READ_REQUEST_CODE) {
                    lifecycleScope.launch {
                        val result = viewModel.importCsv(uri!!)
                        if (!result.success) {
                            result.error?.let {
                                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                            }
                            Toast.makeText(applicationContext, "Could not locate file " + uri.path, Toast.LENGTH_SHORT).show()
                        }
                        initViewPager()
                    }
                }
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
            intent.putExtra("date", dateSelected)
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
        val inflater = menuInflater
        inflater.inflate(R.menu.main_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == R.id.home) {
            val today_position = viewModel.uiState.value.todayPosition // Navigate to today
            if (today_position >= 0) {
                viewPager2!!.setCurrentItem(today_position)
            }
        } else if (item.itemId == R.id.settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}
