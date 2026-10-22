package com.readyplayerai.gainsgoblin.ui.compose.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.ui.ExercisesUiState
import com.readyplayerai.gainsgoblin.ui.compose.CategoryIcon
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar
import kotlinx.coroutines.launch

/**
 * Design B: collapsing medium top app bar (static "Exercises" title, filter
 * state in the subtitle), a docked search bar + single-select filter chips that
 * scroll away as list-header items, an A–Z flat list of two-line expressive
 * rows, and an empty state.
 *
 * The theme is provided by the caller (the hosting Activity wraps this in
 * [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    uiState: ExercisesUiState,
    onExerciseClick: (String) -> Unit,
    onAddExercise: () -> Unit,
    onEditExercise: (oldName: String, newName: String, newCategory: String?) -> Unit,
    onDeleteExercise: (String) -> Unit,
) {
    if (!uiState.loaded) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    // Step-3 hook: the action sheet will be rendered when this is non-null.
    var sheetExercise by remember { mutableStateOf<Exercise?>(null) }

    val categories = stringArrayResource(R.array.Categories)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    // The bar collapses 152dp -> 64dp; give the scroll behavior the same 88dp
    // travel that MediumTopAppBar used to set internally.
    val density = LocalDensity.current
    SideEffect {
        val limitPx = with(density) { -(152.dp - 64.dp).toPx() }
        if (scrollBehavior.state.heightOffsetLimit != limitPx) {
            scrollBehavior.state.heightOffsetLimit = limitPx
        }
    }

    val activeCategory = selectedCategory
    val categoryFiltered = uiState.knownExercises.filter {
        activeCategory == null || it.BodyPart.equals(activeCategory, ignoreCase = true)
    }
    val visible = categoryFiltered
        .filter { query.isBlank() || (it.Name ?: "").lowercase().contains(query.lowercase()) }
        .sortedBy { (it.Name ?: "").lowercase() }

    val subtitle = if (activeCategory == null) {
        stringResource(R.string.exercises_subtitle_az, categoryFiltered.size)
    } else {
        stringResource(R.string.exercises_subtitle_category, categoryFiltered.size, activeCategory)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlexibleHeaderBar(
                title = stringResource(R.string.nav_exercises),
                subtitle = subtitle,
                collapseFraction = collapsedFraction,
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                                focusRequester.requestFocus()
                            }
                        },
                        enabled = collapsedFraction > 0.5f,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .alpha(collapsedFraction),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24px),
                            contentDescription = stringResource(R.string.top_bar_search),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = onAddExercise,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24px),
                            contentDescription = stringResource(R.string.top_bar_add_exercise),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "search") {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24px),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close_24px),
                                    contentDescription = stringResource(R.string.top_bar_close),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    placeholder = { Text(stringResource(R.string.exercises_search_placeholder)) },
                    singleLine = true,
                )
            }

            item(key = "chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(stringResource(R.string.exercises_filter_all)) },
                        leadingIcon = if (selectedCategory == null) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24px),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                    )
                    categories.forEach { category ->
                        val selected = selectedCategory.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_24px),
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            if (visible.isEmpty()) {
                item(key = "empty") {
                    ExercisesEmptyState()
                }
            } else {
                items(
                    items = visible,
                    key = { it.Name ?: it.hashCode().toString() },
                ) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        onClick = { exercise.Name?.let(onExerciseClick) },
                        onLongClick = { sheetExercise = exercise },
                    )
                }
            }
        }
    }

    sheetExercise?.let { exercise ->
        ExerciseActionSheet(
            exercise = exercise,
            onDismiss = { sheetExercise = null },
            onRename = onEditExercise,
            onChangeCategory = onEditExercise,
            onDelete = onDeleteExercise,
        )
    }
}

/**
 * M3 Expressive segmented list item: 16dp rounded container on
 * `surfaceContainerLow`, 72dp min height, leading category avatar, two-line
 * text, trailing 48dp "more" button. Long-press and the more button share
 * [onLongClick].
 */
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIcon(category = exercise.BodyPart)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = exercise.Name.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exercise.BodyPart.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onLongClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert_24px),
                    contentDescription = stringResource(
                        R.string.exercises_cd_more_actions,
                        exercise.Name.orEmpty(),
                    ),
                )
            }
        }
    }
}

/** Centered empty state shown when the active chip + query match no exercises. */
@Composable
private fun ExercisesEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.exercises_no_matches_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.exercises_no_matches_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
