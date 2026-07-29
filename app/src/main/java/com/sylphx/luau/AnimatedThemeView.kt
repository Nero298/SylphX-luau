package com.sylphx.luau

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lightweight animated gradient view. Not a literal particle engine per
 * theme (20 bespoke animations would be a lot of surface area to maintain),
 * but every theme gets its own moving gradient sweep plus a handful of
 * drifting accent-colored dots, which reads as "alive" both in the small
 * preview chips and as a full-screen background.
 */
class AnimatedThemeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var theme: AppTheme = AppTheme.DEFAULT
    private var phase = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Dot(val seedX: Float, val seedY: Float, val speed: Float, val radius: Float)
    private val dots = List(6) {
        Dot(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat() * 0.6f, 2f + Random.nextFloat() * 3f)
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 6000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    fun setTheme(newTheme: AppTheme) {
        theme = newTheme
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val shift = sin(phase * 2 * Math.PI).toFloat() * w * 0.3f
        paint.shader = LinearGradient(
            -shift, 0f, w - shift, h,
            theme.colorStart, theme.colorEnd,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)

        dotPaint.color = theme.dotColor
        dots.forEach { dot ->
            val t = (phase * dot.speed) % 1f
            val x = dot.seedX * w
            val y = ((dot.seedY + t) % 1f) * h
            val alpha = (255 * (0.3f + 0.5f * sin(t * Math.PI).toFloat())).toInt().coerceIn(0, 255)
            dotPaint.alpha = alpha
            canvas.drawCircle(x, y, dot.radius, dotPaint)
        }
    }
}
