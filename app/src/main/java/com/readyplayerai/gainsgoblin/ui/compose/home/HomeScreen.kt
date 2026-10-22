package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.ui.HomeUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The Home screen scaffold: a loading gate, a ten-year day [HorizontalPager],
 * and a derived-day header + carousel strip + summary chips above the pages.
 *
 * The theme is expected to be provided by the caller (integration functions
 * wrap [com.readyplayerai.gainsgoblin.ui.compose.GainsGoblinTheme]).
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    categoryFor: (String?) -> String?,
    onExerciseClick: (exerciseName: String?, date: String?) -> Unit,
    onSelectDate: (date: String) -> Unit,
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

    val pagerState = rememberPagerState(
        initialPage = maxOf(0, uiState.todayPosition),
    ) { uiState.infiniteDays.size }

    val scope = rememberCoroutineScope()

    // Per-day collapse memory. Missing key == 0f == fully expanded.
    val collapse = remember { mutableStateMapOf<Int, Float>() }
    val travelPx = with(LocalDensity.current) { 220.dp.toPx() }

    // Modal M3 date picker dialog visibility.
    var showDatePicker by remember { mutableStateOf(false) }

    // Hoisted per-page list states so the parent-level raw gesture handler can
    // scroll the visible page's list programmatically.
    val pageListStates = remember { mutableStateMapOf<Int, LazyListState>() }

    // The 50% rule: while a swipe passes the midpoint, the UI shows the neighbour page.
    val derivedIndex by remember {
        derivedStateOf {
            val f = pagerState.currentPageOffsetFraction
            val dir = if (f > 0.5f) 1 else if (f < -0.5f) -1 else 0
            (pagerState.currentPage + dir).coerceIn(0, uiState.infiniteDays.lastIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            uiState.infiniteDays.getOrNull(page)?.Date?.let(onSelectDate)
        }
    }

    fun jumpTo(index: Int) {
        val target = index.coerceIn(0, uiState.infiniteDays.lastIndex)
        // Rule 4: explicit arrivals land expanded.
        collapse[target] = 0f
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    val derivedDay: WorkoutDay = uiState.infiniteDays[derivedIndex]
    val derivedRealDay: WorkoutDay? = uiState.realDays.firstOrNull { it.Date == derivedDay.Date }
    val derivedFraction = (collapse[derivedIndex] ?: 0f).coerceIn(0f, 1f)
    val barFraction = derivedFraction

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        HomeAppBar(
            weekdayTitle = weekdayOf(derivedDay.Date),
            fullDate = fullDateOf(derivedDay.Date),
            collapseFraction = barFraction,
            onTodayClick = {
                uiState.todayPosition.takeIf { it >= 0 }?.let(::jumpTo)
            },
            onDateIconClick = {
                showDatePicker = true
            },
        )

        DayCarouselStrip(
            days = uiState.infiniteDays,
            todayIndex = uiState.todayPosition,
            pagerState = pagerState,
            collapseFraction = derivedFraction,
            onPillTap = ::jumpTo,
        )

        SummaryChips(
            day = derivedDay,
            realDay = derivedRealDay,
            modifier = Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    if (barFraction >= 0.99f) {
                        layout(placeable.width, 0) {}
                    } else {
                        val height = (placeable.height * (1f - barFraction))
                            .roundToInt()
                            .coerceAtLeast(0)
                        layout(placeable.width, height) {
                            placeable.placeRelative(
                                x = 0,
                                y = (height - placeable.height).coerceAtMost(0),
                            )
                        }
                    }
                }
                .graphicsLayer { alpha = 1f - barFraction },
        )

        val touchSlopPx = LocalViewConfiguration.current.touchSlop
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .homeVerticalGestures(
                    travelPx = travelPx,
                    touchSlopPx = touchSlopPx,
                    collapse = collapse,
                    pagerState = pagerState,
                    pageListStates = pageListStates,
                    scope = scope,
                ),
        ) { page ->
            val listState = rememberLazyListState()
            LaunchedEffect(page) { pageListStates[page] = listState }
            val day = uiState.infiniteDays[page]
            HomeDayPage(
                day = day,
                realDay = uiState.realDays.firstOrNull { it.Date == day.Date },
                categoryFor = categoryFor,
                onExerciseClick = onExerciseClick,
                listState = listState,
            )
        }
    }

    val dateToIndex = remember(uiState.infiniteDays) {
        val map = HashMap<String, Int>(uiState.infiniteDays.size)
        uiState.infiniteDays.forEachIndexed { index, day -> day.Date?.let { map[it] = index } }
        map
    }

    if (showDatePicker) {
        HomeDatePickerDialog(
            initialDateString = uiState.infiniteDays.getOrNull(pagerState.currentPage)?.Date,
            windowFirstDay = uiState.infiniteDays.first().Date,
            windowLastDay = uiState.infiniteDays.last().Date,
            onConfirm = { date ->
                showDatePicker = false
                dateToIndex[date]?.let(::jumpTo)
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * Raw vertical gesture handling at the pager level. The pager keeps ownership
 * of horizontal paging; once a gesture locks vertical we consume moves and
 * drive the collapse fraction / page list ourselves. This avoids relying on
 * nested-scroll sign conventions entirely.
 */
private fun Modifier.homeVerticalGestures(
    travelPx: Float,
    touchSlopPx: Float,
    collapse: MutableMap<Int, Float>,
    pagerState: PagerState,
    pageListStates: Map<Int, LazyListState>,
    scope: CoroutineScope,
): Modifier = pointerInput(travelPx, touchSlopPx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastPosition = down.position
        var totalX = 0f
        var totalY = 0f
        var mode = 0 // 0 = undecided, 1 = vertical (ours), 2 = horizontal (pager's)
        var released = false
        val samples = ArrayDeque<Pair<Long, Float>>()

        fun applyDrag(dy: Float, time: Long) {
            val page = pagerState.currentPage
            val list = pageListStates[page]
            val fraction = (collapse[page] ?: 0f).coerceIn(0f, 1f)
            when {
                dy < 0f -> {
                    val upward = -dy
                    val needed = ((1f - fraction) * travelPx).coerceAtLeast(0f)
                    val toCollapse = min(upward, needed)
                    if (toCollapse > 0f) {
                        collapse[page] = (fraction + toCollapse / travelPx).coerceIn(0f, 1f)
                    }
                    val leftover = upward - toCollapse
                    if (leftover > 0f && list != null) {
                        scope.launch { list.scrollBy(leftover) }
                    }
                }

                dy > 0f -> {
                    if (list != null && list.canScrollBackward) {
                        scope.launch { list.scrollBy(-dy) }
                    } else if (fraction > 0f) {
                        collapse[page] = (fraction - dy / travelPx).coerceIn(0f, 1f)
                    }
                }
            }
        }

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.position - lastPosition
            lastPosition = change.position
            totalX += delta.x
            totalY += delta.y

            if (mode == 0) {
                if (abs(totalY) > abs(totalX) && abs(totalY) > touchSlopPx) {
                    mode = 1
                } else if (abs(totalX) > abs(totalY) && abs(totalX) > touchSlopPx) {
                    mode = 2
                }
            }

            if (mode == 1) {
                change.consume()
                if (delta.y != 0f) {
                    applyDrag(delta.y, change.uptimeMillis)
                    samples.addLast(change.uptimeMillis to delta.y)
                    while (samples.size > 5) samples.removeFirst()
                }
            }

            if (!change.pressed) {
                released = true
                break
            }
        }

        if (mode == 1 && released) {
            val page = pagerState.currentPage
            val fraction = (collapse[page] ?: 0f).coerceIn(0f, 1f)
            if (fraction > 0f && fraction < 1f) {
                val velocity = if (samples.size >= 2) {
                    val dt = samples.last().first - samples.first().first
                    if (dt > 0L) samples.sumOf { it.second.toDouble() }.toFloat() /
                        (dt / 1000f) else 0f
                } else {
                    0f
                }
                val target = when {
                    velocity < -600f -> 1f
                    velocity > 600f -> 0f
                    fraction >= 0.5f -> 1f
                    else -> 0f
                }
                scope.launch {
                    animate(
                        initialValue = fraction,
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) { value, _ ->
                        collapse[page] = value.coerceIn(0f, 1f)
                    }
                }
            }
        }
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
 * restricted to the ten-year day window.
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
