package com.readyplayerai.gainsgoblin.ui.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

import java.lang.Math
import java.util.ArrayList

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Float> = ArrayList()
    private var animator: ValueAnimator? = null
    private var animationFraction: Float = 1f

    private val barPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun setData(values: List<Float>) {
        this.values = ArrayList(values)
        animator?.cancel()

        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.setDuration(500L)
        anim.setInterpolator(DecelerateInterpolator())
        anim.addUpdateListener { a: ValueAnimator ->
            animationFraction = a.animatedFraction
            invalidate()
        }
        animator = anim
        animationFraction = 0f
        anim.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density

        if (values.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Find max value, guard against zero
        var maxValue = 0f
        for (i in 0 until values.size) {
            val v = values[i]
            if (v > maxValue) maxValue = v
        }
        if (maxValue <= 0f) return

        val topPadding = 4f * density
        val bottom = h
        val availableHeight = bottom - topPadding
        val numBars = values.size
        val slotWidth = w / numBars.toFloat()
        val barWidth = slotWidth * 0.6f
        val barSpacing = slotWidth - barWidth

        for (i in 0 until numBars) {
            val color = ChartColors.JOYFUL_COLORS[i % ChartColors.JOYFUL_COLORS.size]
            barPaint.color = color

            val normalizedHeight = (values[i] / maxValue) * availableHeight * animationFraction
            if (normalizedHeight <= 0f) continue

            val x = i.toFloat() * slotWidth + barSpacing / 2f
            val barTop = bottom - normalizedHeight
            val rect = RectF(x, barTop, x + barWidth, bottom)
            canvas.drawRect(rect, barPaint)
        }
    }
}