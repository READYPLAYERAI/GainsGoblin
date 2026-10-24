package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseMetric
import com.readyplayerai.gainsgoblin.domain.ExerciseProgress
import com.readyplayerai.gainsgoblin.domain.ExerciseRange
import com.readyplayerai.gainsgoblin.domain.ExerciseSession
import com.readyplayerai.gainsgoblin.domain.exerciseWeekAverages
import com.readyplayerai.gainsgoblin.domain.todayDateString
import com.readyplayerai.gainsgoblin.ui.compose.progress.ConsistencyHeatmap
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatWeight
import com.readyplayerai.gainsgoblin.ui.compose.progress.shortDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The trend block of the per-exercise detail: an insight card with the selected
 * metric's range delta and weekly bars, a 16-week consistency heatmap, and a
 * two-column row of all-time rep-range and per-month session distributions.
 *
 * [sessions] must already be filtered to [range] and ordered ascending; records
 * and the distribution cards read all-time [progress] data while the insight
 * and consistency average use [sessions]. The whole block is plain composables
 * so it never introduces a nested vertical scroller.
 */
@Composable
fun ExerciseTrends(
    progress: ExerciseProgress,
    sessions: List<ExerciseSession>,
    metric: ExerciseMetric,
    range: ExerciseRange,
    modifier: Modifier = Modifier,
) {
    val palette = metricPalette(metric)
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InsightCard(sessions = sessions, metric = metric, range = range)
        ConsistencyCard(progress = progress, sessions = sessions, metric = metric)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RepRangesCard(progress = progress, palette = palette, surfaceHigh = surfaceHigh)
            PerMonthCard(progress = progress, surfaceHigh = surfaceHigh)
        }
    }
}

/** Insight card: metric delta over [range], a copy sentence and weekly bars. */
@Composable
private fun InsightCard(
    sessions: List<ExerciseSession>,
    metric: ExerciseMetric,
    range: ExerciseRange,
) {
    val palette = metricPalette(metric)
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(palette.container, surfaceHigh)))
            .padding(16.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            val first = sessions.firstOrNull()
            val last = sessions.lastOrNull()
            val firstValue = first?.let { metricValue(it, metric) } ?: 0.0
            val endValue = last?.let { metricValue(it, metric) } ?: 0.0
            val improving = endValue >= firstValue

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(
                        if (improving) {
                            R.string.exercise_progress_insight_up_title
                        } else {
                            R.string.exercise_progress_insight_down_title
                        },
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onContainer,
                )
                Text(
                    text = metricLabel(metric),
                    fontSize = 11.5.sp,
                    color = palette.onContainer.copy(alpha = 0.72f),
                )
            }

            if (sessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.exercise_progress_insight_no_sessions),
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 12.5.sp,
                    color = palette.onContainer.copy(alpha = 0.8f),
                )
            } else {
                val diff = endValue - firstValue
                val deltaText: String = when (metric) {
                    ExerciseMetric.EST_ONE_REP,
                    ExerciseMetric.TOP_SET,
                    -> {
                        if (abs(diff) < 0.05) {
                            stringResource(R.string.exercise_progress_no_change)
                        } else {
                            (if (diff >= 0) "+" else "-") + formatWeight(abs(diff)) + " kg"
                        }
                    }

                    else -> {
                        if (abs(diff) < 0.05 || firstValue == 0.0) {
                            stringResource(R.string.exercise_progress_no_change)
                        } else {
                            val percent = (diff / firstValue * 100).roundToInt()
                            (if (percent >= 0) "+" else "-") + abs(percent) + "%"
                        }
                    }
                }

                val firstFormatted = formatMetricValue(metric, firstValue) + " " + metricUnit(metric)
                val lastFormatted = formatMetricValue(metric, endValue) + " " + metricUnit(metric)

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = deltaText,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.36).sp,
                        color = palette.onContainer,
                    )
                    Text(
                        text = " " + stringResource(
                            if (range == ExerciseRange.ALL) {
                                R.string.exercise_progress_insight_total
                            } else {
                                R.string.exercise_progress_insight_in
                            },
                            if (range == ExerciseRange.ALL) "" else rangeDescription(range),
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onContainer.copy(alpha = 0.72f),
                    )
                }

                Text(
                    text = stringResource(
                        if (diff >= 0) {
                            R.string.exercise_progress_insight_copy_up
                        } else {
                            R.string.exercise_progress_insight_copy_down
                        },
                        firstFormatted,
                        lastFormatted,
                        sessions.size,
                        shortDate(first?.date),
                    ),
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 12.5.sp,
                    color = palette.onContainer.copy(alpha = 0.82f),
                )

                val weekly = exerciseWeekAverages(sessions, metric).takeLast(14)
                val maxWeekly = weekly.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(42.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    weekly.forEachIndexed { index, average ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((42.0 * average.value / maxWeekly).coerceAtLeast(4.0).dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 5.dp,
                                        topEnd = 5.dp,
                                        bottomStart = 3.dp,
                                        bottomEnd = 3.dp,
                                    ),
                                )
                                .background(
                                    if (index == weekly.lastIndex) {
                                        palette.accent
                                    } else {
                                        palette.accent.copy(alpha = 0.30f)
                                    },
                                ),
                        )
                    }
                }
                if (weekly.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = shortDate(weekly.first().weekStart),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer.copy(alpha = 0.70f),
                        )
                        Text(
                            text = stringResource(R.string.exercise_progress_weekly_avg),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer.copy(alpha = 0.70f),
                        )
                        Text(
                            text = shortDate(weekly.last().weekStart),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer.copy(alpha = 0.70f),
                        )
                    }
                }
            }
        }
    }
}

/** Consistency card: 16-week training heatmap with streak and weekly average. */
@Composable
private fun ConsistencyCard(
    progress: ExerciseProgress,
    sessions: List<ExerciseSession>,
    metric: ExerciseMetric,
) {
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val weeks = exerciseWeekAverages(sessions, metric).size.coerceAtLeast(1)
    val avgPerWeek = String.format(Locale.US, "%.1f", sessions.size.toDouble() / weeks)

    Surface(shape = RoundedCornerShape(30.dp), color = surfaceHigh) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.exercise_progress_consistency),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.exercise_progress_trends_last_weeks,
                        progress.consistencyWeeks,
                    ),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ConsistencyHeatmap(
                cells = progress.consistency,
                weeks = progress.consistencyWeeks,
                today = todayDateString(),
                oldestLabel = stringResource(
                    R.string.exercise_progress_weeks_ago,
                    progress.consistencyWeeks,
                ),
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 11.dp)) {
                Text(
                    text = stringResource(
                        R.string.exercise_progress_consistency_caption,
                        progress.streakWeeks,
                        avgPerWeek,
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Rep-range distribution card: all-time share of sets per repetition bucket. */
@Composable
private fun RowScope.RepRangesCard(
    progress: ExerciseProgress,
    palette: MetricPalette,
    surfaceHigh: Color,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val totalSets = progress.repBuckets.sumOf { it.sets }
    val maxCount = (progress.repBuckets.maxOfOrNull { it.sets } ?: 0).coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = surfaceHigh,
        modifier = Modifier.weight(1f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.exercise_progress_rep_ranges),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.exercise_progress_rep_ranges_meta, totalSets),
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 10.5.sp,
                color = onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                progress.repBuckets.forEachIndexed { index, bucket ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${Math.round(bucket.sets * 100.0 / max(1, totalSets))}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceVariant,
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    (56.0 * bucket.sets / maxCount)
                                        .coerceAtLeast(if (bucket.sets > 0) 4.0 else 2.0)
                                        .dp,
                                )
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (index == progress.repBuckets.lastIndex) {
                                        palette.accent
                                    } else {
                                        lerp(
                                            MaterialTheme.colorScheme.surface,
                                            palette.accent,
                                            0.32f + index * 0.08f,
                                        )
                                    },
                                ),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                progress.repBuckets.forEach { bucket ->
                    Text(
                        text = bucket.label,
                        modifier = Modifier.weight(1f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

/** Per-month card: number of sessions trained in each of the last months. */
@Composable
private fun RowScope.PerMonthCard(
    progress: ExerciseProgress,
    surfaceHigh: Color,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maxMonth = (progress.monthlySessions.maxOfOrNull { it.sessions } ?: 0).coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = surfaceHigh,
        modifier = Modifier.weight(1f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.exercise_progress_per_month),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.exercise_progress_per_month_desc),
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 10.5.sp,
                color = onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                progress.monthlySessions.forEachIndexed { index, month ->
                    val count = month.sessions
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (count > 0) count.toString() else "",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceVariant,
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    (56.0 * count / maxMonth)
                                        .coerceAtLeast(if (count > 0) 4.0 else 2.0)
                                        .dp,
                                )
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (index == progress.monthlySessions.lastIndex) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                ),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                progress.monthlySessions.forEach { month ->
                    Text(
                        text = month.label,
                        modifier = Modifier.weight(1f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}
