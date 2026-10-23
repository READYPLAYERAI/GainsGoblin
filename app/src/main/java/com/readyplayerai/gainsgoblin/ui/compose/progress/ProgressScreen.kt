package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.PeriodRange
import com.readyplayerai.gainsgoblin.domain.categoryFor
import com.readyplayerai.gainsgoblin.domain.exerciseRecords
import com.readyplayerai.gainsgoblin.domain.monthlyVolumeBuckets
import com.readyplayerai.gainsgoblin.domain.overloadDeltas
import com.readyplayerai.gainsgoblin.domain.periodStats
import com.readyplayerai.gainsgoblin.domain.todayDateString
import com.readyplayerai.gainsgoblin.domain.weeklyVolumeBuckets
import com.readyplayerai.gainsgoblin.ui.ProgressUiState
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar

/**
 * Progress screen: a large flexible header with a morphing search field, a hero
 * card that switches between the 1W / 1M / All ranges, and a Journal / Records /
 * Insights lens bar. Journal, Records and Insights are switched by a sticky
 * lens bar that also hosts the category filters.
 *
 * The theme is provided by the caller (the hosting Activity wraps this in
 * [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProgressScreen(
    uiState: ProgressUiState,
) {
    // Wired to the record sheet in a later step.
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

    var lens by remember { mutableStateOf(ProgressLens.JOURNAL) }
    var range by remember { mutableStateOf(PeriodRange.WEEK) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<String?>(null) }
    var favouritesOnly by remember { mutableStateOf(false) }
    var openRecordName by remember { mutableStateOf<String?>(null) }
    val categories = stringArrayResource(R.array.Categories).toList()

    val records = remember(uiState.days, uiState.knownExercises) {
        exerciseRecords(uiState.days, uiState.knownExercises)
    }
    val stats = remember(uiState.days, range) { periodStats(uiState.days, range) }
    val weekly = remember(uiState.days) { weeklyVolumeBuckets(uiState.days) }
    val monthly = remember(uiState.days) { monthlyVolumeBuckets(uiState.days) }
    val sparkValues = if (range == PeriodRange.ALL) {
        monthly.takeLast(12).map { it.value }
    } else {
        weekly.takeLast(10).map { it.value }
    }
    val recordCount = records.sumOf { record ->
        listOf(record.maxWeight, record.maxReps, record.bestSetVolume, record.estOneRep)
            .count { it > 0.0 }
    }
    val subtitle = stringResource(
        R.string.progress_subtitle,
        uiState.days.size,
        records.size,
        recordCount,
    )

    val overloads = remember(uiState.days) { overloadDeltas(uiState.days) }
    val categoryOf: (String?) -> String? = remember(uiState.knownExercises) {
        { name -> categoryFor(uiState.knownExercises, name) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    SideEffect {
        val limitPx = with(density) { -(152.dp - 64.dp).toPx() }
        if (scrollBehavior.state.heightOffsetLimit != limitPx) {
            scrollBehavior.state.heightOffsetLimit = limitPx
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                FlexibleHeaderBar(
                    title = stringResource(R.string.progress_title),
                    subtitle = subtitle,
                    collapseFraction = scrollBehavior.state.collapsedFraction,
                    actions = {
                        IconButton(
                            onClick = {
                                searchOpen = !searchOpen
                                if (!searchOpen) query = ""
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (searchOpen) {
                                        R.drawable.ic_close_24px
                                    } else {
                                        R.drawable.ic_search_24px
                                    }
                                ),
                                contentDescription = stringResource(
                                    if (searchOpen) {
                                        R.string.progress_search_clear
                                    } else {
                                        R.string.progress_search_open
                                    }
                                ),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
                SearchField(
                    open = searchOpen,
                    query = query,
                    onQueryChange = { query = it },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                ProgressHero(
                    range = range,
                    onRangeChange = { range = it },
                    stats = stats,
                    sparkValues = sparkValues,
                )
            }
            stickyHeader(key = "controls") {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ProgressLensBar(
                            selected = lens,
                            onSelect = { lens = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        if (lens != ProgressLens.INSIGHTS) {
                            CategoryChipRow(
                                categories = categories,
                                selected = category,
                                showFavourites = lens == ProgressLens.RECORDS,
                                favouritesOnly = favouritesOnly,
                                onToggleFavourites = { favouritesOnly = !favouritesOnly },
                                onSelect = { category = it },
                            )
                        }
                    }
                }
            }
            when (lens) {
                ProgressLens.JOURNAL -> journalLens(
                    days = uiState.days,
                    categoryOf = categoryOf,
                    query = query,
                    category = category,
                    overloads = overloads,
                    onExerciseClick = { name, _ -> openRecordName = name },
                )
                ProgressLens.RECORDS -> item(key = "records") {
                    RecordsLens(
                        records = records,
                        query = query,
                        category = category,
                        favouritesOnly = favouritesOnly,
                        onOpenRecord = { openRecordName = it },
                    )
                }
                ProgressLens.INSIGHTS -> item(key = "insights") {
                    InsightsLens(days = uiState.days, today = todayDateString())
                }
            }
        }
    }

    val openRecord = openRecordName?.let { name -> records.firstOrNull { it.name == name } }
    if (openRecord != null) {
        RecordSheet(record = openRecord, onDismiss = { openRecordName = null })
    }
}

/**
 * Morphing search field shown under the header while [open]. Renders an official
 * Material 3 filled [TextField] with its default shape and colors, a leading
 * search icon and a placeholder slot. Expands in with a fade and vertical
 * expansion, and requests focus each time it opens.
 */
@Composable
private fun SearchField(
    open: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(open) {
        if (open) {
            // The field may not be placed yet while the expansion animation starts.
            runCatching { focusRequester.requestFocus() }
        }
    }
    AnimatedVisibility(
        visible = open,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.progress_search_placeholder)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search_24px),
                    contentDescription = null,
                )
            },
            singleLine = true,
        )
    }
}

/** Horizontally scrollable body-part filter chips: "All" then one per category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipRow(
    categories: List<String>,
    selected: String?,
    showFavourites: Boolean,
    favouritesOnly: Boolean,
    onToggleFavourites: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(
                label = stringResource(R.string.progress_filter_all),
                category = null,
                selected = selected == null,
                onClick = { onSelect(null) },
            )
            categories.forEach { category ->
                CategoryChip(
                    label = category,
                    category = category,
                    selected = selected == category,
                    onClick = { onSelect(category) },
                )
            }
            if (showFavourites) {
                CategoryChip(
                    label = stringResource(R.string.records_favourites),
                    category = null,
                    selected = favouritesOnly,
                    onClick = onToggleFavourites,
                    favourites = true,
                )
            }
        }
    }
}

/** One official Material 3 filter chip: [category] is null for "All" (no marker), [favourites] shows a star. */
@Composable
private fun CategoryChip(
    label: String,
    category: String?,
    selected: Boolean,
    onClick: () -> Unit,
    favourites: Boolean = false,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = when {
            favourites -> {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_star_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            }
            category != null -> {
                { CategoryMarker(category = category, size = FilterChipDefaults.IconSize) }
            }
            else -> null
        },
    )
}
