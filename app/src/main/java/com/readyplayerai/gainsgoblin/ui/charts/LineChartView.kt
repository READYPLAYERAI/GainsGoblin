package com.readyplayerai.gainsgoblin.ui.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat

import com.readyplayerai.gainsgoblin.R
import java.lang.Math
import java.util.ArrayList

/**
 * Callback interface invoked when the user taps near a data point.
 */
interface OnPointSelectedListener {
    fun onPointSelected(index: Int, x: Float, y: Float)
}

/**
 * A custom Android View that renders a polyline chart with circles, horizontal
 * grid lines, subsampled X-axis labels, a reveal animation, and touch
 * interactions (pinch-zoom, drag-pan, tap-to-select).
 *
 * The view receives pre-formatted X labels and sequential index-based X values;
 * it performs NO date parsing or formatting.
 *
 * Reveal animation: cuts at the last complete segment (no partial-segment
 * interpolation). Grid lines: 4 lines at 20%, 40%, 60%, 80% of plot height.
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // -----------------------------------------------------------------------
    // Data
    // -----------------------------------------------------------------------
    private var points: List<ChartPoint> = ArrayList()
    private var xLabels: List<String> = ArrayList()

    // -----------------------------------------------------------------------
    // Zoom / Pan state
    // -----------------------------------------------------------------------
    private var scaleX: Float = 1f
    private var translateX: Float = 0f

    // -----------------------------------------------------------------------
    // Reveal animation
    // -----------------------------------------------------------------------
    private var animator: ValueAnimator? = null
    private var animationFraction: Float = 1f

    // -----------------------------------------------------------------------
    // Drag-pan tracking (single-finger)
    // -----------------------------------------------------------------------
    private var lastDragX: Float? = null

    // -----------------------------------------------------------------------
    // Listener
    // -----------------------------------------------------------------------
    private var onPointSelectedListener: OnPointSelectedListener? = null

    // -----------------------------------------------------------------------
    // Paints (created once; dynamic properties set per frame)
    // -----------------------------------------------------------------------
    private val gridPaint = Paint().apply {
        isAntiAlias = true
        color = Color.LTGRAY
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val circlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
    }

    // Lazy-loaded primary colour (ContextCompat.getColor needs a Context)
    private var primaryColor: Int = 0

    private fun getPrimaryColor(): Int {
        if (primaryColor == 0) {
            primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)
        }
        return primaryColor
    }

    // -----------------------------------------------------------------------
    // Gesture detectors (lazy)
    // -----------------------------------------------------------------------
    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Replaces the chart data. Stores defensive copies, resets zoom/pan,
     * cancels any running animation, and starts a fresh reveal animation.
     *
     * @param points  sequential data points (x is the index, y is the value)
     * @param xLabels pre-formatted labels, may be empty or shorter than points
     */
    fun setData(points: List<ChartPoint>, xLabels: List<String>) {
        this.points = ArrayList(points)
        this.xLabels = ArrayList(xLabels)

        // Reset zoom / pan
        scaleX = 1f
        translateX = 0f

        // Cancel existing animation
        animator?.cancel()
        animator = null

        // Start reveal animation
        startRevealAnimation()
        invalidate()
    }

    /**
     * Registers a listener that is called when the user taps a data point.
     */
    fun setOnPointSelectedListener(listener: OnPointSelectedListener?) {
        onPointSelectedListener = listener
    }

    /**
     * Resets the view to 1× zoom with no horizontal offset.
     */
    fun resetZoom() {
        scaleX = 1f
        translateX = 0f
        invalidate()
    }

    // -----------------------------------------------------------------------
    // Animation helpers
    // -----------------------------------------------------------------------

    private fun startRevealAnimation() {
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.setDuration(700L)
        anim.setInterpolator(AccelerateDecelerateInterpolator())
        anim.addUpdateListener { a: ValueAnimator ->
            animationFraction = a.animatedFraction
            invalidate()
        }
        animator = anim
        animationFraction = 0f
        anim.start()
    }

    // -----------------------------------------------------------------------
    // View lifecycle
    // -----------------------------------------------------------------------

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    // -----------------------------------------------------------------------
    // Touch handling
    // -----------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Forward to gesture detectors for classification
        gestureDetectorD()?.onTouchEvent(event)
        scaleGestureDetectorD()?.onTouchEvent(event)

        val action = event.getActionMasked()
        val pointerCount = event.getPointerCount()

        if (action == MotionEvent.ACTION_DOWN && pointerCount == 1) {
            lastDragX = event.getX()
        } else if (action == MotionEvent.ACTION_MOVE && pointerCount == 1
                && scaleGestureDetectorD()?.isInProgress() != true) {
            val prevX = lastDragX
            if (prevX != null) {
                val currentX = event.getX()
                translateX += currentX - prevX
                lastDragX = currentX
                clampTranslate()
                invalidate()
            }
        } else if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            lastDragX = null
        }

        // When a secondary pointer is lifted leaving exactly one pointer,
        // update lastDragX so a subsequent single-finger drag is smooth.
        if (action == MotionEvent.ACTION_POINTER_UP && pointerCount == 2) {
            val remainingIndex = 1 - event.getActionIndex()
            lastDragX = event.getX(remainingIndex)
        }

        performClick()
        return true
    }

    // -----------------------------------------------------------------------
    // Drawing
    // -----------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val scaledDensity = resources.displayMetrics.scaledDensity

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // ----- layout --------------------------------------------------------
        val topPad = 16f * density
        val sidePad = 16f * density
        val bottomPad = 16f * density
        val labelHeight = 28f * density

        val plotLeft = sidePad
        val plotTop = topPad
        val plotRight = w - sidePad
        val plotBottom = h - bottomPad - labelHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        if (plotWidth <= 0f || plotHeight <= 0f) return

        // ----- 1. grid lines ------------------------------------------------
        // 4 horizontal lines at 20 %, 40 %, 60 %, 80 % of plot height.
        gridPaint.strokeWidth = 1f * density
        for (i in 0 until 4) {
            val fraction = (i + 1).toFloat() * 0.2f
            val y = plotBottom - fraction * plotHeight
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
        }

        val n = points.size
        if (n == 0) return

        // ----- 2. Y auto-fit (pad 10 %, handle zero-range) ------------------
        var minY = points[0].y
        var maxY = points[0].y
        for (i in 1 until n) {
            val y = points[i].y
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        var yRange = maxY - minY
        if (yRange == 0f) {
            // Fallback: [-1, +1] padded by 10 % → [-1.1, +1.1]
            yRange = 2.2f
            minY = -1.1f
            maxY = 1.1f
        } else {
            val pad = yRange * 0.1f
            minY -= pad
            maxY += pad
            yRange += 2f * pad
        }

        // ----- 3. primary colour for line + circles -------------------------
        val primColor = getPrimaryColor()
        linePaint.color = primColor
        linePaint.strokeWidth = 3f * density
        circlePaint.color = primColor

        // ----- 4. X spacing -------------------------------------------------
        val stepSize: Float
        if (n > 1) {
            stepSize = plotWidth / (n - 1).toFloat()
        } else {
            stepSize = 0f
        }

        // ----- 5. reveal index (cut at last complete segment) ---------------
        val revealedEnd: Int
        if (n > 1) {
            revealedEnd = (animationFraction * (n - 1).toFloat()).toInt()
        } else {
            revealedEnd = if (animationFraction >= 1f) 0 else -1
        }
        if (revealedEnd < 0) return

        // ----- 6. polyline --------------------------------------------------
        if (n > 1) {
            val mapY = { y: Float -> plotBottom - (y - minY) / yRange * plotHeight }
            for (i in 0 until revealedEnd) {
                val x1 = plotLeft + stepSize * i.toFloat() * scaleX + translateX
                val y1 = mapY(points[i].y)
                val x2 = plotLeft + stepSize * (i + 1).toFloat() * scaleX + translateX
                val y2 = mapY(points[i + 1].y)
                canvas.drawLine(x1, y1, x2, y2, linePaint)
            }
        }

        // ----- 7. circles ---------------------------------------------------
        val circleRadius = 2f * density
        val revealCount = if (n > 1) {
            revealedEnd + 1
        } else {
            if (revealedEnd >= 0) 1 else 0
        }
        for (i in 0 until revealCount) {
            val cx: Float
            if (n > 1) {
                cx = plotLeft + stepSize * i.toFloat() * scaleX + translateX
            } else {
                cx = plotLeft + plotWidth / 2f * scaleX + translateX
            }
            val cy = plotBottom - (points[i].y - minY) / yRange * plotHeight
            canvas.drawCircle(cx, cy, circleRadius, circlePaint)
        }

        // ----- 8. X labels (subsampled) -------------------------------------
        labelPaint.textSize = 10f * scaledDensity
        drawSubsampledLabels(
            canvas, plotLeft, plotRight, plotWidth,
            stepSize, n, revealedEnd, density, plotBottom, labelPaint
        )
    }

    // -----------------------------------------------------------------------
    // Label subsampling
    // -----------------------------------------------------------------------

    /**
     * Draws at most 5 X-axis labels that fall inside the visible viewport.
     * Indices beyond [xLabels) or beyond [revealedEnd) are skipped.
     * Always draws the last visible label if the regular step misses it.
     */
    private fun drawSubsampledLabels(
        canvas: Canvas,
        plotLeft: Float, plotRight: Float,
        plotWidth: Float,
        stepSize: Float,
        n: Int,
        revealedEnd: Int,
        density: Float,
        plotBottom: Float,
        paint: Paint
    ) {
        if (n == 0) return

        val margin = 20f * density
        val maxLabelIdx = Math.min(xLabels.size - 1, revealedEnd)
        if (maxLabelIdx < 0) return

        val visibleIndices = ArrayList<Int>()
        for (i in 0 until n) {
            if (i > maxLabelIdx) break
            if (i >= xLabels.size) break
            val sx: Float
            if (n > 1) {
                sx = plotLeft + stepSize * i.toFloat() * scaleX + translateX
            } else {
                sx = plotLeft + plotWidth / 2f * scaleX + translateX
            }
            if (sx >= plotLeft - margin && sx <= plotRight + margin) {
                visibleIndices.add(i)
            }
        }

        val visibleCount = visibleIndices.size
        if (visibleCount == 0) return

        val maxLabels = 5
        var labelStep = 1
        var lastIncluded = -1

        if (visibleCount > maxLabels) {
            labelStep = (visibleCount + maxLabels - 1) / maxLabels
            if (labelStep < 1) labelStep = 1
            lastIncluded = visibleIndices[visibleCount - 1]
        }

        val labelY = plotBottom + 4f * density + paint.textSize

        for (j in 0 until visibleCount step labelStep) {
            val idx = visibleIndices[j]
            if (idx <= maxLabelIdx) {
                val label = xLabels[idx]
                if (label.isNotEmpty()) {
                    drawLabel(canvas, label, idx, plotLeft, plotWidth, stepSize, n, labelY, paint)
                }
            }
        }

        if (lastIncluded >= 0 && visibleCount > 0) {
            val lastIdx = lastIncluded
            val alreadyCovered = (visibleCount - 1) % labelStep == 0
            if (!alreadyCovered && lastIdx <= maxLabelIdx && lastIdx < xLabels.size) {
                val label = xLabels[lastIdx]
                if (label.isNotEmpty()) {
                    drawLabel(canvas, label, lastIdx, plotLeft, plotWidth, stepSize, n, labelY, paint)
                }
            }
        }
    }

    private fun drawLabel(
        canvas: Canvas,
        label: String,
        index: Int,
        plotLeft: Float, plotWidth: Float,
        stepSize: Float,
        n: Int,
        y: Float,
        paint: Paint
    ) {
        val sx: Float
        if (n > 1) {
            sx = plotLeft + stepSize * index.toFloat() * scaleX + translateX
        } else {
            sx = plotLeft + plotWidth / 2f * scaleX + translateX
        }
        val textWidth = paint.measureText(label)
        canvas.drawText(label, sx - textWidth / 2f, y, paint)
    }

    // -----------------------------------------------------------------------
    // Tap handling (called from GestureDetector callback)
    // -----------------------------------------------------------------------

    private fun handleTap(e: MotionEvent) {
        if (points.isEmpty() || onPointSelectedListener == null) return

        val density = resources.displayMetrics.density
        val tapX = e.getX()
        val tapY = e.getY()

        // Replicate layout calculations (same as onDraw)
        val w = width.toFloat()
        val h = height.toFloat()
        val topPad = 16f * density
        val sidePad = 16f * density
        val bottomPad = 16f * density
        val labelHeight = 28f * density
        val plotLeft = sidePad
        val plotTop = topPad
        val plotBottom = h - bottomPad - labelHeight
        val plotWidth = w - 2f * sidePad
        val plotHeight = plotBottom - plotTop
        if (plotWidth <= 0f) return

        val n = points.size
        val stepSize = if (n > 1) plotWidth / (n - 1).toFloat() else 0f

        // Y auto-fit
        var minY = points[0].y
        var maxY = points[0].y
        for (i in 1 until n) {
            val y = points[i].y
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        var yRange = maxY - minY
        if (yRange == 0f) {
            yRange = 2.2f
            minY = -1.1f
            maxY = 1.1f
        } else {
            val pad = yRange * 0.1f
            minY -= pad
            maxY += pad
            yRange += 2f * pad
        }

        val tapThreshold = 24f * density
        val tapThresholdSq = tapThreshold * tapThreshold
        var bestIdx = -1
        var bestDistSq = Float.MAX_VALUE

        for (i in 0 until n) {
            val cx: Float
            if (n > 1) {
                cx = plotLeft + stepSize * i.toFloat() * scaleX + translateX
            } else {
                cx = plotLeft + plotWidth / 2f * scaleX + translateX
            }
            val cy = plotBottom - (points[i].y - minY) / yRange * plotHeight
            val dx = tapX - cx
            val dy = tapY - cy
            val distSq = dx * dx + dy * dy
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestIdx = i
            }
        }

        if (bestIdx >= 0 && bestDistSq <= tapThresholdSq) {
            val pt = points[bestIdx]
            onPointSelectedListener?.onPointSelected(bestIdx, pt.x, pt.y)
        }
    }



    // -----------------------------------------------------------------------
    // Zoom / pan clamping
    // -----------------------------------------------------------------------

    /**
     * Clamps [translateX] so data can never scroll entirely off-screen.
     * When the data fits in the viewport, it is centred.
     */
    private fun clampTranslate() {
        val density = resources.displayMetrics.density
        val sidePad = 16f * density
        val w = width.toFloat()
        val plotWidth = w - 2f * sidePad
        if (plotWidth <= 0f) {
            translateX = 0f
            return
        }

        val n = points.size
        if (n <= 1) {
            // Single point or empty — centre its position.
            translateX = 0f
            return
        }

        val dataWidth = plotWidth * scaleX
        if (dataWidth <= plotWidth) {
            // Data fits — centre horizontally.
            translateX = (plotWidth - dataWidth) / 2f
        } else {
            // Data is wider than the viewport — clamp edges.
            if (translateX > 0f) translateX = 0f
            val minTx = plotWidth - dataWidth
            if (translateX < minTx) translateX = minTx
        }
    }

    // -----------------------------------------------------------------------
    // Pinch-zoom handler
    // -----------------------------------------------------------------------

    private fun handlePinchZoom(detector: ScaleGestureDetector) {
        val prevSpanX = detector.getPreviousSpanX()
        if (prevSpanX <= 0f) return

        val factor = detector.getCurrentSpanX() / prevSpanX
        if (factor <= 0f) return

        val density = resources.displayMetrics.density
        val sidePad = 16f * density
        val plotLeft = sidePad

        val focusX = detector.getFocusX()
        val focusXRel = focusX - plotLeft

        // Data coordinate at the focal point before scaling
        val dataFocus = (focusXRel - translateX) / scaleX

        var newScale = scaleX * factor
        if (newScale < 1f) newScale = 1f
        if (newScale > 10f) newScale = 10f

        // Adjust translation so the focal point stays fixed on screen
        translateX = focusXRel - dataFocus * newScale

        scaleX = newScale
        clampTranslate()
        invalidate()
    }

    // -----------------------------------------------------------------------
    // Lazy gesture-detector initialisation
    // -----------------------------------------------------------------------

    private fun gestureDetectorD(): GestureDetector {
        if (gestureDetector == null) {
            gestureDetector = GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        handleTap(e)
                        return true
                    }
                }
            )
        }
        return gestureDetector!!
    }

    private fun scaleGestureDetectorD(): ScaleGestureDetector {
        if (scaleGestureDetector == null) {
            scaleGestureDetector = ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        handlePinchZoom(detector)
                        return true
                    }
                    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                        return true
                    }
                    override fun onScaleEnd(detector: ScaleGestureDetector) {
                        // No-op
                    }
                }
            )
        }
        return scaleGestureDetector!!
    }
}