package com.readyplayerai.gainsgoblin.ui.compose.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.categoryFor as exerciseCategoryFor
import com.readyplayerai.gainsgoblin.ui.JournalViewModel
import com.readyplayerai.gainsgoblin.ui.compose.journal.JournalScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Journal tab: collects [JournalViewModel] and renders [JournalScreen]. */
@Composable
fun JournalRoute(scrollToTopTick: StateFlow<Int>) {
    val viewModel: JournalViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rawRecoveryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = viewModel.exportLegacyRaw(uri)
            Toast.makeText(
                context,
                if (ok) context.getString(R.string.storage_recovery_exported)
                else context.getString(R.string.storage_recovery_export_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    JournalScreen(
        uiState = uiState,
        categoryFor = { name -> exerciseCategoryFor(uiState.knownExercises, name) },
        onSelectDate = { date -> viewModel.selectDate(date) },
        scrollToTopTick = scrollToTopTick,
        onExportRawData = {
            rawRecoveryPicker.launch("gainsgoblin_legacy_raw_" + journalTimestamp() + ".txt")
        },
        onRetryStorage = {
            scope.launch {
                val ok = viewModel.retryMigration()
                if (!ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.storage_recovery_retry_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        },
        onAddSet = { date, name, category, reps, weight, comment ->
            scope.launch { viewModel.addJournalSet(date, name, category, reps, weight, comment) }
        },
        onUpdateSet = { date, name, index, weight, reps ->
            scope.launch { viewModel.updateJournalSet(date, name, index, weight, reps) }
        },
        onDeleteSet = { date, name, index ->
            scope.launch { viewModel.deleteJournalSet(date, name, index) }
        },
        onSetNote = { date, name, index, note ->
            scope.launch { viewModel.updateJournalSetNote(date, name, index, note) }
        },
        onExerciseNote = { date, name, note ->
            scope.launch { viewModel.updateJournalExerciseNote(date, name, note) }
        },
        onAddExercise = { name, category, onResult ->
            scope.launch { onResult(viewModel.addExercise(name, category)) }
        },
    )
}

private fun journalTimestamp(): String =
    SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date())
