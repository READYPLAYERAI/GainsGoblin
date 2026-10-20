package com.readyplayerai.gainsgoblin.ui.compose

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.ChartsActivity
import com.readyplayerai.gainsgoblin.ui.DiaryActivity
import com.readyplayerai.gainsgoblin.ui.ExercisesActivity
import com.readyplayerai.gainsgoblin.ui.MainActivity
import com.readyplayerai.gainsgoblin.ui.SettingsActivity

/**
 * Installs the shared M3 Expressive bottom navigation bar into an activity's
 * layout. The layout must contain a ComposeView with id
 * [R.id.bottom_navigation_view].
 *
 * Tapping the already-selected tab is a no-op. [diaryDateProvider] is only
 * read at click time and its result is passed to DiaryActivity as the "date"
 * intent extra (used when navigating from Home).
 */
fun AppCompatActivity.setupBottomNavBar(
    currentTab: BottomNavTab,
    diaryDateProvider: () -> String? = { null },
) {
    val activity = this
    findViewById<ComposeView>(R.id.bottom_navigation_view).setContent {
        GainsGoblinTheme {
            GainsGoblinBottomBar(selectedTab = currentTab) { tab ->
                if (tab != currentTab) {
                    activity.navigateToTab(tab, diaryDateProvider())
                }
            }
        }
    }
}

private fun AppCompatActivity.navigateToTab(tab: BottomNavTab, diaryDate: String?) {
    val intent = when (tab) {
        BottomNavTab.DIARY -> Intent(this, DiaryActivity::class.java)
        BottomNavTab.EXERCISES -> Intent(this, ExercisesActivity::class.java)
        BottomNavTab.HOME -> Intent(this, MainActivity::class.java)
        BottomNavTab.CHARTS -> Intent(this, ChartsActivity::class.java)
        BottomNavTab.SETTINGS -> Intent(this, SettingsActivity::class.java)
    }
    if (tab == BottomNavTab.DIARY && diaryDate != null) {
        intent.putExtra("date", diaryDate)
    }
    startActivity(intent)
    overridePendingTransition(0, 0)
}
