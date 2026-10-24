package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseMetric
import com.readyplayerai.gainsgoblin.domain.ExercisePrKind
import com.readyplayerai.gainsgoblin.domain.ExerciseProgressRecord
import com.readyplayerai.gainsgoblin.ui.compose.progress.dayWithYear
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatVolume
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatWeight

/** One record card spec: which all-time [kind] to show, its label/icon and the metric it opens. */
private data class RecordCardSpec(
    val kind: ExercisePrKind,
    val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val metric: ExerciseMetric,
)

private val RecordCardSpecs = listOf(
    RecordCardSpec(
        kind = ExercisePrKind.MAX_WEIGHT,
        labelRes = R.string.exercise_progress_record_heaviest,
        iconRes = R.drawable.ic_leaderboard_24px,
        metric = ExerciseMetric.TOP_SET,
    ),
    RecordCardSpec(
        kind = ExercisePrKind.EST_ONE_REP,
        labelRes = R.string.exercise_progress_metric_est_1rm,
        iconRes = R.drawable.ic_whatshot_24px,
        metric = ExerciseMetric.EST_ONE_REP,
    ),
    RecordCardSpec(
        kind = ExercisePrKind.ACTUAL_ONE_REP,
        labelRes = R.string.exercise_progress_record_actual_1rm,
        iconRes = R.drawable.ic_whatshot_24px,
        metric = ExerciseMetric.TOP_SET,
    ),
    RecordCardSpec(
        kind = ExercisePrKind.SET_VOLUME,
        labelRes = R.string.exercise_progress_record_best_set,
        iconRes = R.drawable.ic_fitness_center_24px,
        metric = ExerciseMetric.VOLUME,
    ),
    RecordCardSpec(
        kind = ExercisePrKind.MAX_REPS,
        labelRes = R.string.exercise_progress_record_most_reps,
        iconRes = R.drawable.ic_nav_charts,
        metric = ExerciseMetric.REPS,
    ),
    RecordCardSpec(
        kind = ExercisePrKind.VOLUME,
        labelRes = R.string.exercise_progress_record_best_session,
        iconRes = R.drawable.ic_dashboard_24px,
        metric = ExerciseMetric.VOLUME,
    ),
)

/**
 * Horizontally scrollable row of all-time record cards for the selected
 * exercise. Each card shows one [ExerciseProgressRecord] (value, unit, meta and
 * date) and, when tapped, reports the matching [ExerciseMetric] so the host can
 * switch the chart metric. Cards are only rendered when the matching record
 * exists; the caller supplies the user-visible strings via [RecordCardSpecs].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseRecordsRow(
    records: List<ExerciseProgressRecord>,
    onRecordClick: (ExerciseMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(RecordCardSpecs) { spec ->
            val record = records.firstOrNull { it.kind == spec.kind } ?: return@items
            if (record.value <= 0.0) return@items

            val accent = when (spec.kind) {
                ExercisePrKind.MAX_WEIGHT -> MaterialTheme.colorScheme.tertiary
                ExercisePrKind.EST_ONE_REP -> MaterialTheme.colorScheme.primary
                ExercisePrKind.SET_VOLUME -> metricPalette(ExerciseMetric.VOLUME).accent
                ExercisePrKind.MAX_REPS -> metricPalette(ExerciseMetric.REPS).accent
                ExercisePrKind.VOLUME -> MaterialTheme.colorScheme.secondary
                ExercisePrKind.ACTUAL_ONE_REP -> MaterialTheme.colorScheme.primary
            }

            val valueText: String
            val unitText: String
            val metaText: String
            when (spec.kind) {
                ExercisePrKind.MAX_WEIGHT -> {
                    valueText = formatWeight(record.value)
                    unitText = stringResource(R.string.add_exercise_kg)
                    metaText = stringResource(
                        R.string.exercise_progress_record_meta_reps,
                        formatWeight(record.reps),
                    )
                }

                ExercisePrKind.EST_ONE_REP -> {
                    valueText = formatWeight(record.value)
                    unitText = stringResource(R.string.add_exercise_kg)
                    metaText = stringResource(
                        R.string.exercise_progress_record_meta_weight_reps,
                        formatWeight(record.weight),
                        formatWeight(record.reps),
                    )
                }

                ExercisePrKind.SET_VOLUME -> {
                    valueText = formatVolume(record.value)
                    unitText = stringResource(R.string.add_exercise_kg)
                    metaText = stringResource(
                        R.string.exercise_progress_record_meta_weight_reps,
                        formatWeight(record.weight),
                        formatWeight(record.reps),
                    )
                }

                ExercisePrKind.MAX_REPS -> {
                    valueText = formatWeight(record.value)
                    unitText = stringResource(R.string.add_exercise_reps_unit)
                    metaText = stringResource(
                        R.string.exercise_progress_record_meta_at_weight,
                        formatWeight(record.weight),
                    )
                }

                ExercisePrKind.VOLUME -> {
                    valueText = formatVolume(record.value)
                    unitText = stringResource(R.string.add_exercise_kg)
                    metaText = stringResource(
                        R.string.exercise_progress_record_meta_sets,
                        record.setCount,
                    )
                }

                ExercisePrKind.ACTUAL_ONE_REP -> {
                    valueText = formatWeight(record.value)
                    unitText = stringResource(R.string.add_exercise_kg)
                    metaText = stringResource(R.string.exercise_progress_record_meta_single)
                }
            }

            Surface(
                onClick = { onRecordClick(spec.metric) },
                modifier = Modifier.width(154.dp),
                shape = RoundedCornerShape(25.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawRect(
                                    color = accent,
                                    size = Size(4.dp.toPx(), size.height),
                                )
                            },
                    )
                    Column(
                        modifier = Modifier.padding(
                            start = 14.dp,
                            top = 13.dp,
                            end = 14.dp,
                            bottom = 13.dp,
                        ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(spec.iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(spec.labelRes).uppercase(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.76.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = valueText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.84).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = " $unitText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = metaText,
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = dayWithYear(record.date),
                            modifier = Modifier.padding(top = 5.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}
