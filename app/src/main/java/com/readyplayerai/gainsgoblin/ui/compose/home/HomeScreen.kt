package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.ui.HomeUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The Home screen: a loading gate and a single continuous vertical journal feed,
 * newest-first, with today pinned at index 0 and the past flowing down. A flexible
 * app bar collapses via Material 3 nested scroll as the feed scrolls, and its
 * title/subtitle track the day currently at the top of the feed. A day carousel
 * strip sits under the flexible app bar, synced to the feed's top day (same
 * design as the old pager, now driving a vertical feed).
 *
 * The theme is expected to be provided by the caller (integration functions
 * wrap [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                CircularProgressIndicator()
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val density = LocalDensity.current
    // Collapse travel = app bar (152dp -> 64dp) + day carousel strip (66dp -> 0).
    val headerTravelPx =
        with(density) { -((152.dp - 64.dp) + DayCarouselStripHeight).toPx() }

    // True while a programmatic arrival flight runs (see scrollToDay); the SideEffect
    // below must not re-apply the travel limit mid-flight.
    var arrivalInFlight by remember { mutableStateOf(false) }

    SideEffect {
        if (!arrivalInFlight && behavior.state.heightOffsetLimit != headerTravelPx) {
            behavior.state.heightOffsetLimit = headerTravelPx
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    // The day currently at the top of the feed (index 0 = today).
    val topDayIndex by remember(uiState.infiniteDays) {
        derivedStateOf {
            listState.firstVisibleItemIndex.coerceIn(0, uiState.infiniteDays.lastIndex)
        }
    }
    val topDay: WorkoutDay by remember(uiState.infiniteDays) {
        derivedStateOf { uiState.infiniteDays[topDayIndex] }
    }

    // Explicit arrivals (Today button, strip pill taps, picker confirm, HOME reselect)
    // land expanded and exactly on the day — design rule 4 from the old pager. While the
    // flight runs the scroll behavior is frozen (limit 0 = nothing for nested scroll to
    // consume), so exitUntilCollapsed can't spend part of the flight collapsing the bar
    // and short-landing the arrival.
    fun scrollToDay(index: Int) {
        scope.launch {
            arrivalInFlight = true
            try {
                behavior.state.heightOffsetLimit = 0f
                behavior.state.heightOffset = 0f
                listState.animateScrollToItem(index)
            } finally {
                arrivalInFlight = false
                behavior.state.heightOffsetLimit = headerTravelPx
            }
        }
    }

    // Notify the host (MainActivity persists selectedDate) whenever the top day changes.
    LaunchedEffect(listState, uiState.infiniteDays) {
        snapshotFlow { topDay.Date }
            .distinctUntilChanged()
            .collect { date -> date?.let(onSelectDate) }
    }

    // Bottom-nav HOME reselect hook (Step 3 wires the producer; null = disabled).
    LaunchedEffect(scrollToTopTick) {
        scrollToTopTick?.collect { tick -> if (tick > 0) scrollToDay(0) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(behavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                HomeAppBar(
                    // While collapsed the date subtitle is gone, so the title carries the date
                    // ("Tuesday, Oct 20, 2026"); expanded keeps the big weekday-only title.
                    weekdayTitle = if (behavior.state.collapsedFraction >= 0.5f) {
                        weekdayPlusShortDateOf(topDay.Date)
                    } else {
                        weekdayOf(topDay.Date)
                    },
                    fullDate = fullDateOf(topDay.Date),
                    collapseFraction = behavior.state.collapsedFraction,
                    onTodayClick = { scrollToDay(0) },
                    onDateIconClick = { showDatePicker = true },
                )
                DayCarouselStrip(
                    days = uiState.infiniteDays,
                    // Descending window: today is index 0.
                    todayIndex = 0,
                    activeIndex = topDayIndex,
                    collapseFraction = behavior.state.collapsedFraction,
                    feedListState = listState,
                    onPillTap = { index -> scrollToDay(index) },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            items(
                uiState.infiniteDays,
                key = { day -> day.Date ?: System.identityHashCode(day).toString() },
            ) { day ->
                HomeDaySection(
                    day = day,
                    realDay = uiState.realDays.firstOrNull { it.Date == day.Date },
                    categoryFor = categoryFor,
                    onExerciseClick = onExerciseClick,
                )
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
            initialDateString = topDay.Date,
            // windowFirstDay/windowLastDay must be CHRONOLOGICAL (first <= last):
            // the feed is DESCENDING, so the oldest day is last(), today is first().
            windowFirstDay = uiState.infiniteDays.last().Date,
            windowLastDay = uiState.infiniteDays.first().Date,
            onConfirm = { date ->
                showDatePicker = false
                dateToIndex[date]?.let(::scrollToDay)
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

private fun weekdayOf(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) SimpleDateFormat("EEEE", Locale.getDefault()).format(parsed) else ""
} catch (e: ParseException) {
    ""
}

private fun fullDateOf(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(parsed) else ""
} catch (e: ParseException) {
    ""
}

/** Formats a date as "Tuesday, Oct 20, 2026" (short month) for the collapsed bar title. */
private fun weekdayPlusShortDateOf(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) {
        SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(parsed)
    } else {
        ""
    }
} catch (e: ParseException) {
    ""
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