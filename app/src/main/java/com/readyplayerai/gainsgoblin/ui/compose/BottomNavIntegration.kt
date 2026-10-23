package com.readyplayerai.gainsgoblin.ui.compose

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.ExercisesActivity
import com.readyplayerai.gainsgoblin.ui.MainActivity
import com.readyplayerai.gainsgoblin.ui.ProgressActivity
import com.readyplayerai.gainsgoblin.ui.SettingsActivity

/**
 * Installs the shared M3 Expressive bottom navigation bar into an activity's
 * layout. The layout must contain a ComposeView with id
 * [R.id.bottom_navigation_view].
 *
 * Tapping the already-selected tab is a no-op, except for HOME where
 * [onHomeTabReselect] is invoked so the host can scroll the feed back to today.
 */
fun AppCompatActivity.setupBottomNavBar(
    currentTab: BottomNavTab,
    onHomeTabReselect: () -> Unit = {},
) {
    val activity = this
    findViewById<ComposeView>(R.id.bottom_navigation_view).setContent {
        GainsGoblinTheme {
            GainsGoblinBottomBar(selectedTab = currentTab) { tab ->
                if (tab != currentTab) {
                    activity.navigateToTab(tab)
                } else if (tab == BottomNavTab.HOME) {
                    onHomeTabReselect()
                }
            }
        }
    }
}

private fun AppCompatActivity.navigateToTab(tab: BottomNavTab) {
    val intent = when (tab) {
        BottomNavTab.HOME -> Intent(this, MainActivity::class.java)
        BottomNavTab.EXERCISES -> Intent(this, ExercisesActivity::class.java)
        BottomNavTab.PROGRESS -> Intent(this, ProgressActivity::class.java)
        BottomNavTab.SETTINGS -> Intent(this, SettingsActivity::class.java)
    }
    startActivity(intent)
    overridePendingTransition(0, 0)
}
