package com.readyplayerai.gainsgoblin.ui

import android.content.Intent
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
import com.readyplayerai.gainsgoblin.ui.compose.charts.ChartsScreen
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChartsActivity : AppCompatActivity() {

    private val viewModel: ChartsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_charts)

        setupBottomNavBar(BottomNavTab.CHARTS)

        val activity = this
        findViewById<ComposeView>(R.id.charts_view).setContent {
            GainsGoblinTheme {
                val state by viewModel.uiState.collectAsState()
                ChartsScreen(uiState = state) {
                    startActivity(Intent(activity, PersonalRecordsActivity::class.java))
                }
            }
        }
    }
}
