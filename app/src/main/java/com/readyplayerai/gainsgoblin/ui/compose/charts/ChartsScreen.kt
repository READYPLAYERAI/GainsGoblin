package com.readyplayerai.gainsgoblin.ui.compose.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.ui.ChartsUiState
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import com.readyplayerai.gainsgoblin.ui.compose.FlexibleHeaderBar
import com.readyplayerai.gainsgoblin.ui.compose.categoryColor
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.HashMap
import java.util.Locale

/**
 * Charts screen: the shared collapsing [FlexibleHeaderBar] with a personal
 * records action, over a scrollable column of three chart cards (workouts per
 * year, sets per bodypart, and volume for the last five workouts).
 *
 * The theme is provided by the caller (the hosting Activity wraps this in
 * [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    uiState: ChartsUiState,
    onPersonalRecordsClick: () -> Unit,
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

    val validDays = remember(uiState.days) {
        uiState.days.filter { !it.Date.isNullOrBlank() && it.Date != "0000-00-00" }
    }

    val yearColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
    )

    val workoutCounts = remember(validDays) { countWorkoutsPerYear(validDays) }
    val workoutSlices = workoutCounts.mapIndexed { index, (year, count) ->
        PieSliceData(year, count.toFloat(), yearColors[index % yearColors.size])
    }

    val bodypartCounts = remember(validDays) { countSetsPerBodypart(validDays) }
    val bodypartSlices = bodypartCounts.map { (category, count) ->
        PieSliceData(category, count.toFloat(), categoryColor(category))
    }

    val volumeEntries = remember(validDays) { lastVolumes(validDays) }
        .map { (label, value) -> BarEntryData(label, value) }

    if (validDays.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pie_chart_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.charts_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        return
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
            FlexibleHeaderBar(
                title = stringResource(R.string.nav_charts),
                subtitle = stringResource(R.string.charts_subtitle, validDays.size),
                collapseFraction = scrollBehavior.state.collapsedFraction,
                actions = {
                    IconButton(
                        onClick = onPersonalRecordsClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_emoji_events_24px),
                            contentDescription = stringResource(R.string.top_bar_personal_records),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "workouts") {
                ChartCard(
                    title = stringResource(R.string.charts_title_workouts),
                    description = stringResource(R.string.charts_desc_workouts),
                ) {
                    PieChart(workoutSlices)
                    PieLegend(
                        slices = workoutSlices,
                        markerContent = { slice ->
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color),
                            )
                        },
                    )
                }
            }
            item(key = "bodyparts") {
                ChartCard(
                    title = stringResource(R.string.charts_title_bodyparts),
                    description = stringResource(R.string.charts_desc_bodyparts),
                ) {
                    PieChart(bodypartSlices)
                    PieLegend(
                        slices = bodypartSlices,
                        markerContent = { slice ->
                            CategoryMarker(category = slice.label, size = 10.dp)
                        },
                    )
                }
            }
            item(key = "volume") {
                ChartCard(
                    title = stringResource(R.string.charts_title_volume),
                    description = stringResource(R.string.charts_desc_volume),
                ) {
                    BarChart(volumeEntries)
                }
            }
        }
    }
}

/** Rounded card holding a chart title, description and chart content. */
@Composable
private fun ChartCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(2.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** Workout days counted per year, sorted ascending by year label. */
private fun countWorkoutsPerYear(days: List<WorkoutDay>): List<Pair<String, Int>> {
    val workoutsPerYear = HashMap<String, Int>()
    for (day in days) {
        val year = day.Date!!.substring(0, 4)
        workoutsPerYear[year] = (workoutsPerYear[year] ?: 0) + 1
    }
    return workoutsPerYear.map { (year, count) -> year to count }.sortedBy { it.first }
}

/** Sets counted per body-part category, value descending then label ascending. */
private fun countSetsPerBodypart(days: List<WorkoutDay>): List<Pair<String, Int>> {
    val setsPerBodypart = HashMap<String, Int>()
    for (day in days) {
        val sets = day.Sets ?: continue
        for (set in sets) {
            val category = set.category ?: continue
            setsPerBodypart[category] = (setsPerBodypart[category] ?: 0) + 1
        }
    }
    return setsPerBodypart.map { (category, count) -> category to count }
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
}

/** Total volume for the last five workout days by date, oldest first. */
private fun lastVolumes(days: List<WorkoutDay>): List<Pair<String, Float>> =
    days.sortedByDescending { it.Date }
        .take(5)
        .reversed()
        .map { day -> shortDate(day.Date) to (day.DayVolume ?: 0.0).toFloat() }

/** Formats a `yyyy-MM-dd` date as `dd.MM`, falling back to the raw string. */
private fun shortDate(date: String?): String {
    if (date.isNullOrBlank()) return ""
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        SimpleDateFormat("dd.MM", Locale.US).format(parsed!!)
    } catch (e: ParseException) {
        date
    }
}
