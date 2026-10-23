package com.readyplayerai.gainsgoblin.ui

import android.content.Intent
import android.net.Uri
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
import com.readyplayerai.gainsgoblin.data.backup.ImportMode
import com.readyplayerai.gainsgoblin.data.backup.ImportPreview
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme
import com.readyplayerai.gainsgoblin.ui.compose.settings.SettingsScreen
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private val pendingImport = MutableStateFlow<Pair<Uri, ImportPreview>?>(null)

    private val importPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val preview = viewModel.previewImport(uri)
                pendingImport.value = uri to preview
            } catch (e: Exception) {
                toast(getString(R.string.settings_import_failed, e.message ?: e.toString()))
            }
        }
    }

    private val exportPicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                viewModel.exportBackup(uri)
                toast(getString(R.string.settings_backup_exported))
            } catch (e: Exception) {
                toast(e.message ?: e.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.settings_activity)

        setupBottomNavBar(BottomNavTab.SETTINGS)

        findViewById<ComposeView>(R.id.settings_view).setContent {
            GainsGoblinTheme {
                val hasUndoBackup by viewModel.hasUndoBackup.collectAsState()
                val pending by pendingImport.collectAsState()
                SettingsScreen(
                    hasUndoBackup = hasUndoBackup,
                    importPreview = pending?.second,
                    onImportPick = { importPicker.launch(arrayOf("*/*")) },
                    onImportMode = { mode -> handleImportMode(mode) },
                    onImportCancel = { pendingImport.value = null },
                    onExportClick = { exportPicker.launch(defaultBackupFileName()) },
                    onUndoClick = {
                        lifecycleScope.launch {
                            val ok = viewModel.undoLastImport()
                            toast(
                                if (ok) {
                                    getString(R.string.settings_undo_import_done)
                                } else {
                                    getString(R.string.settings_undo_import_none)
                                }
                            )
                        }
                    },
                    onDeleteConfirm = {
                        lifecycleScope.launch {
                            viewModel.clearWorkouts()
                            toast(getString(R.string.settings_data_deleted))
                            startActivity(Intent(this@SettingsActivity, JournalActivity::class.java))
                        }
                    },
                    onSourceCodeClick = {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/READYPLAYERAI/GainsGoblin"),
                            ),
                        )
                    },
                    onLicenceClick = {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.gnu.org/licenses/gpl-3.0.en.html"),
                            ),
                        )
                    },
                )
            }
        }
    }

    private fun handleImportMode(mode: ImportMode) {
        val pending = pendingImport.value ?: return
        pendingImport.value = null
        lifecycleScope.launch {
            try {
                val result = viewModel.importBackup(pending.first, mode)
                toast(getString(R.string.settings_import_success, result.setCount))
            } catch (e: Exception) {
                toast(getString(R.string.settings_import_failed, e.message ?: e.toString()))
            }
        }
    }

    private fun defaultBackupFileName(): String =
        "gainsgoblin_" +
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date()) +
            ".json"

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
