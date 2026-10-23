package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.PeriodRange
import com.readyplayerai.gainsgoblin.domain.consistencyCells
import com.readyplayerai.gainsgoblin.domain.periodStats
import com.readyplayerai.gainsgoblin.domain.setsPerCategory
import com.readyplayerai.gainsgoblin.domain.weeklyVolumeBuckets
import com.readyplayerai.gainsgoblin.model.WorkoutDay
import com.readyplayerai.gainsgoblin.ui.compose.CategoryMarker
import com.readyplayerai.gainsgoblin.ui.compose.categoryColor
import com.readyplayerai.gainsgoblin.ui.compose.charts.BarChart
import com.readyplayerai.gainsgoblin.ui.compose.charts.BarEntryData
import com.readyplayerai.gainsgoblin.ui.compose.charts.PieChart
import com.readyplayerai.gainsgoblin.ui.compose.charts.PieLegend
import com.readyplayerai.gainsgoblin.ui.compose.charts.PieSliceData
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Renders the Insights lens: a volume-trend bar chart with its weekly average,
 * an all-time bodypart split pie, and a ten-week consistency heatmap.
 */
@Composable
fun InsightsLens(days: List<WorkoutDay>, today: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VolumeTrendCard(days)
        BodypartSplitCard(days)
        ConsistencyCard(days, today)
    }
}

/** Shared rounded card container used by the three Insights cards. */
@Composable
private fun InsightCard(
    title: String,
    caption: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(18.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(caption, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** Card 1: the last ten weekly volume buckets with an average line and delta. */
@Composable
private fun VolumeTrendCard(days: List<WorkoutDay>) {
    val weekly = remember(days) { weeklyVolumeBuckets(days).takeLast(10) }
    val stats = remember(days) { periodStats(days, PeriodRange.WEEK) }

    InsightCard(
        title = stringResource(R.string.insights_volume_title),
        caption = stringResource(R.string.insights_volume_caption, 10),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatTonsOrKg(stats.volume),
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            val delta = stats.deltaPercent
            if (delta != null) {
                val up = delta >= 0
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (up) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (up) R.string.insights_delta_up else R.string.insights_delta_down,
                            abs(delta).roundToInt(),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
        BarChart(
            entries = weekly.map { BarEntryData(label = shortDate(it.label), value = it.value) },
            averageValue = if (weekly.isEmpty()) {
                null
            } else {
                weekly.sumOf { it.value.toDouble() }.toFloat() / weekly.size
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

/** Card 2: the all-time set split per category as a pie with its legend. */
@Composable
private fun BodypartSplitCard(days: List<WorkoutDay>) {
    val counts = remember(days) { setsPerCategory(days) }
    val slices = counts.map {
        PieSliceData(
            label = it.category,
            value = it.sets.toFloat(),
            color = categoryColor(it.category),
        )
    }

    InsightCard(
        title = stringResource(R.string.insights_bodypart_title),
        caption = stringResource(R.string.insights_bodypart_caption, counts.sumOf { it.sets }),
    ) {
        if (counts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PieChart(
                    slices = slices,
                    modifier = Modifier.width(140.dp),
                    chartHeight = 140.dp,
                )
                PieLegend(
                    slices = slices,
                    markerContent = { CategoryMarker(category = it.label, size = 13.dp) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Card 3: the ten-week consistency heatmap with the best training week. */
@Composable
private fun ConsistencyCard(days: List<WorkoutDay>, today: String) {
    val cells = remember(days, today) { consistencyCells(days, weeks = 10) }
    val bestWeek = remember(cells) {
        cells.chunked(7).maxOfOrNull { chunk -> chunk.count { it.level > 0 } } ?: 0
    }

    InsightCard(
        title = stringResource(R.string.insights_consistency_title),
        caption = stringResource(R.string.insights_consistency_caption, bestWeek),
    ) {
        ConsistencyHeatmap(
            cells = cells,
            weeks = 10,
            today = today,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
