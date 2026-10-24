package com.readyplayerai.gainsgoblin.ui.compose.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.data.backup.ImportPreview
import com.readyplayerai.gainsgoblin.ui.SettingsViewModel
import com.readyplayerai.gainsgoblin.ui.compose.settings.SettingsScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Settings tab: collects [SettingsViewModel] and renders [SettingsScreen].
 * [onDataDeleted] is invoked after "delete all data" so the host can navigate
 * back to the Journal tab.
 */
@Composable
fun SettingsRoute(onDataDeleted: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val hasUndoBackup by viewModel.hasUndoBackup.collectAsState()
    var pending by remember { mutableStateOf<Pair<Uri, ImportPreview>?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val preview = viewModel.previewImport(uri)
                pending = uri to preview
            } catch (e: Exception) {
                settingsToast(context, context.getString(R.string.settings_import_failed, e.message ?: e.toString()))
            }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                viewModel.exportBackup(uri)
                settingsToast(context, context.getString(R.string.settings_backup_exported))
            } catch (e: Exception) {
                settingsToast(context, e.message ?: e.toString())
            }
        }
    }

    SettingsScreen(
        hasUndoBackup = hasUndoBackup,
        importPreview = pending?.second,
        onImportPick = { importPicker.launch(arrayOf("*/*")) },
        onImportMode = { mode ->
            val current = pending
            if (current != null) {
                pending = null
                scope.launch {
                    try {
                        val result = viewModel.importBackup(current.first, mode)
                        settingsToast(context, context.getString(R.string.settings_import_success, result.setCount))
                    } catch (e: Exception) {
                        settingsToast(context, context.getString(R.string.settings_import_failed, e.message ?: e.toString()))
                    }
                }
            }
        },
        onImportCancel = { pending = null },
        onExportClick = { exportPicker.launch(settingsBackupFileName()) },
        onUndoClick = {
            scope.launch {
                val ok = viewModel.undoLastImport()
                settingsToast(
                    context,
                    context.getString(
                        if (ok) R.string.settings_undo_import_done else R.string.settings_undo_import_none,
                    ),
                )
            }
        },
        onDeleteConfirm = {
            scope.launch {
                viewModel.clearWorkouts()
                settingsToast(context, context.getString(R.string.settings_data_deleted))
                onDataDeleted()
            }
        },
        onSourceCodeClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/READYPLAYERAI/GainsGoblin")),
            )
        },
        onLicenceClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.en.html")),
            )
        },
    )
}

private fun settingsToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private fun settingsBackupFileName(): String =
    "gainsgoblin_" +
        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date()) +
        ".json"
