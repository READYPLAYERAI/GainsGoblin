package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.HomeUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The Home screen: one continuous vertical journal, newest-first, with today as item 0 and
 * the past flowing down. Each day is a sticky header followed by its exercise cards; today's
 * rest day renders a single compact rest card while past rest days render only their header.
 * A shared FlexibleHeaderBar collapses as the list scrolls and shows the active day's month.
 *
 * The theme is expected to be provided by the caller (integration functions
 * wrap [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    categoryFor: (String?) -> String?,
    onExerciseClick: (exerciseName: String?, date: String?) -> Unit,
    onSelectDate: (date: String) -> Unit,
    scrollToTopTick: StateFlow<Int>? = null,
) {
    if (!uiState.loaded || uiState.infiniteDays.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator(modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.home_loading),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    SideEffect {
        val limitPx = with(density) { -(152.dp - 64.dp).toPx() }
        if (scrollBehavior.state.heightOffsetLimit != limitPx) {
            scrollBehavior.state.heightOffsetLimit = limitPx
        }
    }
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }

    val realDaysByDate = remember(uiState.realDays) {
        uiState.realDays.associateBy { it.Date }
    }
    val prByDate = remember(uiState.realDays) {
        uiState.realDays.associate { it.Date to findHomePr(it.Exercises.orEmpty()) }
    }
    // Maps each day index to the index of its sticky header in the LazyColumn. A day adds one
    // item for its header plus one body item only for today when it is a rest day; training
    // days add one item per exercise.
    val dayStartIndices = remember(uiState.infiniteDays, uiState.realDays) {
        val indices = IntArray(uiState.infiniteDays.size)
        var itemIndex = 0
        uiState.infiniteDays.forEachIndexed { index, day ->
            indices[index] = itemIndex
            itemIndex += 1 // sticky day header
            val realExercises = realDaysByDate[day.Date]?.Exercises
            if (realExercises.isNullOrEmpty()) {
                if (index == 0) itemIndex += 1 // today's compact rest card
            } else {
                itemIndex += realExercises.size
            }
        }
        indices
    }

    // The active day is the last day whose header starts at or before the first visible item.
    val activeDayIndex by remember(dayStartIndices, uiState.infiniteDays) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            var lo = 0
            var hi = dayStartIndices.lastIndex
            var result = 0
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                if (dayStartIndices[mid] <= first) {
                    result = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            result.coerceIn(0, uiState.infiniteDays.lastIndex)
        }
    }
    val activeDay = uiState.infiniteDays.getOrNull(activeDayIndex)
    val monthLabel = remember(activeDay?.Date) { formatMonthYear(activeDay?.Date) }

    // Notify the host (MainActivity persists selectedDate) whenever the active day changes.
    LaunchedEffect(activeDayIndex, uiState.infiniteDays) {
        activeDay?.Date?.let(onSelectDate)
    }

    // Bottom-nav HOME reselect hook (null = disabled).
    LaunchedEffect(scrollToTopTick) {
        scrollToTopTick?.collect { tick ->
            if (tick > 0) listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            HomeAppBar(
                monthLabel = monthLabel,
                collapseFraction = scrollBehavior.state.collapsedFraction,
                onDateIconClick = { showDatePicker = true },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            uiState.infiniteDays.forEachIndexed { index, day ->
                val realDay = realDaysByDate[day.Date]
                val exercises = realDay?.Exercises.orEmpty()
                val keyDate = day.Date ?: index.toString()

                stickyHeader(key = "day-header-$keyDate") {
                    HomeDayHeader(
                        day = day,
                        realDay = realDay,
                        isToday = index == 0,
                    )
                }

                if (exercises.isEmpty()) {
                    if (index == 0) {
                        item(key = "rest-$keyDate") {
                            HomeRestDayCard(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                } else {
                    itemsIndexed(
                        items = exercises,
                        key = { exerciseIndex, exercise ->
                            "ex-$keyDate-$exerciseIndex-${exercise.Exercise}"
                        },
                    ) { exerciseIndex, exercise ->
                        HomeExerciseCard(
                            exercise = exercise,
                            day = day,
                            categoryFor = categoryFor,
                            onExerciseClick = onExerciseClick,
                            pr = prByDate[day.Date],
                            modifier = if (exerciseIndex == 0) {
                                Modifier.padding(top = 8.dp)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }

    // Date -> index into the (descending) feed for the picker.
    val dateToIndex = remember(uiState.infiniteDays) {
        val map = HashMap<String, Int>(uiState.infiniteDays.size)
        uiState.infiniteDays.forEachIndexed { index, day -> day.Date?.let { map[it] = index } }
        map
    }

    if (showDatePicker) {
        HomeDatePickerDialog(
            initialDateString = activeDay?.Date,
            // windowFirstDay/windowLastDay must be CHRONOLOGICAL (first <= last):
            // the feed is DESCENDING, so the oldest day is last(), today is first().
            windowFirstDay = uiState.infiniteDays.last().Date,
            windowLastDay = uiState.infiniteDays.first().Date,
            onConfirm = { date ->
                showDatePicker = false
                dateToIndex[date]?.let { dayIndex ->
                    scope.launch { listState.animateScrollToItem(dayStartIndices[dayIndex]) }
                }
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

private val utcDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/** Parses an app date string (yyyy-MM-dd) into UTC-midnight epoch millis, or null if unparseable. */
private fun dateStringToUtcMillis(date: String): Long? = try {
    utcDateFormatter.parse(date)?.time
} catch (e: ParseException) {
    null
}

/** Formats UTC-midnight epoch millis back into an app date string (yyyy-MM-dd). */
private fun utcMillisToDateString(millis: Long): String = utcDateFormatter.format(millis)

/**
 * The M3 modal date picker dialog (spec: m3.material.io date pickers),
 * restricted to the five-year past day window.
 */
@Composable
private fun HomeDatePickerDialog(
    initialDateString: String?,
    windowFirstDay: String?,
    windowLastDay: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val windowFirstMillis = windowFirstDay?.let(::dateStringToUtcMillis)
    val windowLastMillis = windowLastDay?.let(::dateStringToUtcMillis)
    val windowFirstYear = windowFirstDay?.take(4)?.toIntOrNull()
    val windowLastYear = windowLastDay?.take(4)?.toIntOrNull()

    val selectableDates = remember(windowFirstDay, windowLastDay) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val first = windowFirstMillis ?: return true
                val last = windowLastMillis ?: return true
                return utcTimeMillis in first..last
            }

            override fun isSelectableYear(year: Int): Boolean {
                val first = windowFirstYear ?: return true
                val last = windowLastYear ?: return true
                return year in first..last
            }
        }
    }

    val initialSelectedMillis = dateStringToUtcMillis(initialDateString ?: "")
    val state = if (windowFirstYear != null && windowLastYear != null) {
        rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis,
            yearRange = windowFirstYear..windowLastYear,
            selectableDates = selectableDates,
        )
    } else {
        rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis,
            selectableDates = selectableDates,
        )
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis
                        ?.let { onConfirm(utcMillisToDateString(it)) }
                        ?: onDismiss()
                },
            ) {
                Text(stringResource(R.string.home_date_picker_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.exercises_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}
