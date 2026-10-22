package com.readyplayerai.gainsgoblin.ui.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

/**
 * Material 3 Expressive medium top app bar with no navigation icon. When
 * [scrollBehavior] is null the bar stays expanded; screens can wire in a
 * scroll behavior at Compose migration time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GainsGoblinTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumTopAppBar(
        title = { Text(title) },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}
