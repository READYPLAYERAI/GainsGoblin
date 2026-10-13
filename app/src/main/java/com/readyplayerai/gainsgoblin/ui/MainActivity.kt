package com.readyplayerai.gainsgoblin.ui

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
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

import com.readyplayerai.gainsgoblin.DataStorage
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.adapters.ViewPagerWorkoutDayAdapter
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

import java.text.DateFormat
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, DatePickerDialog.OnDateSetListener {

    companion object {
        @JvmField
        val dataStorage: DataStorage = DataStorage() // Holds all Gains Goblin data and handles file I/O
        @JvmField
        var dateSelected: String? = null // Used for other activities to get the selected date, by default it's set to today
        @JvmField
        var viewPager2: ViewPager2? = null // View Pager that is used in main activity

        const val READ_REQUEST_CODE: Int = 42
        @JvmField
        var EXPORT_FILENAME: String = "gainsgoblin_backup"

        @JvmStatic
        fun setExportBackupName() {
            EXPORT_FILENAME = "gainsgoblin"
            val formatter: Format = SimpleDateFormat("_yyyy-MM-dd_HH:mm:ss")
            val str_date = formatter.format(Date())
            EXPORT_FILENAME = EXPORT_FILENAME + str_date
        }
    }

    @JvmField
    var fab: FloatingActionButton? = null

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
        MainActivity.dateSelected = date_clicked

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
        setExportBackupName()

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
                    dataStorage.writeFile(applicationContext)
                }

                // Or else nothing comes up
                initViewPager()
            }
        } else {
            // Offline mode: load local data
            dataStorage.loadWorkoutData(applicationContext)
            dataStorage.loadKnownExercisesData(applicationContext)
            initViewPager()
        }
    }

    override fun onRestart() {
        // This was already there so I am not deleting it
        super.onRestart()

        // Get WorkoutDays from shared preferences
        dataStorage.loadWorkoutData(applicationContext)

        // Get Known Exercises from shared preferences
        dataStorage.loadKnownExercisesData(applicationContext)

        // After Loading Data Initialize ViewPager
        initViewPager()

        // Bottom Navigation Bar Intents
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setSelectedItemId(R.id.home)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    // Initialize View pager object
    fun initViewPager() {
        // Skip creation of empty workouts if you don't have to
        if (dataStorage.infiniteWorkoutDays.isEmpty() || dataStorage.infiniteWorkoutDays == null) {
            // "Infinite" Data Structure
            dataStorage.infiniteWorkoutDays.clear()

            // Find start and End Dates
            val c = Calendar.getInstance()
            c.time = Date()
            c.add(Calendar.YEAR, -5)
            val startDate = c.time
            c.add(Calendar.YEAR, +10)
            val endDate = c.time

            // Create Calendar Objects that represent start and end date
            val start = Calendar.getInstance()
            start.time = startDate
            val end = Calendar.getInstance()
            end.time = endDate

            // Construct 20 years worth of empty workout days
            var date = start.time
            while (start.before(end)) {
                // Get Date in String format
                val date_str = SimpleDateFormat("yyyy-MM-dd").format(date)

                // Create new mostly empty object
                val today = WorkoutDay()
                today.Date = date_str
                dataStorage.infiniteWorkoutDays.add(today)

                start.add(Calendar.DATE, 1)
                date = start.time
            }
        }

        // Use View Pager with Infinite Days
        viewPager2 = findViewById(R.id.viewPager2)
        viewPager2!!.setAdapter(ViewPagerWorkoutDayAdapter(this, dataStorage.infiniteWorkoutDays))
        viewPager2!!.visibility = View.VISIBLE
        val today_position = getTodayPosition() // Navigate to today
        if (today_position >= 0) {
            viewPager2!!.setCurrentItem(today_position)
        }

        viewPager2!!.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                val dateScrolled = dataStorage.infiniteWorkoutDays[position].Date
                MainActivity.dateSelected = dateScrolled
            }
        })

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.GONE
    }

    // Returns the position of today's date within the infinite workout days list, or -1 if not found
    private fun getTodayPosition(): Int {
        val today_str = SimpleDateFormat("yyyy-MM-dd").format(Date())
        for (i in dataStorage.infiniteWorkoutDays.indices) {
            if (dataStorage.infiniteWorkoutDays[i].Date == today_str) {
                return i
            }
        }
        return -1
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
                    if (dataStorage.readFile(uri!!, applicationContext)) {
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
            val today_position = getTodayPosition() // Navigate to today
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