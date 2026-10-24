package com.readyplayerai.gainsgoblin.ui.compose.progress

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Decorative line-and-area sparkline for a list of [values]. Renders a rounded
 * polyline with an optional gradient fill and an optional dot on the last point.
 * Draws nothing when there are fewer than two values.
 */
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    showArea: Boolean = true,
    showLastDot: Boolean = true,
    strokeWidth: Dp = 2.4.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    dotInnerColor: Color = MaterialTheme.colorScheme.surface,
) {
    if (values.size < 2) return

    Canvas(modifier) {
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 0f
        val padded = 3.dp.toPx()
        val usableHeight = size.height - padded * 2f

        if (max - min < 0.0001f) {
            drawLine(
                color = lineColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
            return@Canvas
        }

        val dx = size.width / (values.size - 1)
        fun x(index: Int): Float = dx * index
        fun y(value: Float): Float = padded + (1f - (value - min) / (max - min)) * usableHeight

        val line = Path()
        line.moveTo(x(0), y(values[0]))
        for (index in 1 until values.size) {
            line.lineTo(x(index), y(values[index]))
        }

        if (showArea) {
            val area = Path()
            area.moveTo(x(0), y(values[0]))
            for (index in 1 until values.size) {
                area.lineTo(x(index), y(values[index]))
            }
            area.lineTo(x(values.size - 1), size.height)
            area.lineTo(0f, size.height)
            area.close()
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)),
                    startY = 0f,
                    endY = size.height,
                ),
            )
        }

        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        if (showLastDot) {
            val last = Offset(x(values.size - 1), y(values[values.size - 1]))
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = last)
            drawCircle(color = dotInnerColor, radius = 2.dp.toPx(), center = last)
        }
    }
}
