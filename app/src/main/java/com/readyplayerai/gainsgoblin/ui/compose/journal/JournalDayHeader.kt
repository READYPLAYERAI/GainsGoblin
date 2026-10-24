package com.readyplayerai.gainsgoblin.ui.compose.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Sticky day header for Journal's vertical journal: date tile, day title, optional
 * Today pill and a one-line summary. Opaque background so it reads cleanly while
 * pinned during scroll.
 */
@Composable
internal fun JournalDayHeader(
    day: WorkoutDay,
    realDay: WorkoutDay?,
    isToday: Boolean,
    draftExerciseCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val headerBackground = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val titleColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val summaryColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(headerBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateTile(day = day, isToday = isToday)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = headerTitle(day.Date),
                        fontSize = 15.sp,
                        fontWeight = FontWeight(650),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isToday) {
                        Spacer(Modifier.width(8.dp))
                        TodayPill()
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = headerSummary(realDay, draftExerciseCount),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = summaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    if (isToday) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.20f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
        )
    }
}

@Composable
private fun DateTile(day: WorkoutDay, isToday: Boolean) {
    val weekday = remember(day.Date) { formatDatePart(day.Date, "EEE") }
    val dayOfMonth = remember(day.Date) { formatDatePart(day.Date, "d") }

    Column(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isToday) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = weekday.uppercase(Locale.getDefault()),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
            color = if (isToday) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = dayOfMonth,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            color = if (isToday) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun TodayPill() {
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onPrimaryContainer)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.journal_today_pill),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.09.em,
            color = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun headerSummary(realDay: WorkoutDay?, draftExerciseCount: Int): String {
    val exercises = realDay?.Exercises.orEmpty()
    val exerciseCount = exercises.size + draftExerciseCount
    if (exerciseCount == 0) {
        return stringResource(R.string.journal_rest_day_summary)
    }
    val setCount = exercises.sumOf { it.Sets?.size ?: 0 }
    val volume = realDay?.DayVolume ?: 0.0
    val volumeText = NumberFormat.getIntegerInstance(Locale.getDefault()).format(volume.roundToInt())
    val base = when {
        exerciseCount == 1 && setCount == 1 ->
            stringResource(R.string.journal_day_summary_one_set, volumeText)
        exerciseCount == 1 ->
            stringResource(R.string.journal_day_summary_one, setCount, volumeText)
        else ->
            stringResource(
                R.string.journal_day_summary,
                exerciseCount,
                setCount,
                volumeText,
            )
    }
    val pr = findJournalPr(exercises)
    return if (pr != null) {
        base + " · ★ " + stringResource(R.string.journal_day_pr_flag)
    } else {
        base
    }
}

private fun headerTitle(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(parsed) else ""
} catch (e: Exception) {
    ""
}

internal fun formatMonthYear(date: String?): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(parsed) else ""
} catch (e: Exception) {
    ""
}

private fun formatDatePart(date: String?, pattern: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date ?: "")
    if (parsed != null) SimpleDateFormat(pattern, Locale.getDefault()).format(parsed) else ""
} catch (e: Exception) {
    ""
}
