package com.readyplayerai.gainsgoblin.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.readyplayerai.gainsgoblin.R

/** The four top-level screens, in bottom bar display order. */
enum class BottomNavTab { JOURNAL, EXERCISES, PROGRESS, SETTINGS }

private data class BottomNavItem(
    val tab: BottomNavTab,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

private val bottomNavItems = listOf(
    BottomNavItem(BottomNavTab.JOURNAL, R.string.nav_journal, R.drawable.ic_nav_journal),
    BottomNavItem(BottomNavTab.EXERCISES, R.string.nav_exercises, R.drawable.ic_nav_exercises),
    BottomNavItem(BottomNavTab.PROGRESS, R.string.nav_progress, R.drawable.ic_nav_charts),
    BottomNavItem(BottomNavTab.SETTINGS, R.string.nav_settings, R.drawable.ic_nav_settings),
)

/**
 * Material 3 Expressive short navigation bar (64dp container). Pill indicator
 * and springy motion come from [GainsGoblinTheme]'s expressive motion scheme.
 */
@Composable
fun GainsGoblinBottomBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    ShortNavigationBar(
        containerColor = surfaceContainer,
    ) {
        bottomNavItems.forEach { item ->
            ShortNavigationBarItem(
                selected = item.tab == selectedTab,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.labelRes),
                    )
                },
                label = { Text(stringResource(item.labelRes)) },
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedTextColorTopIconPosition = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }
}
