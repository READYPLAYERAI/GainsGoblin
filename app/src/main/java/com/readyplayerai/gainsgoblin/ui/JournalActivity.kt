package com.readyplayerai.gainsgoblin.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.readyplayerai.gainsgoblin.ui.compose.journal.JournalScreen
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar

import dagger.hilt.android.AndroidEntryPoint

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


@AndroidEntryPoint
class JournalActivity : AppCompatActivity() {

    private val viewModel: JournalViewModel by viewModels()

    // Incremented on bottom-nav JOURNAL reselect; Journal's feed collects it and scrolls to today.
    private val journalScrollToTopTick = MutableStateFlow(0)

    // Writes the retained raw legacy payloads to a user-chosen file.
    private val rawRecoveryPicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val ok = viewModel.exportLegacyRaw(uri)
            Toast.makeText(
                this@JournalActivity,
                if (ok) getString(R.string.storage_recovery_exported)
                else getString(R.string.storage_recovery_export_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupJournal()
        // Hacky way to have the same code run in onRestart() as well
        onCreateStuff()
    }

    // General Initialization Stuff
    fun onCreateStuff() {
        setupBottomNavBar(
            currentTab = BottomNavTab.JOURNAL,
            onJournalTabReselect = { journalScrollToTopTick.value++ },
        )
    }

    // The Compose journal island: vertical journal feed + flexible app bar.
    private fun setupJournal() {
        val activity = this
        findViewById<ComposeView>(R.id.journal_view).setContent {
            GainsGoblinTheme {
                val uiState by viewModel.uiState.collectAsState()
                JournalScreen(
                    uiState = uiState,
                    categoryFor = { name -> exerciseCategoryFor(uiState.knownExercises, name) },
                    onExerciseClick = { exercise, date ->
                        val intent = Intent(activity, AddExerciseActivity::class.java)
                        intent.putExtra("exercise", exercise)
                        date?.let { viewModel.selectDate(it) }
                        activity.startActivity(intent)
                    },
                    onSelectDate = { date ->
                        viewModel.selectDate(date)
                    },
                    scrollToTopTick = journalScrollToTopTick,
                    onExportRawData = {
                        rawRecoveryPicker.launch("gainsgoblin_legacy_raw_" + defaultTimestamp() + ".txt")
                    },
                    onRetryStorage = {
                        lifecycleScope.launch {
                            val ok = viewModel.retryMigration()
                            if (!ok) {
                                Toast.makeText(
                                    this@JournalActivity,
                                    getString(R.string.storage_recovery_retry_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                )
            }
        }
    }

    private fun defaultTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date())

    override fun onRestart() {
        // Nothing to re-init: the Compose journal is StateFlow-driven.
        super.onRestart()
    }
}
