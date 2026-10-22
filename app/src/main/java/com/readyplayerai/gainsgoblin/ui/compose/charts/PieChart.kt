package com.readyplayerai.gainsgoblin.ui.compose.charts

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** One labeled, colored wedge in [PieChart]. */
data class PieSliceData(val label: String, val value: Float, val color: Color)

/**
 * Draws an animated pie chart, revealing each wedge clockwise from 12 o'clock
 * with a small gap between slices and the value printed inside larger slices.
 */
@Composable
fun PieChart(slices: List<PieSliceData>, modifier: Modifier = Modifier) {
    val reveal = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    Box(modifier.fillMaxWidth().height(220.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            if (slices.isEmpty()) return@Canvas

            var total = 0f
            for (slice in slices) {
                total += slice.value
            }
            if (total <= 0f) return@Canvas

            val fraction = reveal.value

            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f - 4.dp.toPx()
            if (radius <= 0f) return@Canvas

            val gapAngle = 3f
            val totalSliceAngle = 360f - gapAngle * slices.size
            val animatedSliceAngle = totalSliceAngle * fraction

            val nativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 15.sp.toPx()
            }

            var currentAngle = -90f
            var drawnSliceAngle = 0f

            for (slice in slices) {
                val proportionalAngle = (slice.value / total) * totalSliceAngle
                val remaining = animatedSliceAngle - drawnSliceAngle
                if (remaining <= 0.001f) break

                val drawSweep = minOf(proportionalAngle, remaining)
                if (drawSweep > 0f) {
                    drawArc(
                        brush = SolidColor(slice.color),
                        startAngle = currentAngle,
                        sweepAngle = drawSweep,
                        useCenter = true,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                    )

                    if (drawSweep >= 15f) {
                        val centroidAngleRad = Math.toRadians((currentAngle + drawSweep / 2f).toDouble())
                        val textRadius = radius * 0.6f
                        val tx = cx + textRadius * cos(centroidAngleRad).toFloat()
                        val ty = cy + textRadius * sin(centroidAngleRad).toFloat()

                        val text = slice.value.toInt().toString()
                        val textWidth = nativePaint.measureText(text)
                        val luminance = 0.299f * slice.color.red +
                            0.587f * slice.color.green +
                            0.114f * slice.color.blue
                        nativePaint.color = if (luminance > 0.6f) 0xFF191C20.toInt() else 0xFFFFFFFF.toInt()

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                text,
                                tx - textWidth / 2f,
                                ty - nativePaint.textSize / 3f,
                                nativePaint,
                            )
                        }
                    }
                }

                currentAngle += proportionalAngle + gapAngle
                drawnSliceAngle += proportionalAngle
            }
        }
    }
}

/**
 * Vertical legend for [PieChart]; [markerContent] draws the per-slice color
 * marker while this composable supplies the label and value text.
 */
@Composable
fun PieLegend(
    slices: List<PieSliceData>,
    markerContent: @Composable (slice: PieSliceData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        for (slice in slices) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                markerContent(slice)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${slice.label} · ${slice.value.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
