package com.readyplayerai.gainsgoblin.ui.compose

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.readyplayerai.gainsgoblin.R

/**
 * Installs a shared M3 Expressive top bar into an activity's layout. The
 * layout must contain a ComposeView with id [R.id.top_bar_view]. The [content]
 * is rendered inside [GainsGoblinTheme].
 */
fun AppCompatActivity.setupTopBar(content: @Composable () -> Unit) {
    findViewById<ComposeView>(R.id.top_bar_view).setContent {
        GainsGoblinTheme {
            content()
        }
    }
}
