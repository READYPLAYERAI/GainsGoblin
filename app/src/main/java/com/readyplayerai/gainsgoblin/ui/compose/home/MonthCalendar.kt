package com.readyplayerai.gainsgoblin.ui.compose.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val TNUM = TextStyle(fontFeatureSettings = "tnum")

private val weekdayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

private data class DisplayMonth(val year: Int, val month: Int)

private data class MonthCell(val dayOfMonth: Int?, val date: String?)

private fun monthOf(date: String?): DisplayMonth? {
    if (date.isNullOrBlank()) return null
    return try {
        val cal = Calendar.getInstance()
        cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: return null
        DisplayMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    } catch (e: Exception) {
        null
    }
}

private fun buildMonthCells(year: Int, month: Int): List<MonthCell> {
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val leading = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return List(42) { i ->
        val day = i - leading + 1
        if (day in 1..daysInMonth) {
            MonthCell(
                dayOfMonth = day,
                date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day),
            )
        } else {
            MonthCell(dayOfMonth = null, date = null)
        }
    }
}

/**
 * The in-flow full-month calendar that replaces the day strip on Home.
 *
 * Selecting a day closes the panel (the caller performs the rule-4 jump), while
 * a drag further into the content closes it without picking. The panel sits on
 * the surface with no elevation or scrim.
 */
@Composable
fun MonthCalendar(
    days: List<WorkoutDay>,
    todayIndex: Int,
    selectedDayIndex: Int,
    onPickDay: (Int) -> Unit,
    onCloseGesture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return

    val monthTitleFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dateToIndex = remember(days) {
        val map = HashMap<String, Int>(days.size)
        days.forEachIndexed { index, day ->
            day.Date?.let { map[it] = index }
        }
        map
    }

    val initialMonth = remember(days, selectedDayIndex) {
        monthOf(days.getOrNull(selectedDayIndex)?.Date)
            ?: monthOf(days.firstOrNull { !it.Date.isNullOrBlank() }?.Date)
            ?: DisplayMonth(
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
            )
    }
    var displayedMonth by remember(selectedDayIndex) { mutableStateOf(initialMonth) }

    val cells = remember(displayedMonth) {
        buildMonthCells(displayedMonth.year, displayedMonth.month)
    }
    val title = remember(displayedMonth) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(displayedMonth.year, displayedMonth.month, 1)
        }
        monthTitleFormatter.format(cal.time)
    }
    val firstDate = days.firstOrNull()?.Date
    val lastDate = days.lastOrNull()?.Date
    val closeThresholdPx = with(LocalDensity.current) { 30.dp.toPx() }

    fun navigateMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(displayedMonth.year, displayedMonth.month, 1)
            add(Calendar.MONTH, delta)
        }
        displayedMonth = DisplayMonth(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp)
            .pointerInput(closeThresholdPx) {
                var accumulated = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDragEnd = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f },
                ) { change, dragAmount ->
                    change.consume()
                    // Negative y == finger moving up == into the content.
                    if (dragAmount > 0f) {
                        accumulated = 0f
                    } else {
                        accumulated += dragAmount
                    }
                    if (accumulated <= -closeThresholdPx) {
                        accumulated = 0f
                        onCloseGesture()
                    }
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { navigateMonth(-1) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_left_24px),
                    contentDescription = stringResource(R.string.home_prev_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight(650),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { navigateMonth(1) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right_24px),
                    contentDescription = stringResource(R.string.home_next_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.06.em,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (col in 0 until 7) {
                        val cell = cells[row * 7 + col]
                        val dayOfMonth = cell.dayOfMonth
                        val date = cell.date
                        if (dayOfMonth == null || date == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                            )
                        } else {
                            val index = dateToIndex[date]
                            val outOfWindow = index == null
                            val pickIndex = index ?: if (firstDate != null && date < firstDate) {
                                0
                            } else if (lastDate != null && date > lastDate) {
                                days.lastIndex
                            } else {
                                days.lastIndex
                            }
                            MonthDayCell(
                                dayOfMonth = dayOfMonth,
                                isToday = index != null && index == todayIndex,
                                isSelected = index != null && index == selectedDayIndex,
                                outOfWindow = outOfWindow,
                                onClick = { onPickDay(pickIndex) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    dayOfMonth: Int,
    isToday: Boolean,
    isSelected: Boolean,
    outOfWindow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .then(if (outOfWindow) Modifier.alpha(0.38f) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayOfMonth.toString(),
            style = TNUM.copy(
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight(600),
            ),
            color = contentColor,
            maxLines = 1,
        )
        if (isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}
