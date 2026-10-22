package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
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
 * The Home screen: a loading gate and a TikTok-style full-screen day pager, newest-first,
 * with today as page 0 and the past flowing down. Each day is one page whose content scrolls
 * vertically inside the page first; a day-boundary [androidx.compose.ui.input.nestedscroll.NestedScrollConnection]
 * adds friction to slow drags and lets fast drags/flings page freely (see
 * [rememberDayBoundaryPagingConnection]). The flexible app bar collapses from the current
 * page's inner scroll only, so it stays expanded across page swipes. A day carousel strip sits
 * under the flexible app bar, synced to the current page.
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

    val pagerState = rememberPagerState(initialPage = 0) { uiState.infiniteDays.size }
    val scope = rememberCoroutineScope()
    val pageScrolls = remember { mutableStateMapOf<Int, ScrollState>() }
    val density = LocalDensity.current
    // Positive magnitude of bar travel: app bar 152dp -> 64dp + strip 66dp -> 0.
    val barTravelPx = with(density) { ((152.dp - 64.dp) + DayCarouselStripHeight).toPx() }

    var showDatePicker by remember { mutableStateOf(false) }

    // The day currently at the top of the pager (index 0 = today).
    val topDayIndex = pagerState.currentPage.coerceIn(0, uiState.infiniteDays.lastIndex)
    val topDay: WorkoutDay = uiState.infiniteDays[topDayIndex]

    // Manual collapse: only the current page's inner scroll drives the bar, so page swipes
    // never move it.
    val collapseFraction by remember(pageScrolls, pagerState) {
        derivedStateOf {
            val st = pageScrolls[pagerState.currentPage]
            if (st == null || barTravelPx <= 0f) {
                0f
            } else {
                (st.value / barTravelPx).coerceIn(0f, 1f)
            }
        }
    }

    // Explicit arrivals (strip pill taps, picker confirm, HOME reselect) land expanded and
    // exactly on the day.
    fun scrollToDay(index: Int) {
        scope.launch {
            pagerState.animateScrollToPage(index)
            pageScrolls[index]?.scrollTo(0) // expand the bar on arrival
        }
    }

    // Notify the host (MainActivity persists selectedDate) whenever the top day changes.
    LaunchedEffect(pagerState, uiState.infiniteDays) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { index ->
                uiState.infiniteDays.getOrNull(index)?.Date?.let(onSelectDate)
            }
    }

    // Bottom-nav HOME reselect hook (null = disabled).
    LaunchedEffect(scrollToTopTick) {
        scrollToTopTick?.collect { tick -> if (tick > 0) scrollToDay(0) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                HomeAppBar(
                    // While collapsed the date subtitle is gone, so the title carries the date
                    // ("Tuesday, Oct 20, 2026"); expanded keeps the big weekday-only title.
                    weekdayTitle = if (collapseFraction >= 0.5f) {
                        weekdayPlusShortDateOf(topDay.Date)
                    } else {
                        weekdayOf(topDay.Date)
                    },
                    fullDate = fullDateOf(topDay.Date),
                    collapseFraction = collapseFraction,
                    onDateIconClick = { showDatePicker = true },
                )
                DayCarouselStrip(
                    days = uiState.infiniteDays,
                    // Descending window: today is index 0.
                    todayIndex = 0,
                    activeIndex = topDayIndex,
                    collapseFraction = collapseFraction,
                    pagerState = pagerState,
                    onPillTap = { index -> scrollToDay(index) },
                    pageScrolls = pageScrolls,
                )
            }
        },
    ) { innerPadding ->
        val topInset = innerPadding.calculateTopPadding()
        VerticalPager(
            state = pagerState,
            userScrollEnabled = false,
            pageNestedScrollConnection = NoOpNestedScrollConnection,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val day = uiState.infiniteDays.getOrNull(page) ?: return@VerticalPager
            val realDay = uiState.realDays.firstOrNull { it.Date == day.Date }
            val st = rememberScrollState()
            pageScrolls[page] = st
            // Arrival is composition: every (re)composed page starts at its top, so any day
            // change (fling, pill tap, strip scrub, picker, HOME) lands top-of-day; a page that
            // never disposes (tease-abort back onto it) keeps its place.
            LaunchedEffect(page) { st.scrollTo(0) }
            val conn = rememberDayBoundaryPagingConnection(
                pagerState = pagerState,
                childScroll = st,
                pageCount = uiState.infiniteDays.size,
                scope = scope,
                pageScrolls = pageScrolls,
            )
            val isRestDay = realDay?.Exercises.isNullOrEmpty()
            val topInsetPx = with(density) { topInset.toPx() }
            var viewportHeightPx by remember { mutableStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportHeightPx = it.height }
                    .nestedScroll(conn)
                    .verticalScroll(st),
            ) {
                if (isRestDay) {
                    // Full-viewport Box centers the rest card; the 1dp tail spacer below keeps
                    // the page's scrollable in charge of the gesture.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topInset)
                            .height(
                                with(density) {
                                    (viewportHeightPx - topInsetPx).coerceAtLeast(0f).toDp()
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        HomeDaySection(
                            day = day,
                            realDay = realDay,
                            categoryFor = categoryFor,
                            onExerciseClick = onExerciseClick,
                        )
                    }
                } else {
                    Spacer(Modifier.height(topInset + 8.dp))
                    HomeDaySection(
                        day = day,
                        realDay = realDay,
                        categoryFor = categoryFor,
                        onExerciseClick = onExerciseClick,
                    )
                    Spacer(Modifier.height(24.dp))
                }
                Spacer(Modifier.height(1.dp))
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
