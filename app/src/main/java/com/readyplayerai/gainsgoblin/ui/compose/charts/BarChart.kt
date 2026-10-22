package com.readyplayerai.gainsgoblin.ui.compose.charts

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** One labeled bar in [BarChart]. */
data class BarEntryData(val label: String, val value: Float)

/**
 * Draws an animated bar chart with horizontal gridlines, y-axis tick labels
 * and a centered label under each bar.
 */
@Composable
fun BarChart(entries: List<BarEntryData>, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = MaterialTheme.colorScheme.primary
    val labelColorArgb = labelColor.toArgb()

    val reveal = remember(entries) { Animatable(0f) }
    LaunchedEffect(entries) {
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    Canvas(modifier.fillMaxWidth().height(200.dp)) {
        if (entries.isEmpty()) return@Canvas

        var maxValue = 0f
        for (entry in entries) {
            if (entry.value > maxValue) maxValue = entry.value
        }
        if (maxValue <= 0f) return@Canvas

        val fraction = reveal.value

        val plotLeft = 36.dp.toPx()
        val plotRight = size.width
        val plotTop = 4.dp.toPx()
        val plotBottom = size.height - 20.dp.toPx()
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop
        if (plotW <= 0f || plotH <= 0f) return@Canvas

        val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11.sp.toPx()
            textAlign = Paint.Align.RIGHT
            color = labelColorArgb
        }

        val fracs = floatArrayOf(0.25f, 0.5f, 0.75f, 1f)
        for (frac in fracs) {
            val y = plotBottom - plotH * frac
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    formatValue(maxValue * frac),
                    plotLeft - 6.dp.toPx(),
                    y,
                    yLabelPaint,
                )
            }
        }

        val slotWidth = plotW / entries.size
        val barWidth = slotWidth * 0.6f

        val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            color = labelColorArgb
        }

        for ((index, entry) in entries.withIndex()) {
            val normalizedHeight = (entry.value / maxValue) * plotH * fraction
            if (normalizedHeight <= 0f) continue

            val x = plotLeft + index * slotWidth + (slotWidth - barWidth) / 2f
            val barTop = plotBottom - normalizedHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, barTop),
                size = Size(barWidth, plotBottom - barTop),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    entry.label,
                    x + barWidth / 2f,
                    plotBottom + 14.dp.toPx(),
                    xLabelPaint,
                )
            }
        }
    }
}

private fun formatValue(v: Float): String =
    if (v < 1000f) v.toInt().toString() else String.format(Locale.US, "%.1fk", v / 1000f)
