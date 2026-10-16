package com.readyplayerai.gainsgoblin.ui.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

import java.util.ArrayList

data class PieSlice(val label: String, val value: Float)

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var slices: List<PieSlice> = ArrayList()
    private var sliceSpacingDp: Float = 3f
    private var valueTextSizeSp: Float = 15f
    private var noDataText: String = ""
    private var animator: ValueAnimator? = null
    private var animationFraction: Float = 1f

    private val slicePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val valueTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
    }

    private val noDataPaint = Paint().apply {
        isAntiAlias = true
        color = Color.GRAY
        textSize = 30f
    }

    fun setData(slices: List<PieSlice>) {
        this.slices = ArrayList(slices)
        animator?.cancel()

        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.setDuration(1000L)
        anim.setInterpolator(AccelerateDecelerateInterpolator())
        anim.addUpdateListener { a: ValueAnimator ->
            animationFraction = a.animatedFraction
            invalidate()
        }
        animator = anim
        animationFraction = 0f
        anim.start()
    }

    fun setSliceSpacingDp(dp: Float) {
        sliceSpacingDp = dp
    }

    fun setValueTextSizeSp(sp: Float) {
        valueTextSizeSp = sp
    }

    fun setNoDataText(text: String) {
        noDataText = text
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val scaledDensity = resources.displayMetrics.scaledDensity

        if (slices.isEmpty()) {
            if (noDataText.isNotEmpty()) {
                noDataPaint.textSize = 30f * scaledDensity
                val textWidth = noDataPaint.measureText(noDataText)
                val x = (width.toFloat() - textWidth) / 2f
                val y = height.toFloat() / 2f
                canvas.drawText(noDataText, x, y, noDataPaint)
            }
            return
        }

        var total = 0f
        for (i in 0 until slices.size) {
            total += slices[i].value
        }
        if (total <= 0f) return

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val padding = 4f * density
        val radius = netMin(cx, cy) - padding
        if (radius <= 0f) return

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        valueTextPaint.textSize = valueTextSizeSp * scaledDensity

        val numSlices = slices.size
        val gapAngle: Float = sliceSpacingDp
        val totalGap = gapAngle * numSlices.toFloat()
        val totalSliceAngle = 360f - totalGap
        val animatedSliceAngle = totalSliceAngle * animationFraction
        val degToRad: Float = 0.017453292f

        var currentAngle = -90f
        var drawnSliceAngle = 0f

        for (i in 0 until numSlices) {
            val proportionalAngle = (slices[i].value / total) * totalSliceAngle
            val remaining = animatedSliceAngle - drawnSliceAngle
            if (remaining <= 0.001f) break

            val drawSweep = netMin(proportionalAngle, remaining)
            if (drawSweep > 0f) {
                val color = ChartColors.JOYFUL_COLORS[i % ChartColors.JOYFUL_COLORS.size]
                slicePaint.color = color

                canvas.drawArc(rect, currentAngle, drawSweep, true, slicePaint)

                if (drawSweep >= 15f) {
                    val centroidAngleDeg = currentAngle + drawSweep / 2f
                    val centroidAngleRad = centroidAngleDeg * degToRad
                    val textRadius = radius * 0.6f

                    val cosCent = netCos(centroidAngleRad)
                    val sinCent = netSin(centroidAngleRad)
                    val tx = cx + textRadius * cosCent
                    val ty = cy + textRadius * sinCent

                    val text = slices[i].value.toInt().toString()
                    val textWidth = valueTextPaint.measureText(text)
                    val sinHalf = netSin(drawSweep / 2f * degToRad)
                    val chordLength = 2f * radius * sinHalf

                    if (textWidth <= chordLength * 0.9f) {
                        canvas.drawText(text, tx - textWidth / 2f, ty - valueTextPaint.textSize / 3f, valueTextPaint)
                    }
                }
            }

            currentAngle += proportionalAngle + gapAngle
            drawnSliceAngle += proportionalAngle
        }
    }

    private fun netCos(v: Float): Float {
        return java.lang.Math.cos(v.toDouble()).toFloat()
    }

    private fun netSin(v: Float): Float {
        return java.lang.Math.sin(v.toDouble()).toFloat()
    }

    private fun netMin(a: Float, b: Float): Float {
        if (a < b) return a
        return b
    }
}