package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.PeriodRange
import com.readyplayerai.gainsgoblin.domain.PeriodStats
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hero card for the Progress screen. The full variant shows the period label,
 * a 1W/1M/All range switch, the period volume, a delta chip, a sparkline and
 * three metric pills.
 */
@Composable
fun ProgressHero(
    range: PeriodRange,
    onRangeChange: (PeriodRange) -> Unit,
    stats: PeriodStats,
    sparkValues: List<Float>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.60f),
                    )
                )
            )
            .padding(20.dp),
    ) {
        FullHero(
            range = range,
            onRangeChange = onRangeChange,
            stats = stats,
            sparkValues = sparkValues,
        )
    }
}

/** Full hero body: range switch, volume, delta, sparkline and metric pills. */
@Composable
private fun FullHero(
    range: PeriodRange,
    onRangeChange: (PeriodRange) -> Unit,
    stats: PeriodStats,
    sparkValues: List<Float>,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = periodLabel(range),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            RangeSwitch(range = range, onRangeChange = onRangeChange)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatVolume(stats.volume),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " kg",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DeltaChip(stats)
        }
        Sparkline(
            values = sparkValues,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricPill(stats.workouts, stringResource(R.string.progress_metric_workouts), Modifier.weight(1f))
            MetricPill(stats.sets, stringResource(R.string.progress_metric_sets), Modifier.weight(1f))
            MetricPill(stats.prCount, stringResource(R.string.progress_metric_prs), Modifier.weight(1f))
        }
    }
}

/**
 * Official Material 3 Expressive range switch. Built on
 * [SingleChoiceSegmentedButtonRow], so it uses the official default colours,
 * shapes and motion, and shows the official check-icon crossfade on the
 * selected 1W / 1M / All segment.
 */
@Composable
private fun RangeSwitch(range: PeriodRange, onRangeChange: (PeriodRange) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        PeriodRange.entries.forEachIndexed { index, r ->
            SegmentedButton(
                selected = range == r,
                onClick = { onRangeChange(r) },
                shape = SegmentedButtonDefaults.itemShape(index, PeriodRange.entries.size),
                label = { Text(stringResource(r.labelRes), maxLines = 1) },
            )
        }
    }
}

/** The label string shown on a [PeriodRange] segment. */
private val PeriodRange.labelRes: Int
    get() = when (this) {
        PeriodRange.WEEK -> R.string.progress_range_week
        PeriodRange.MONTH -> R.string.progress_range_month
        PeriodRange.ALL -> R.string.progress_range_all
    }

/** Small pill showing the period-over-period volume delta, or the first date. */
@Composable
private fun DeltaChip(stats: PeriodStats) {
    val delta = stats.deltaPercent
    val since = stats.sinceDate
    when {
        delta != null -> {
            val up = delta >= 0
            Surface(
                modifier = Modifier.height(28.dp),
                color = if (up) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(999.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (up) R.string.progress_delta_up else R.string.progress_delta_down,
                            abs(delta).roundToInt(),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (up) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }
        }

        since != null -> {
            Surface(
                modifier = Modifier.height(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(999.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.progress_since, shortDate(since)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One rounded metric pill with a value over a caption. */
@Composable
private fun MetricPill(value: Int, caption: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Localised label for the selected [PeriodRange]. */
@Composable
private fun periodLabel(range: PeriodRange): String = stringResource(
    when (range) {
        PeriodRange.WEEK -> R.string.progress_period_week
        PeriodRange.MONTH -> R.string.progress_period_month
        PeriodRange.ALL -> R.string.progress_period_all
    }
)
