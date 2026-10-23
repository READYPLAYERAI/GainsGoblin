package com.readyplayerai.gainsgoblin.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme
import com.readyplayerai.gainsgoblin.ui.compose.settings.SettingsScreen
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.settings_activity)

        setupBottomNavBar(BottomNavTab.SETTINGS)

        val activity = this
        findViewById<ComposeView>(R.id.settings_view).setContent {
            GainsGoblinTheme {
                SettingsScreen(
                    onImportConfirm = {
                        startActivity(
                            Intent(activity, JournalActivity::class.java)
                                .putExtra("doit", "importcsv"),
                        )
                    },
                    onExportClick = {
                        startActivity(
                            Intent(activity, JournalActivity::class.java)
                                .putExtra("doit", "exportcsv"),
                        )
                    },
                    onDeleteConfirm = {
                        lifecycleScope.launch {
                            viewModel.clearWorkouts()
                            Toast.makeText(
                                activity,
                                R.string.settings_data_deleted,
                                Toast.LENGTH_SHORT,
                            ).show()
                            startActivity(Intent(activity, JournalActivity::class.java))
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
}