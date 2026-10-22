package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val TNUM = TextStyle(fontFeatureSettings = "tnum")

private val monthTagWidth: Dp = 38.dp
private val pillWidth: Dp = 48.dp
private val itemHeight: Dp = 46.dp
private val itemGap: Dp = 8.dp
private val stripContentPadding: Dp = 16.dp
private val stripHeight: Dp = 58.dp
private val stripWrapHeight: Dp = 66.dp
private val stripTopPadding: Dp = 8.dp

/** The strip's wrap height ([stripWrapHeight]) — its collapse travel in the Home top bar. */
internal val DayCarouselStripHeight: Dp = stripWrapHeight

private data class DayCell(
    val weekdayShort: String,
    val weekdayRaw: String,
    val monthShort: String,
    val monthKey: String,
    val dayOfMonth: String,
)

private fun buildDayCells(days: List<WorkoutDay>): List<DayCell> {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val weekday = SimpleDateFormat("EEE", Locale.getDefault())
    val weekdayLong = SimpleDateFormat("EEE", Locale.getDefault())
    val month = SimpleDateFormat("MMM", Locale.getDefault())
    val day = SimpleDateFormat("d", Locale.getDefault())
    return days.map { workoutDay ->
        val date: Date? = try {
            parser.parse(workoutDay.Date ?: "")
        } catch (e: Exception) {
            null
        }
        if (date == null) {
            DayCell("", "", "", workoutDay.Date?.take(7) ?: "", "")
        } else {
            DayCell(
                weekdayShort = weekday.format(date).uppercase(Locale.getDefault()),
                weekdayRaw = weekdayLong.format(date),
                monthShort = month.format(date).uppercase(Locale.getDefault()),
                monthKey = workoutDay.Date?.take(7) ?: "",
                dayOfMonth = day.format(date),
            )
        }
    }
}

/**
 * The day carousel strip above the summary chips.
 *
 * The strip is a non-user-scrollable [LazyRow] whose scroll position follows the
 * day currently at the top of the vertical Home feed. It also supports
 * drag-to-scrub (which drives the feed instantly) and tap-to-jump (handled by
 * [onPillTap]).
 */
@Composable
fun DayCarouselStrip(
    days: List<WorkoutDay>,
    todayIndex: Int,
    activeIndex: Int,
    collapseFraction: Float,
    pagerState: PagerState,
    onPillTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return

    val f = collapseFraction.coerceIn(0f, 1f)
    val cells = remember(days) { buildDayCells(days) }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Track the latest activeIndex for long-lived closures (effect + gesture lambdas).
    val activeIndexState = rememberUpdatedState(activeIndex)

    // Center x (in strip content coordinates, including the 16dp start padding)
    // of every pill. We walk tag/pill widths exactly like the LazyRow does.
    val (pillCenters, pillItemIndices) = remember(cells, density) {
        with(density) {
            val tagWidthPx = monthTagWidth.toPx()
            val pillWidthPx = pillWidth.toPx()
            val gapPx = itemGap.toPx()
            var prefix = stripContentPadding.toPx()
            var itemIndex = 0
            val centers = FloatArray(cells.size)
            val itemIndices = IntArray(cells.size)
            for (i in cells.indices) {
                if (i == 0 || cells[i].monthKey != cells[i - 1].monthKey) {
                    prefix += tagWidthPx + gapPx
                    itemIndex += 1
                }
                centers[i] = prefix + pillWidthPx / 2f
                itemIndices[i] = itemIndex
                prefix += pillWidthPx + gapPx
                itemIndex += 1
            }
            centers to itemIndices
        }
    }

    var windowWidthPx by remember { mutableIntStateOf(0) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    // Strip follows the feed's top day: the active pill stays centered.
    LaunchedEffect(pillCenters, windowWidthPx) {
        if (windowWidthPx <= 0 || pillCenters.isEmpty()) return@LaunchedEffect
        val windowCenterPx = windowWidthPx / 2f
        val pillWidthPx = with(density) { pillWidth.toPx() }
        val contentPaddingPx = with(density) { stripContentPadding.toPx() }
        snapshotFlow { activeIndexState.value }
            .distinctUntilChanged()
            .collect { rawIndex ->
                val i = rawIndex.coerceIn(0, days.lastIndex)
                val activeCenter = pillCenters[i]
                // scrollToItem places the anchored pill's start edge scrollOffset px
                // above the viewport start; centering the active pill on the window
                // needs off = C - pillStart - windowCenter. Anchor at the pill's
                // LazyRow item index (month tags counted).
                val anchorItemIndex = pillItemIndices[i]
                val pillStartContent = pillCenters[i] - pillWidthPx / 2f
                val scrollOffset =
                    (activeCenter - pillStartContent - windowCenterPx + contentPaddingPx)
                        .roundToInt()
                listState.scrollToItem(index = anchorItemIndex, scrollOffset = scrollOffset)
            }
    }

    val dayPitchPx = with(density) { (pillWidth + itemGap).toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = 1f - f }
            .background(MaterialTheme.colorScheme.surface)
            .height(lerp(stripWrapHeight, 0.dp, f))
            .padding(top = lerp(stripTopPadding, 0.dp, f))
            .clipToBounds(),
        verticalArrangement = Arrangement.Top,
    ) {
        LazyRow(
            state = listState,
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = stripContentPadding),
            horizontalArrangement = Arrangement.spacedBy(itemGap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(stripHeight)
                .onSizeChanged { windowWidthPx = it.width }
                .pointerInput(days.size, dayPitchPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            scrubPosition = activeIndexState.value.toFloat()
                                .coerceIn(0f, days.lastIndex.toFloat())
                        },
                        onHorizontalDrag = { change, dragAmountPx ->
                            change.consume()
                            scrubPosition = (scrubPosition - dragAmountPx / dayPitchPx)
                                .coerceIn(0f, days.lastIndex.toFloat())
                            // Snap the pager to the scrubbed day's page.
                            scope.launch {
                                pagerState.scrollToPage(
                                    scrubPosition.roundToInt().coerceIn(0, days.lastIndex),
                                )
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    scrubPosition.roundToInt()
                                        .coerceIn(0, days.lastIndex),
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    scrubPosition.roundToInt()
                                        .coerceIn(0, days.lastIndex),
                                )
                            }
                        },
                    )
                },
        ) {
            days.indices.forEach { i ->
                val cell = cells[i]
                if (i == 0 || cell.monthKey != cells[i - 1].monthKey) {
                    item(key = "tag-$i") {
                        MonthTag(label = cell.monthShort)
                    }
                }
                item(key = "pill-$i") {
                    DayPill(
                        cell = cell,
                        active = i == activeIndex,
                        isToday = i == todayIndex,
                        onClick = { onPillTap(i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthTag(label: String) {
    Box(
        modifier = Modifier
            .width(monthTagWidth)
            .height(itemHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun DayPill(
    cell: DayCell,
    active: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dayPillScale",
    )
    val background = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dotColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .zIndex(if (active) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(pillWidth)
            .height(itemHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .semantics { contentDescription = "${cell.weekdayRaw} ${cell.dayOfMonth}" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = cell.weekdayShort,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.em,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = cell.dayOfMonth,
                style = TNUM.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight(650)),
                color = contentColor,
                maxLines = 1,
            )
        }
        if (isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}
