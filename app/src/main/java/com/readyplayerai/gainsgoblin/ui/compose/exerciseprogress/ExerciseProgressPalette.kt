package com.readyplayerai.gainsgoblin.ui.compose.exerciseprogress

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.readyplayerai.gainsgoblin.domain.ExerciseMetric

/**
 * Colour set used to tint the per-exercise progress hero and metric bar for one
 * [ExerciseMetric]. [accent]/[onAccent] drive the selected chip, while
 * [container]/[onContainer] drive the hero gradient and all of its content; the
 * fixed teal and amber palettes keep Volume and Reps visually distinct from the
 * theme-derived Est. 1RM and Top set cards.
 */
data class MetricPalette(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color,
)

/** Returns the [MetricPalette] for [metric], theme-aware for Volume and Reps. */
@Composable
fun metricPalette(metric: ExerciseMetric): MetricPalette {
    val scheme = MaterialTheme.colorScheme
    return when (metric) {
        ExerciseMetric.EST_ONE_REP -> MetricPalette(
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            container = scheme.primaryContainer,
            onContainer = scheme.onPrimaryContainer,
        )

        ExerciseMetric.TOP_SET -> MetricPalette(
            accent = scheme.tertiary,
            onAccent = scheme.onTertiary,
            container = scheme.tertiaryContainer,
            onContainer = scheme.onTertiaryContainer,
        )

        ExerciseMetric.VOLUME -> if (isSystemInDarkTheme()) {
            MetricPalette(
                accent = Color(0xFF62D3E0),
                onAccent = Color(0xFF00382F),
                container = Color(0xFF00503F),
                onContainer = Color(0xFFA7F2DD),
            )
        } else {
            MetricPalette(
                accent = Color(0xFF28B0C0),
                onAccent = Color(0xFF002F29),
                container = Color(0xFFC6EFE5),
                onContainer = Color(0xFF00382F),
            )
        }

        ExerciseMetric.REPS -> if (isSystemInDarkTheme()) {
            MetricPalette(
                accent = Color(0xFFEBC95E),
                onAccent = Color(0xFF2B1F00),
                container = Color(0xFF5A4400),
                onContainer = Color(0xFFFFE08A),
            )
        } else {
            MetricPalette(
                accent = Color(0xFFCC9A00),
                onAccent = Color(0xFF2B1F00),
                container = Color(0xFFFFE7A3),
                onContainer = Color(0xFF3D2E00),
            )
        }
    }
}
