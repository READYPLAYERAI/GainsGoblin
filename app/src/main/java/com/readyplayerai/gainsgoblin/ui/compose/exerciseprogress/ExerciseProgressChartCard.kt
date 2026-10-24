package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseMetric
import com.readyplayerai.gainsgoblin.domain.ExerciseRange
import com.readyplayerai.gainsgoblin.domain.ExerciseSession
import com.readyplayerai.gainsgoblin.ui.compose.charts.ChartPoint
import com.readyplayerai.gainsgoblin.ui.compose.charts.ExerciseLineChart
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatWeight
import com.readyplayerai.gainsgoblin.ui.compose.progress.shortDate
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Large scrubable progression card for the selected [metric] and [range]: a
 * header with the metric and PR count, the [ExerciseLineChart] itself and a
 * legend footer.
 */
@Composable
fun ExerciseProgressChartCard(
    sessions: List<ExerciseSession>,
    metric: ExerciseMetric,
    range: ExerciseRange,
    modifier: Modifier = Modifier,
) {
    val palette = metricPalette(metric)
    val prRingColor = if (isSystemInDarkTheme()) {
        Color(0xFFFFB68B)
    } else {
        Color(0xFFD98A5B)
    }
    val prLabel = stringResource(R.string.exercise_progress_chart_pr)
    val unit = metricUnit(metric)
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.US) }
    val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    fun labelFor(date: String): String {
        if (range == ExerciseRange.ONE_MONTH) return shortDate(date)
        val parsed = try {
            isoFormat.parse(date)
        } catch (e: ParseException) {
            null
        }
        return if (parsed != null) monthFormat.format(parsed) else date
    }

    val points = remember(sessions, metric, range, prLabel) {
        sessions.map { session ->
            ChartPoint(
                label = labelFor(session.date),
                value = metricValue(session, metric).toFloat(),
                isPr = session.prKinds.contains(metricPrKind(metric)),
                dateLabel = shortDate(session.date),
                detail = "${formatWeight(session.topWeight)} kg × " +
                    "${formatWeight(session.topReps)} · ${session.sets.size} sets",
                prLabel = prLabel,
            )
        }
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(points) { selectedIndex = null }

    val prCount = points.count { it.isPr }

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.exercise_progress_chart_kicker).uppercase(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.05.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(palette.accent),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = metricLabel(metric),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.exercise_progress_pr_count,
                            prCount,
                            prCount,
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            ExerciseLineChart(
                points = points,
                accent = palette.accent,
                prRingColor = prRingColor,
                valueFormatter = { value ->
                    formatMetricValue(metric, value.toDouble()) + " " + unit
                },
                axisFormatter = { value -> formatAxisValue(metric, value.toDouble()) },
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { selectedIndex = it },
                emptyText = stringResource(R.string.exercise_progress_chart_empty),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .padding(top = 12.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                    )
                    Spacer(Modifier.width(5.dp))
                    val first = sessions.firstOrNull()
                    val last = sessions.lastOrNull()
                    Text(
                        text = metricLabel(metric) + if (first != null && last != null) {
                            " · " + stringResource(
                                R.string.exercise_progress_chart_date_range,
                                shortDate(first.date),
                                shortDate(last.date),
                            )
                        } else {
                            ""
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.5.dp, prRingColor, CircleShape),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.exercise_progress_pr_legend),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
