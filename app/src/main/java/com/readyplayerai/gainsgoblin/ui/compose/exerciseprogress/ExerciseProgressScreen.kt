package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseMetric
import com.readyplayerai.gainsgoblin.domain.ExerciseRange
import com.readyplayerai.gainsgoblin.domain.exerciseProgress
import com.readyplayerai.gainsgoblin.domain.exerciseSessionsInRange
import com.readyplayerai.gainsgoblin.model.Exercise
import com.readyplayerai.gainsgoblin.ui.ExerciseProgressUiState
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import com.readyplayerai.gainsgoblin.ui.compose.exercises.ExerciseActionSheet
import com.readyplayerai.gainsgoblin.ui.compose.journal.LogSetSheet
import com.readyplayerai.gainsgoblin.ui.compose.progress.dayWithYear
import com.readyplayerai.gainsgoblin.ui.compose.progress.shortDate
import java.util.Date
import kotlinx.coroutines.launch

/**
 * Per-exercise progress detail: a compact app bar with back / log-set /
 * favourite / overflow actions, a large header and a hero summary, metric bar,
 * chart, records, trends and per-session history. The overflow opens the shared
 * action sheet for rename, change body part and delete.
 *
 * The exercise payload comes from [exerciseProgress], which returns `null` for an
 * unknown, never-trained name — treated as the empty state because the exercise
 * may have been deleted. The theme is provided by the hosting Activity.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseProgressScreen(
    uiState: ExerciseProgressUiState,
    onBack: () -> Unit,
    onAddSet: (weight: Double, reps: Double) -> Unit,
    onUpdateSet: (date: String, setIndex: Int, weight: Double, reps: Double) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onRename: (oldName: String, newName: String, newCategory: String?) -> Unit,
    onDelete: () -> Unit,
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

    val today = remember { Date() }
    var metric by rememberSaveable { mutableStateOf(ExerciseMetric.EST_ONE_REP) }
    var range by rememberSaveable { mutableStateOf(ExerciseRange.THREE_MONTHS) }
    var prOnly by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var expandedDates by rememberSaveable(
        stateSaver = listSaver<Set<String>, String>(
            save = { it.toList() },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(emptySet<String>()) }
    var showActions by remember { mutableStateOf(false) }
    var showLogSheet by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<SetEditTarget?>(null) }
    val progress = remember(uiState.days, uiState.knownExercises, uiState.exerciseName, today) {
        exerciseProgress(
            uiState.days,
            uiState.knownExercises,
            uiState.exerciseName,
            today,
        )
    }
    val sessions = remember(progress, range, today) {
        progress?.let { exerciseSessionsInRange(it.sessions, range, today) }.orEmpty()
    }
    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 72
        }
    }
    val titleAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        label = "exerciseProgressTitleAlpha",
    )
    val chartRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    val sessionsCount = progress?.sessions?.size ?: 0
    val totalSets = progress?.totalSets ?: 0

    Scaffold(
        topBar = {
            ExerciseProgressTopBar(
                title = progress?.name ?: uiState.exerciseName,
                favorite = progress?.favorite == true,
                titleAlpha = titleAlpha,
                onBack = onBack,
                onLogSets = { showLogSheet = true },
                onToggleFavorite = { onToggleFavorite(!(progress?.favorite ?: false)) },
                onMore = { showActions = true },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item(key = "header") {
                ExerciseProgressHeader(
                    name = progress?.name ?: uiState.exerciseName,
                    category = progress?.category.orEmpty(),
                    sessionsCount = sessionsCount,
                    lastDate = progress?.lastDate,
                    totalSets = totalSets,
                )
            }
            if (progress == null || progress.sessions.isEmpty()) {
                item(key = "empty") {
                    ExerciseProgressEmptyState(
                        name = uiState.exerciseName,
                        onLogSets = { showLogSheet = true },
                    )
                }
            } else {
                item(key = "hero") {
                    ExerciseProgressHero(
                        progress = progress,
                        sessions = sessions,
                        metric = metric,
                        range = range,
                        onRangeChange = { range = it },
                    )
                }
                stickyHeader(key = "metric_bar") {
                    ExerciseProgressMetricBar(
                        metric = metric,
                        onMetricChange = { metric = it },
                    )
                }
                item(key = "chart") {
                    ExerciseProgressChartCard(
                        sessions = sessions,
                        metric = metric,
                        range = range,
                        modifier = Modifier.bringIntoViewRequester(chartRequester),
                    )
                }
                item(key = "records_header") {
                    ExerciseProgressSectionHeader(
                        title = stringResource(R.string.exercise_progress_section_records),
                        meta = stringResource(R.string.exercise_progress_section_all_time),
                    )
                }
                item(key = "records") {
                    ExerciseRecordsRow(
                        records = progress.records,
                        onRecordClick = { selected ->
                            metric = selected
                            scope.launch { chartRequester.bringIntoView() }
                        },
                    )
                }
                item(key = "trends_header") {
                    ExerciseProgressSectionHeader(
                        title = stringResource(R.string.exercise_progress_section_trends),
                        meta = rangeDescription(range),
                    )
                }
                item(key = "trends") {
                    ExerciseTrends(
                        progress = progress,
                        sessions = sessions,
                        metric = metric,
                        range = range,
                    )
                }
                val history = exerciseHistoryRows(
                    sessions = progress.sessions,
                    prOnly = prOnly,
                    showAll = showAll,
                )
                item(key = "history_header") {
                    ExerciseProgressSectionHeader(
                        title = stringResource(R.string.exercise_progress_history_title),
                        meta = pluralStringResource(
                            R.plurals.exercise_progress_sessions,
                            progress.sessions.size,
                            progress.sessions.size,
                        ),
                    )
                }
                item(key = "history_filter") {
                    ExerciseHistoryFilterRow(prOnly = prOnly, onPrOnlyChange = { prOnly = it })
                }
                if (history.rows.isEmpty()) {
                    item(key = "history_empty") {
                        ExerciseHistoryEmptyState(onShowAll = { prOnly = false })
                    }
                } else {
                    history.rows.forEach { row ->
                        when (row) {
                            is ExerciseHistoryRow.Month -> item(key = "month-${row.label}") {
                                ExerciseHistoryMonth(
                                    label = row.label,
                                    sessionCount = row.sessionCount,
                                    volume = row.volume,
                                )
                            }

                            is ExerciseHistoryRow.Session -> item(key = "session-${row.session.date}") {
                                val date = row.session.date
                                SessionCard(
                                    session = row.session,
                                    expanded = date in expandedDates,
                                    onToggle = {
                                        expandedDates = if (date in expandedDates) {
                                            expandedDates - date
                                        } else {
                                            expandedDates + date
                                        }
                                    },
                                    onSetClick = { index ->
                                        row.session.sets.getOrNull(index)?.let { set ->
                                            editTarget = SetEditTarget(
                                                date = row.session.date,
                                                setIndex = index,
                                                weight = set.weight,
                                                reps = set.reps,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                if (history.total > 9) {
                    item(key = "history_show_all") {
                        ExerciseShowAllButton(
                            showAll = showAll,
                            total = history.total,
                            onToggle = { showAll = !showAll },
                        )
                    }
                }
            }
        }
    }

    val sheetExercise = remember(progress?.name, progress?.category, progress?.favorite) {
        progress?.let {
            Exercise(it.name, it.category.ifBlank { null }).also { exercise -> exercise.favorite = it.favorite }
        }
    }
    if (showActions && sheetExercise != null) {
        ExerciseActionSheet(
            exercise = sheetExercise,
            onLogSets = {
                showActions = false
                showLogSheet = true
            },
            onToggleFavorite = { favorite -> onToggleFavorite(favorite) },
            onDismiss = { showActions = false },
            onRename = onRename,
            onChangeCategory = onRename,
            onDelete = { onDelete() },
        )
    }

    val target = editTarget
    if (showLogSheet || target != null) {
        val lastEntry = progress?.sessions?.lastOrNull()?.sets?.lastOrNull()
        LogSetSheet(
            exerciseName = progress?.name ?: uiState.exerciseName,
            dateLabel = dayWithYear(target?.date ?: uiState.selectedDate),
            initialWeight = target?.weight ?: lastEntry?.weight,
            initialReps = target?.reps ?: lastEntry?.reps,
            editingSetIndex = target?.setIndex,
            onSave = { weight, reps ->
                if (target != null) {
                    onUpdateSet(target.date, target.setIndex, weight, reps)
                } else {
                    onAddSet(weight, reps)
                }
                showLogSheet = false
                editTarget = null
            },
            onDismiss = {
                showLogSheet = false
                editTarget = null
            },
        )
    }
}

/** Section header with a title on the start edge and a muted meta label on the end edge. */
@Composable
private fun ExerciseProgressSectionHeader(title: String, meta: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = meta,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Large header: category line, exercise name and last-trained meta row. */
@Composable
private fun ExerciseProgressHeader(
    name: String,
    category: String,
    sessionsCount: Int,
    lastDate: String?,
    totalSets: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (category.isNotBlank()) {
                CategoryMarker(category = category, size = 13.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = category.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.72.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sessionsCount > 0) {
                if (category.isNotBlank()) {
                    Spacer(Modifier.width(7.dp))
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.exercise_progress_sessions,
                        sessionsCount,
                        sessionsCount,
                    ).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.72.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = name,
            modifier = Modifier.padding(top = 7.dp),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 31.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight(680),
                letterSpacing = (-1).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (lastDate != null) {
                stringResource(R.string.exercise_progress_meta, shortDate(lastDate), totalSets)
            } else {
                stringResource(R.string.exercise_progress_no_history)
            },
            modifier = Modifier.padding(top = 7.dp),
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Centred empty state for an exercise with no logged sets yet. */
@Composable
private fun ExerciseProgressEmptyState(
    name: String,
    onLogSets: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = stringResource(R.string.exercise_progress_no_sets_title),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.exercise_progress_no_sets_body, name),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(
            onClick = onLogSets,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.exercise_progress_log_first_set))
        }
    }
}

/** Sticky metric selector bar: one filter chip per [ExerciseMetric]. */
@Composable
private fun ExerciseProgressMetricBar(
    metric: ExerciseMetric,
    onMetricChange: (ExerciseMetric) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ExerciseMetric.entries.forEach { m ->
                val selected = metric == m
                val palette = metricPalette(m)
                FilterChip(
                    selected = selected,
                    onClick = { onMetricChange(m) },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(13.dp),
                    label = {
                        Text(
                            text = metricLabel(m),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (selected) palette.onAccent else palette.accent),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent,
                        selectedLabelColor = palette.onAccent,
                    ),
                )
            }
        }
    }
}

private data class SetEditTarget(
    val date: String,
    val setIndex: Int,
    val weight: Double,
    val reps: Double,
)
