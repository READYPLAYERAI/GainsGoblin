package com.readyplayerai.gainsgoblin.ui.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.readyplayerai.gainsgoblin.R

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

/**
 * Top bar for the Exercises screen. Shows a search and an add action; tapping
 * search swaps the bar for a small bar containing a query text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onQueryChange: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    if (!searchActive) {
        GainsGoblinTopBar(title, scrollBehavior) {
            IconButton(onClick = { searchActive = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_24px),
                    contentDescription = stringResource(R.string.top_bar_search),
                )
            }
            IconButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_brush_24px),
                    contentDescription = stringResource(R.string.top_bar_add_exercise),
                )
            }
        }
    } else {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        query = ""
                        onQueryChange("")
                        searchActive = false
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24px),
                        contentDescription = stringResource(R.string.top_bar_close),
                    )
                }
            },
            title = {
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.top_bar_search)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            },
            actions = {
                IconButton(onClick = onAddClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_brush_24px),
                        contentDescription = stringResource(R.string.top_bar_add_exercise),
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
}
