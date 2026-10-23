package com.readyplayerai.gainsgoblin.ui.compose.exercises

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseStats
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.ui.ExercisesUiState
import com.readyplayerai.gainsgoblin.ui.compose.CategoryIcon
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar
import kotlinx.coroutines.launch

/**
 * Design D: search-first exercise library. A flexible header collapses
 * 120dp -> 64dp, the search field and body-part chips stay pinned below it, and
 * the list below carries favourite/star controls, contextual metadata, a sort
 * sheet and contextual empty states.
 *
 * The theme is provided by the caller (the hosting Activity wraps this in
 * [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    uiState: ExercisesUiState,
    onExerciseClick: (String) -> Unit,
    onAddExercise: (name: String, category: String, onResult: (Boolean) -> Unit) -> Unit,
    onToggleFavorite: (name: String, favorite: Boolean) -> Unit,
    onEditExercise: (oldName: String, newName: String, newCategory: String?) -> Unit,
    onDeleteExercise: (Exercise) -> Unit,
    onRestoreExercise: (Exercise) -> Unit,
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
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(ExerciseSortMode.AZ) }
    var sheetExerciseName by remember { mutableStateOf<String?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val categories = stringArrayResource(R.array.Categories)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    val density = LocalDensity.current

    val expandedHeader = 120.dp
    val collapsedHeader = 64.dp

    SideEffect {
        scrollBehavior.state.heightOffsetLimit = with(density) { -(expandedHeader - collapsedHeader).toPx() }
    }

    val sheetExercise = sheetExerciseName?.let { name: String ->
        uiState.knownExercises.firstOrNull { it.Name == name }
    }

    val normalizedQuery = query.trim()
    val stats = uiState.stats
    val favouritesCount = uiState.knownExercises.count { it.favorite == true }
    val visible = remember(
        uiState.knownExercises,
        stats,
        normalizedQuery,
        selectedCategory,
        favoritesOnly,
        sortMode,
    ) {
        uiState.knownExercises
            .filter { !favoritesOnly || it.favorite == true }
            .filter { selectedCategory == null || it.BodyPart.equals(selectedCategory, ignoreCase = true) }
            .filter {
                normalizedQuery.isBlank() ||
                    (it.Name ?: "").contains(normalizedQuery, ignoreCase = true) ||
                    (it.BodyPart ?: "").contains(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(
                when (sortMode) {
                    ExerciseSortMode.AZ -> compareBy<Exercise> { it.Name?.lowercase() ?: "" }
                    ExerciseSortMode.RECENT -> compareBy<Exercise> {
                        stats[it.Name]?.lastTrainedDaysAgo ?: Int.MAX_VALUE
                    }.thenBy { it.Name?.lowercase() ?: "" }

                    ExerciseSortMode.SETS -> compareByDescending<Exercise> {
                        stats[it.Name]?.totalSets ?: 0
                    }.thenBy { it.Name?.lowercase() ?: "" }
                },
            )
    }

    val subtitle = when {
        normalizedQuery.isNotBlank() -> when {
            favoritesOnly -> stringResource(
                R.string.exercises_subtitle_results_favourites, visible.size, normalizedQuery,
            )

            selectedCategory != null -> stringResource(
                R.string.exercises_subtitle_results_category,
                visible.size, normalizedQuery, selectedCategory.orEmpty(),
            )

            else -> stringResource(
                R.string.exercises_subtitle_results, visible.size, normalizedQuery,
            )
        }

        favoritesOnly -> stringResource(R.string.exercises_subtitle_favourites, visible.size)
        selectedCategory != null -> stringResource(
            R.string.exercises_subtitle_category, visible.size, selectedCategory.orEmpty(),
        )

        else -> stringResource(R.string.exercises_subtitle_default, visible.size)
    }

    val scrollToTop: () -> Unit = {
        scope.launch {
            if (listState.layoutInfo.totalItemsCount > 0) listState.animateScrollToItem(0)
        }
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
                expandedHeight = expandedHeader,
                collapsedHeight = collapsedHeader,
                expandedTitleSize = 34.sp,
                collapsedTitleSize = 22.sp,
                expandedBottomPadding = 16.dp,
                collapsedBottomPadding = 10.dp,
                actions = {
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier.size(48.dp),
                    ) {
                        BadgedBox(
                            badge = {
                                if (sortMode != ExerciseSortMode.AZ) Badge()
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sort_24px),
                                contentDescription = stringResource(R.string.exercises_sort),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.size(48.dp),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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
                                    contentDescription = stringResource(R.string.exercises_search_clear),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    placeholder = { Text(stringResource(R.string.exercises_search_placeholder)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !favoritesOnly && selectedCategory == null,
                        onClick = {
                            selectedCategory = null
                            favoritesOnly = false
                            scrollToTop()
                        },
                        label = { Text(stringResource(R.string.exercises_filter_all)) },
                        leadingIcon = if (!favoritesOnly && selectedCategory == null) {
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
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = {
                            favoritesOnly = true
                            selectedCategory = null
                            scrollToTop()
                        },
                        label = {
                            Text(
                                stringResource(
                                    R.string.exercises_filter_favourites_count,
                                    favouritesCount,
                                ),
                            )
                        },
                        leadingIcon = if (favoritesOnly) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24px),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_star_outline_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        },
                    )
                    categories.forEach { category ->
                        val selected = !favoritesOnly &&
                            selectedCategory.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedCategory = category
                                favoritesOnly = false
                                scrollToTop()
                            },
                            label = { Text(category) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_24px),
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                {
                                    CategoryMarker(
                                        category = category,
                                        size = FilterChipDefaults.IconSize,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visible.isEmpty()) {
                    item(key = "empty") {
                        when {
                            normalizedQuery.isNotBlank() -> ExercisesEmptyState(
                                iconRes = R.drawable.ic_search_24px,
                                title = stringResource(
                                    R.string.exercises_no_matches_query_title,
                                    normalizedQuery,
                                ),
                                body = stringResource(R.string.exercises_no_matches_query_body),
                                primaryLabel = stringResource(R.string.exercises_clear_search),
                                onPrimary = {
                                    query = ""
                                    scrollToTop()
                                },
                                secondaryLabel = stringResource(R.string.exercises_show_all),
                                onSecondary = {
                                    query = ""
                                    selectedCategory = null
                                    favoritesOnly = false
                                    scrollToTop()
                                },
                            )

                            favoritesOnly -> ExercisesEmptyState(
                                iconRes = R.drawable.ic_star_fill0_wght400_grad0_opsz24,
                                title = stringResource(R.string.exercises_no_favourites_title),
                                body = stringResource(R.string.exercises_no_favourites_body),
                                primaryLabel = stringResource(R.string.exercises_browse_all),
                                onPrimary = {
                                    favoritesOnly = false
                                    scrollToTop()
                                },
                            )

                            selectedCategory != null -> ExercisesEmptyState(
                                iconRes = R.drawable.ic_dashboard_24px,
                                title = stringResource(
                                    R.string.exercises_no_category_title,
                                    selectedCategory.orEmpty(),
                                ),
                                body = stringResource(
                                    R.string.exercises_no_category_body,
                                    selectedCategory.orEmpty(),
                                ),
                                primaryLabel = stringResource(R.string.exercises_show_all),
                                onPrimary = {
                                    selectedCategory = null
                                    scrollToTop()
                                },
                                secondaryLabel = stringResource(R.string.exercises_new_exercise),
                                onSecondary = { showAddSheet = true },
                            )

                            else -> ExercisesEmptyState(
                                iconRes = R.drawable.ic_add_24px,
                                title = stringResource(R.string.exercises_empty_title),
                                body = stringResource(R.string.exercises_empty_body),
                                primaryLabel = stringResource(R.string.exercises_new_exercise),
                                onPrimary = { showAddSheet = true },
                            )
                        }
                    }
                } else {
                    items(
                        items = visible,
                        key = { it.Name ?: it.hashCode().toString() },
                    ) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            stats = stats[exercise.Name],
                            onClick = { onExerciseClick(exercise.Name.orEmpty()) },
                            onLongClick = { sheetExerciseName = exercise.Name },
                            onToggleFavorite = { favorite ->
                                onToggleFavorite(exercise.Name.orEmpty(), favorite)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(
                                            if (favorite) R.string.exercises_favourite_added
                                            else R.string.exercises_favourite_removed,
                                            exercise.Name.orEmpty(),
                                        ),
                                        actionLabel = context.getString(R.string.exercises_undo),
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onToggleFavorite(exercise.Name.orEmpty(), !favorite)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        NewExerciseSheet(
            onDismiss = { showAddSheet = false },
            onAdd = onAddExercise,
            onAdded = { addedName ->
                query = ""
                selectedCategory = null
                favoritesOnly = false
                scrollToTop()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.exercises_added, addedName),
                        duration = SnackbarDuration.Short,
                    )
                }
            },
        )
    }

    if (showSortSheet) {
        ExerciseSortSheet(
            selected = sortMode,
            onSelect = {
                sortMode = it
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }

    sheetExercise?.let { exercise ->
        ExerciseActionSheet(
            exercise = exercise,
            onLogSets = {
                onExerciseClick(exercise.Name.orEmpty())
                sheetExerciseName = null
            },
            onToggleFavorite = { favorite ->
                onToggleFavorite(exercise.Name.orEmpty(), favorite)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(
                            if (favorite) R.string.exercises_favourite_added
                            else R.string.exercises_favourite_removed,
                            exercise.Name.orEmpty(),
                        ),
                        actionLabel = context.getString(R.string.exercises_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onToggleFavorite(exercise.Name.orEmpty(), !favorite)
                    }
                }
            },
            onDismiss = { sheetExerciseName = null },
            onRename = onEditExercise,
            onChangeCategory = onEditExercise,
            onDelete = {
                onDeleteExercise(exercise)
                sheetExerciseName = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.exercises_deleted,
                            exercise.Name.orEmpty(),
                        ),
                        actionLabel = context.getString(R.string.exercises_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onRestoreExercise(exercise)
                    }
                }
            },
        )
    }
}

/**
 * M3 Expressive segmented list item: 16dp rounded container on
 * `surfaceContainerHighest`, 72dp min height, leading category avatar, two-line
 * text (name + contextual metadata), trailing favourite star and "more" button.
 * Long-press and the more button share [onLongClick].
 */
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    stats: ExerciseStats?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
) {
    val favorite = exercise.favorite == true
    val name = exercise.Name.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
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
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exerciseMeta(exercise, stats),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onToggleFavorite(!favorite) }) {
                Icon(
                    painter = painterResource(
                        if (favorite) R.drawable.ic_star_fill0_wght400_grad0_opsz48
                        else R.drawable.ic_star_outline_24px,
                    ),
                    contentDescription = stringResource(
                        if (favorite) R.string.exercises_cd_favourite_remove
                        else R.string.exercises_cd_favourite_add,
                        name,
                    ),
                    modifier = Modifier.size(24.dp),
                    tint = if (favorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = { onLongClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert_24px),
                    contentDescription = stringResource(R.string.exercises_cd_more_actions, name),
                )
            }
        }
    }
}

/**
 * Contextual support text for a row: `Body part · last trained · sets`, or
 * `Body part · not logged yet` when there is no history.
 */
@Composable
private fun exerciseMeta(exercise: Exercise, stats: ExerciseStats?): String {
    val category = exercise.BodyPart?.takeIf { it.isNotBlank() } ?: "Other"
    val days = stats?.lastTrainedDaysAgo
    if (stats == null || days == null) {
        return stringResource(R.string.exercises_row_no_history, category)
    }
    val last = when {
        days == 0 -> stringResource(R.string.exercises_last_today)
        days == 1 -> stringResource(R.string.exercises_last_yesterday)
        days < 7 -> stringResource(R.string.exercises_last_days, days)
        else -> stringResource(R.string.exercises_last_weeks, days / 7)
    }
    val sets = pluralStringResource(R.plurals.exercises_sets, stats.totalSets, stats.totalSets)
    return stringResource(R.string.exercises_row_meta, category, last, sets)
}

/** Contextual empty state with an optional secondary action. */
@Composable
private fun ExercisesEmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onPrimary) {
                Text(primaryLabel)
            }
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}
