package com.readyplayerai.gainsgoblin.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.categoryFor as exerciseCategoryFor
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme
import com.readyplayerai.gainsgoblin.ui.compose.home.HomeScreen
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar

import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val READ_REQUEST_CODE: Int = 42
    }

    // Selected `yyyy-MM-dd` date; defaulted to today in [onCreateStuff].
    private var dateSelected: String? = null

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupHome()
        // Hacky way to have the same code run in onRestart() as well
        onCreateStuff()
    }

    // General Initialization Stuff
    fun onCreateStuff() {
        initActivity()

        setupBottomNavBar(BottomNavTab.HOME) { dateSelected }

        // Date selected is by default today
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        dateSelected = dateFormat.format(Date())
    }

    // The Compose home island: pager, strip, chips, calendar, appbar.
    private fun setupHome() {
        val activity = this
        findViewById<ComposeView>(R.id.home_view).setContent {
            GainsGoblinTheme {
                val uiState by viewModel.uiState.collectAsState()
                HomeScreen(
                    uiState = uiState,
                    categoryFor = { name -> exerciseCategoryFor(uiState.knownExercises, name) },
                    onExerciseClick = { exercise, date ->
                        val intent = Intent(activity, AddExerciseActivity::class.java)
                        intent.putExtra("exercise", exercise)
                        dateSelected = date
                        date?.let { viewModel.selectDate(it) }
                        activity.startActivity(intent)
                    },
                    onSelectDate = { date ->
                        dateSelected = date
                        viewModel.selectDate(date)
                    },
                )
            }
        }
    }

    // You guessed it!
    fun initActivity() {
        // From Settings Activity when importing CSV
        val whatToDo = intent.getStringExtra("doit")
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
            // No pager re-init: the Compose home collects the repository StateFlow.
        }
    }

    override fun onRestart() {
        // Nothing to re-init: the Compose home is StateFlow-driven.
        super.onRestart()
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

        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (requestCode == READ_REQUEST_CODE && uri != null) {
                lifecycleScope.launch {
                    val result = viewModel.importCsv(uri)
                    if (!result.success) {
                        result.error?.let {
                            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                        }
                        Toast.makeText(applicationContext, "Could not locate file " + uri.path, Toast.LENGTH_SHORT).show()
                    }
                    // No pager re-init: the Compose home re-renders from the StateFlow.
                }
            }
        }
    }
}
