package com.readyplayerai.gainsgoblin.ui.compose.navigation

import android.net.Uri
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab

const val JOURNAL_ROUTE = "journal"
const val EXERCISES_ROUTE = "exercises"
const val PROGRESS_ROUTE = "progress"
const val SETTINGS_ROUTE = "settings"
const val ADD_EXERCISE_ROUTE = "add_exercise?exercise={exercise}"

/** Builds the AddExercise route with the exercise name URL-encoded. */
fun addExerciseRoute(name: String): String = "add_exercise?exercise=${Uri.encode(name)}"

/** Maps a bottom-bar tab to its NavHost route. */
fun routeFor(tab: BottomNavTab): String = when (tab) {
    BottomNavTab.JOURNAL -> JOURNAL_ROUTE
    BottomNavTab.EXERCISES -> EXERCISES_ROUTE
    BottomNavTab.PROGRESS -> PROGRESS_ROUTE
    BottomNavTab.SETTINGS -> SETTINGS_ROUTE
}

/** Maps a NavHost route back to its bottom-bar tab, or null for non-top-level routes. */
fun tabForRoute(route: String?): BottomNavTab? = when (route) {
    JOURNAL_ROUTE -> BottomNavTab.JOURNAL
    EXERCISES_ROUTE -> BottomNavTab.EXERCISES
    PROGRESS_ROUTE -> BottomNavTab.PROGRESS
    SETTINGS_ROUTE -> BottomNavTab.SETTINGS
    else -> null
}
