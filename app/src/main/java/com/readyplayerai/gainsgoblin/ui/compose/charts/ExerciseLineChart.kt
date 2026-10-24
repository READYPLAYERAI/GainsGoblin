package com.readyplayerai.gainsgoblin.ui.compose.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** One labeled point in [ExerciseLineChart]. */
data class ChartPoint(
    val label: String,      // x-axis label
    val value: Float,
    val isPr: Boolean,
    val dateLabel: String,  // tooltip date, e.g. "19 Sep"
    val detail: String,     // tooltip detail, e.g. "107.5 kg × 3 · 5 sets"
    val prLabel: String,    // tooltip PR chip text, e.g. "PR"
)

private data class ChartScale(
    val lo: Float,
    val hi: Float,
    val step: Float,
    val ticks: List<Float>,
)

/**
 * Draws a smooth line/area progression chart with horizontal gridlines, y-axis
 * tick labels, PR rings and a scrub tooltip. Renders [emptyText] when [points]
 * is empty.
 */
@Composable
fun ExerciseLineChart(
    points: List<ChartPoint>,
    accent: Color,
    prRingColor: Color,
    valueFormatter: (Float) -> String,
    axisFormatter: (Float) -> String,
    selectedIndex: Int?,
    onSelectedIndexChange: (Int?) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    var chartSize by remember { mutableStateOf(IntSize.Zero) }

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .onSizeChanged { chartSize = it }
                .heightIn(min = 168.dp),
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    val scale = remember(points) { computeScale(points) }
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
            .onSizeChanged { chartSize = it }
            .pointerInput(points) {
                if (points.isEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun pick(position: Offset) {
                        val left = 34.dp.toPx()
                        val right = size.width - 6.dp.toPx()
                        val width = (right - left).coerceAtLeast(1f)
                        val fraction = ((position.x - left) / width).coerceIn(0f, 1f)
                        val index = if (points.size == 1) {
                            0
                        } else {
                            (fraction * (points.size - 1)).roundToInt()
                        }
                        onSelectedIndexChange(index.coerceIn(0, points.size - 1))
                    }
                    pick(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) pick(change.position)
                        if (event.changes.none { it.pressed }) break
                    }
                    onSelectedIndexChange(null)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val plotLeft = 34.dp.toPx()
            val plotRight = size.width - 6.dp.toPx()
            val plotTop = 12.dp.toPx()
            val plotBottom = size.height - 26.dp.toPx()
            val plotW = plotRight - plotLeft
            val plotH = plotBottom - plotTop
            if (plotW <= 0f || plotH <= 0f) return@Canvas

            val span = (scale.hi - scale.lo).let { if (it == 0f) 1f else it }
            fun xFor(index: Int): Float = if (points.size == 1) {
                (plotLeft + plotRight) / 2f
            } else {
                plotLeft + (index / (points.size - 1f)) * plotW
            }

            fun yFor(value: Float): Float =
                plotTop + (1f - (value - scale.lo) / span) * plotH

            // 1. Horizontal dashed grid lines.
            val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
            for (tick in scale.ticks) {
                val y = yFor(tick)
                drawLine(
                    color = outlineVariant.copy(alpha = 0.7f),
                    start = Offset(plotLeft, y),
                    end = Offset(plotRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }

            // 2. Tick labels.
            val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9.sp.toPx()
                textAlign = Paint.Align.RIGHT
                color = labelColor.toArgb()
            }
            for (tick in scale.ticks) {
                val y = yFor(tick)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        axisFormatter(tick),
                        plotLeft - 6.dp.toPx(),
                        y + (yLabelPaint.ascent() + yLabelPaint.descent()) / -2f,
                        yLabelPaint,
                    )
                }
            }

            // 3. Area under the line.
            val curve = Path()
            curve.moveTo(xFor(0), yFor(points[0].value))
            for (index in 1 until points.size) {
                val x0 = xFor(index - 1)
                val y0 = yFor(points[index - 1].value)
                val x1 = xFor(index)
                val y1 = yFor(points[index].value)
                val midX = (x0 + x1) / 2f
                curve.cubicTo(midX, y0, midX, y1, x1, y1)
            }
            val area = Path().apply {
                addPath(curve)
                lineTo(xFor(points.size - 1), plotBottom)
                lineTo(xFor(0), plotBottom)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0f)),
                    startY = plotTop,
                    endY = plotBottom,
                ),
            )

            // 4. Smooth line.
            drawPath(
                path = curve,
                color = accent,
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // 5. Point dots.
            for (index in points.indices) {
                drawCircle(
                    color = accent.copy(alpha = 0.55f),
                    radius = 2.4.dp.toPx(),
                    center = Offset(xFor(index), yFor(points[index].value)),
                )
            }

            // 6. PR rings.
            for (index in points.indices) {
                if (!points[index].isPr) continue
                val center = Offset(xFor(index), yFor(points[index].value))
                drawCircle(
                    color = prRingColor,
                    radius = 5.8.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.3.dp.toPx()),
                )
                drawCircle(
                    color = accent,
                    radius = 3.1.dp.toPx(),
                    center = center,
                )
            }

            // 7. X-axis labels.
            val candidateLabelIndices = if (points.size == 1) {
                listOf(0)
            } else {
                listOf(0, (points.size - 1) / 3, 2 * (points.size - 1) / 3, points.size - 1)
                    .distinct()
            }
            val seenLabels = HashSet<String>()
            val labelIndices = candidateLabelIndices.filter { index ->
                seenLabels.add(points[index].label)
            }
            val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9.sp.toPx()
                color = labelColor.toArgb()
            }
            val lastIndex = points.size - 1
            for (index in labelIndices) {
                xLabelPaint.textAlign = when {
                    index == 0 -> Paint.Align.LEFT
                    index == lastIndex -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        points[index].label,
                        xFor(index),
                        size.height - 7.dp.toPx(),
                        xLabelPaint,
                    )
                }
            }

            // 8. Selected-point indicator.
            if (selectedIndex != null && selectedIndex in points.indices) {
                val x = xFor(selectedIndex)
                val y = yFor(points[selectedIndex].value)
                drawLine(
                    color = labelColor.copy(alpha = 0.5f),
                    start = Offset(x, plotTop),
                    end = Offset(x, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(color = accent, radius = 4.6.dp.toPx(), center = Offset(x, y))
                drawCircle(
                    color = surfaceContainerHigh,
                    radius = 4.6.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 2.2.dp.toPx()),
                )
            }
        }

        val selected = selectedIndex?.takeIf { it in points.indices }
        if (selected != null && chartSize.width > 0 && chartSize.height > 0) {
            val point = points[selected]
            val density = LocalDensity.current
            val plotLeft = with(density) { 34.dp.toPx() }
            val plotRight = chartSize.width - with(density) { 6.dp.toPx() }
            val plotTop = with(density) { 12.dp.toPx() }
            val plotBottom = chartSize.height - with(density) { 26.dp.toPx() }
            val plotW = plotRight - plotLeft
            val plotH = plotBottom - plotTop
            val span = (scale.hi - scale.lo).let { if (it == 0f) 1f else it }
            val pointX = if (points.size == 1) {
                (plotLeft + plotRight) / 2f
            } else {
                plotLeft + (selected / (points.size - 1f)) * plotW
            }
            val pointY = plotTop + (1f - (point.value - scale.lo) / span) * plotH

            var tipSize by remember { mutableStateOf(IntSize.Zero) }
            val padPx = with(density) { 4.dp.toPx() }
            val minX = padPx.roundToInt()
            val maxX = (chartSize.width - tipSize.width - padPx)
                .roundToInt()
                .coerceAtLeast(4)
            val x = (pointX - tipSize.width / 2f).roundToInt().coerceIn(minX, maxX)
            val y = (pointY - tipSize.height - with(density) { 6.dp.toPx() })
                .roundToInt()
                .coerceAtLeast(2)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .width(144.dp)
                    .onSizeChanged { tipSize = it }
                    .offset { IntOffset(x, y) },
            ) {
                val onInverse = MaterialTheme.colorScheme.inverseOnSurface
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(
                        text = point.dateLabel.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = onInverse.copy(alpha = 0.7f),
                    )
                    Text(
                        text = valueFormatter(point.value),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = onInverse,
                    )
                    Text(
                        text = point.detail,
                        fontSize = 10.5.sp,
                        color = onInverse.copy(alpha = 0.75f),
                    )
                    if (point.isPr) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(
                                text = point.prLabel.uppercase(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.7.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun computeScale(points: List<ChartPoint>): ChartScale {
    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }
    var edge = (maxValue - minValue) * 0.08f
    if (edge == 0f) edge = max(1f, maxValue * 0.04f)

    val rawStep = (maxValue - minValue + 2 * edge) / 4f
    val magnitude = 10f.pow(floor(log10(rawStep)))
    val normalized = if (magnitude == 0f) 1f else rawStep / magnitude
    val step = when {
        normalized <= 1f -> 1f
        normalized <= 2f -> 2f
        normalized <= 5f -> 5f
        else -> 10f
    } * magnitude

    val lo = floor((minValue - edge) / step) * step
    var hi = ceil((maxValue + edge) / step) * step
    if (hi <= lo) hi = lo + step

    val ticks = ArrayList<Float>()
    var value = lo
    var guard = 0
    while (value <= hi + step / 2f && guard < 8) {
        ticks.add(value)
        value += step
        guard++
    }
    return ChartScale(lo = lo, hi = hi, step = step, ticks = ticks)
}
