package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.domain.ExerciseMetric
import com.readyplayerai.gainsgoblin.domain.ExercisePrKind
import com.readyplayerai.gainsgoblin.domain.ExerciseRange
import com.readyplayerai.gainsgoblin.domain.ExerciseSession
import com.readyplayerai.gainsgoblin.domain.metricValue
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatVolume
import com.readyplayerai.gainsgoblin.ui.compose.progress.formatWeight
import java.util.Locale
import kotlin.math.roundToInt

/** Localised label for [metric] (e.g. "Est. 1RM", "Volume"). */
@Composable
fun metricLabel(metric: ExerciseMetric): String = stringResource(
    when (metric) {
        ExerciseMetric.EST_ONE_REP -> R.string.exercise_progress_metric_est_1rm
        ExerciseMetric.TOP_SET -> R.string.exercise_progress_metric_top_set
        ExerciseMetric.VOLUME -> R.string.exercise_progress_metric_volume
        ExerciseMetric.REPS -> R.string.exercise_progress_metric_reps
    }
)

/** Localised unit for [metric]: kg for load/volume metrics, reps otherwise. */
@Composable
fun metricUnit(metric: ExerciseMetric): String = stringResource(
    when (metric) {
        ExerciseMetric.REPS -> R.string.add_exercise_reps_unit
        else -> R.string.add_exercise_kg
    }
)

/** Short label for [range] shown on the segmented range switch. */
@Composable
fun rangeLabel(range: ExerciseRange): String = stringResource(
    when (range) {
        ExerciseRange.ONE_MONTH -> R.string.exercise_progress_range_1m
        ExerciseRange.THREE_MONTHS -> R.string.exercise_progress_range_3m
        ExerciseRange.SIX_MONTHS -> R.string.exercise_progress_range_6m
        ExerciseRange.ALL -> R.string.exercise_progress_range_all
    }
)

/** Long-form description of [range] used in the hero meta line. */
@Composable
fun rangeDescription(range: ExerciseRange): String = stringResource(
    when (range) {
        ExerciseRange.ONE_MONTH -> R.string.exercise_progress_range_last_month
        ExerciseRange.THREE_MONTHS -> R.string.exercise_progress_range_last_3_months
        ExerciseRange.SIX_MONTHS -> R.string.exercise_progress_range_last_6_months
        ExerciseRange.ALL -> R.string.exercise_progress_range_all_time
    }
)

/** Maps [metric] onto the record kind used for the PR pill. */
fun metricPrKind(metric: ExerciseMetric): ExercisePrKind = when (metric) {
    ExerciseMetric.EST_ONE_REP -> ExercisePrKind.EST_ONE_REP
    ExerciseMetric.TOP_SET -> ExercisePrKind.MAX_WEIGHT
    ExerciseMetric.VOLUME -> ExercisePrKind.VOLUME
    ExerciseMetric.REPS -> ExercisePrKind.MAX_REPS
}

/** Formats the hero's big [value] for [metric]. */
fun formatMetricValue(metric: ExerciseMetric, value: Double): String = when (metric) {
    ExerciseMetric.EST_ONE_REP,
    ExerciseMetric.TOP_SET,
    -> formatWeight(value)

    ExerciseMetric.VOLUME -> formatVolume(value)
    ExerciseMetric.REPS -> value.roundToInt().toString()
}

/** Formats a chart-axis [value] for [metric], compacting large volumes to tonnes. */
fun formatAxisValue(metric: ExerciseMetric, value: Double): String = when (metric) {
    ExerciseMetric.EST_ONE_REP -> value.roundToInt().toString()
    ExerciseMetric.TOP_SET -> formatWeight(value)
    ExerciseMetric.VOLUME -> if (value >= 1000.0) {
        String.format(Locale.US, "%.1f t", value / 1000.0)
    } else {
        value.roundToInt().toString()
    }

    ExerciseMetric.REPS -> value.roundToInt().toString()
}

/** Value of [metric] for [session] (plain wrapper around the domain extension). */
fun metricValue(session: ExerciseSession, metric: ExerciseMetric): Double =
    session.metricValue(metric)
