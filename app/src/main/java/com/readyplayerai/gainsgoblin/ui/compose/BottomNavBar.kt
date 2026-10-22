package com.readyplayerai.gainsgoblin.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R

/** The five top-level screens, in bottom bar display order. */
enum class BottomNavTab { DIARY, EXERCISES, HOME, CHARTS, SETTINGS }

private data class BottomNavItem(
    val tab: BottomNavTab,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

private val bottomNavItems = listOf(
    BottomNavItem(BottomNavTab.DIARY, R.string.nav_diary, R.drawable.ic_nav_diary),
    BottomNavItem(BottomNavTab.EXERCISES, R.string.nav_exercises, R.drawable.ic_nav_exercises),
    BottomNavItem(BottomNavTab.HOME, R.string.nav_home, R.drawable.ic_nav_home),
    BottomNavItem(BottomNavTab.CHARTS, R.string.nav_charts, R.drawable.ic_nav_charts),
    BottomNavItem(BottomNavTab.SETTINGS, R.string.nav_settings, R.drawable.ic_nav_settings),
)

/**
 * Material 3 Expressive bottom navigation bar. Pill indicator and springy
 * motion come from [GainsGoblinTheme]'s expressive motion scheme.
 */
@Composable
fun GainsGoblinBottomBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    NavigationBar(
        modifier = Modifier
            .drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                drawLine(
                    color = outlineVariant,
                    strokeWidth = stroke,
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                )
            },
        containerColor = surfaceContainer,
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = item.tab == selectedTab,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = stringResource(item.labelRes),
                    )
                },
                label = { Text(stringResource(item.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }
}
