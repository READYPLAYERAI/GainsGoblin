package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseMetric
import com.readyplayerai.gainsgoblin.domain.ExerciseProgress
import com.readyplayerai.gainsgoblin.domain.ExerciseRange
import com.readyplayerai.gainsgoblin.domain.ExerciseSession
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import com.readyplayerai.gainsgoblin.ui.compose.progress.Sparkline
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatTonsOrKg
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatWeight
import com.readyplayerai.gainsgoblin.ui.compose.progress.shortDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tinted metric hero for the per-exercise progress detail screen: a range switch,
 * the big metric value with a delta chip, an optional PR pill, a sparkline and
 * three stat pills. [sessions] are already range-filtered and ascending.
 */
@Composable
fun ExerciseProgressHero(
    progress: ExerciseProgress,
    sessions: List<ExerciseSession>,
    metric: ExerciseMetric,
    range: ExerciseRange,
    onRangeChange: (ExerciseRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = metricPalette(metric)
    val surface = MaterialTheme.colorScheme.surface

    val first = sessions.firstOrNull()
    val last = sessions.lastOrNull()
    val value = last?.let { metricValue(it, metric) } ?: 0.0
    val firstValue = first?.let { metricValue(it, metric) } ?: 0.0
    val diff = if (sessions.size >= 2) value - firstValue else 0.0

    val flat = abs(diff) < 0.05 ||
        ((metric == ExerciseMetric.VOLUME || metric == ExerciseMetric.REPS) && firstValue == 0.0)

    val deltaText = if (flat) {
        stringResource(R.string.exercise_progress_no_change)
    } else if (metric == ExerciseMetric.EST_ONE_REP || metric == ExerciseMetric.TOP_SET) {
        if (diff > 0) {
            stringResource(R.string.exercise_progress_delta_up_kg, formatWeight(diff))
        } else {
            stringResource(R.string.exercise_progress_delta_down_kg, formatWeight(-diff))
        }
    } else {
        val pct = diff / firstValue * 100.0
        if (diff > 0) {
            stringResource(R.string.exercise_progress_delta_up_pct, abs(pct).roundToInt())
        } else {
            stringResource(R.string.exercise_progress_delta_down_pct, abs(pct).roundToInt())
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(palette.container, lerp(palette.container, surface, 0.45f))
                )
            )
            .padding(18.dp),
    ) {
        val narrowKicker = maxWidth < 340.dp

        if (progress.category.isNotBlank()) {
            CategoryMarker(
                category = progress.category,
                size = 164.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 26.dp, y = 30.dp)
                    .graphicsLayer {
                        alpha = 0.10f
                        rotationZ = -8f
                    },
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val kicker = if (narrowKicker) {
                    metricLabel(metric).uppercase()
                } else {
                    stringResource(
                        R.string.exercise_progress_metric_kicker,
                        metricLabel(metric).uppercase(),
                        if (range == ExerciseRange.ALL) {
                            stringResource(R.string.exercise_progress_range_all_time)
                        } else {
                            rangeLabel(range)
                        },
                    )
                }
                Text(
                    text = kicker,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = palette.onContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                SingleChoiceSegmentedButtonRow {
                    ExerciseRange.entries.forEachIndexed { index, r ->
                        SegmentedButton(
                            selected = range == r,
                            onClick = { onRangeChange(r) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index,
                                ExerciseRange.entries.size,
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            label = { Text(rangeLabel(r), maxLines = 1, fontSize = 11.5.sp) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = formatMetricValue(metric, value),
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp,
                        lineHeight = 46.sp,
                        color = palette.onContainer,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = " " + metricUnit(metric),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onContainer.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                }
                Surface(
                    modifier = Modifier
                        .padding(start = 10.dp, bottom = 5.dp)
                        .height(27.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = palette.onContainer.copy(alpha = 0.13f),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = deltaText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.onContainer,
                            maxLines = 1,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (last != null && last.prKinds.contains(metricPrKind(metric))) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = palette.onContainer.copy(alpha = 0.15f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_whatshot_24px),
                                contentDescription = null,
                                tint = palette.onContainer,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.exercise_progress_pr_pill,
                                    shortDate(last.date),
                                ).uppercase(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.315.sp,
                                color = palette.onContainer,
                            )
                        }
                    }
                } else {
                    Text(
                        text = pluralStringResource(
                            R.plurals.exercise_progress_range_meta,
                            sessions.size,
                            rangeDescription(range),
                            sessions.size,
                        ),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.onContainer.copy(alpha = 0.70f),
                    )
                }
            }

            Sparkline(
                values = sessions.map { metricValue(it, metric).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(top = 10.dp),
                lineColor = palette.onContainer,
                dotInnerColor = palette.container,
            )

            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text(
                            text = sessions.size.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.32).sp,
                            color = palette.onContainer,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.exercise_progress_stat_sessions),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer.copy(alpha = 0.70f),
                            maxLines = 1,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text(
                            text = sessions.sumOf { it.sets.size }.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.32).sp,
                            color = palette.onContainer,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.exercise_progress_stat_sets),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer.copy(alpha = 0.70f),
                            maxLines = 1,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text(
                            text = formatTonsOrKg(sessions.sumOf { it.volume }),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.32).sp,
                            color = palette.onContainer,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.exercise_progress_stat_volume),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onContainer.copy(alpha = 0.70f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
