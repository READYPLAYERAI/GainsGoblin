package com.readyplayerai.gainsgoblin.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme
import com.readyplayerai.gainsgoblin.ui.compose.navigation.GainsGoblinApp
import dagger.hilt.android.AndroidEntryPoint

/** Single host Activity for the whole Compose UI. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GainsGoblinTheme {
                GainsGoblinApp()
            }
        }
    }
}
