package com.readyplayerai.gainsgoblin.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme
import com.readyplayerai.gainsgoblin.ui.compose.progress.ProgressScreen
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar
import dagger.hilt.android.AndroidEntryPoint

/** Hosts the Progress screen. */
@AndroidEntryPoint
class ProgressActivity : AppCompatActivity() {

    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_progress)

        setupBottomNavBar(BottomNavTab.PROGRESS)

        findViewById<ComposeView>(R.id.progress_view).setContent {
            GainsGoblinTheme {
                val state by viewModel.uiState.collectAsState()
                ProgressScreen(uiState = state)
            }
        }
    }
}
