package com.readyplayerai.gainsgoblin.ui.compose.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinBottomBar
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Single-Activity Compose shell: a persistent bottom bar plus the four
 * top-level destinations. Tab switches preserve each destination's state.
 */
@Composable
fun GainsGoblinApp() {
    val navController = rememberNavController()
    // Incremented when JOURNAL is reselected; the journal feed scrolls to today.
    val journalScrollToTopTick = remember { MutableStateFlow(0) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = tabForRoute(currentRoute)

    Scaffold(
        // The screens own their own Scaffold and status-bar padding; only the
        // bottom bar height should be subtracted from the NavHost content.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute == null || currentTab != null) {
                GainsGoblinBottomBar(selectedTab = currentTab ?: BottomNavTab.JOURNAL) { tab ->
                    if (tab == currentTab) {
                        if (tab == BottomNavTab.JOURNAL) journalScrollToTopTick.value++
                    } else {
                        navController.navigate(routeFor(tab)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = JOURNAL_ROUTE,
            // The shell pays for the bottom bar + system bar insets here, so consume
            // them: nested screen Scaffolds must not apply the same insets again
            // (that double-count pushed the Journal FAB 48dp too high).
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(JOURNAL_ROUTE) { JournalRoute(scrollToTopTick = journalScrollToTopTick) }
            composable(EXERCISES_ROUTE) {
                ExercisesRoute(
                    onExerciseClick = { name ->
                        navController.navigate(exerciseProgressRoute(name))
                    },
                )
            }
            composable(PROGRESS_ROUTE) {
                ProgressRoute(
                    onOpenExercise = { name -> navController.navigate(exerciseProgressRoute(name)) },
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsRoute(
                    onDataDeleted = {
                        navController.navigate(JOURNAL_ROUTE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(
                route = EXERCISE_PROGRESS_ROUTE,
                arguments = listOf(
                    navArgument("exercise") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                ExerciseProgressRoute(
                    onBack = { navController.popBackStack() },
                    onOpenExercise = { newName ->
                        navController.navigate(exerciseProgressRoute(newName)) {
                            popUpTo(EXERCISE_PROGRESS_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
        }
    }
}
