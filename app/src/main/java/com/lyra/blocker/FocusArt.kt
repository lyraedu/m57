// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/** Original scalable illustrations. Decorative motion stops with view visibility or detachment. */
class FocusArt @JvmOverloads constructor(context: Context, private val scene: Scene = Scene.FOCUS) : View(context) {
    enum class Scene { FOCUS, SCROLL, BROWSE, INDEPENDENT }
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wordFace = android.graphics.Typeface.create(context.resources.getFont(R.font.sora), 600, false)
    private val oval = RectF()
    private var animator: ValueAnimator? = null
    private var phase = 0f
    var showMark = true
    var resting = false
        set(value) { if (field != value) { field = value; invalidate() } }
    var progress = 0.64f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    private fun animation() {
        animator?.cancel(); animator = null
        if (!isAttachedToWindow || windowVisibility != VISIBLE || !isShown || !Motion.enabled(context)) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 9000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }; start()
        }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); animation() }
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); animation() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) { super.onVisibilityChanged(changedView, visibility); animation() }
    override fun onDetachedFromWindow() { animator?.cancel(); animator = null; animate().cancel(); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        canvas.save()
        val scale = minOf(width / 320f, height / 250f)
        canvas.translate(width / 2f, height / 2f); canvas.scale(scale, scale)
        pen.style = Paint.Style.FILL; pen.color = if (resting) LyraUi.mossSoft else LyraUi.soft
        canvas.drawCircle(0f, 0f, 110f, pen)
        pen.style = Paint.Style.STROKE; pen.strokeWidth = 1f; pen.color = Color.rgb(226, 207, 190)
        canvas.drawCircle(0f, 0f, 108f, pen); canvas.drawCircle(0f, 0f, 90f, pen)
        when (scene) {
            Scene.FOCUS -> {
                pen.strokeWidth = 14f; pen.strokeCap = Paint.Cap.ROUND; pen.color = Color.rgb(238, 215, 195)
                canvas.drawCircle(0f, 0f, 71f, pen)
                pen.color = if (resting) LyraUi.moss else LyraUi.orange; oval.set(-71f, -71f, 71f, 71f)
                canvas.drawArc(oval, -90f, progress * 360f, false, pen)
                pen.style = Paint.Style.FILL; pen.textAlign = Paint.Align.CENTER; pen.color = LyraUi.ink
                pen.typeface = wordFace; pen.textSize = 33f
                if (showMark) canvas.drawText("m57", 0f, 10f, pen)
            }
            Scene.SCROLL -> {
                pen.style = Paint.Style.FILL; pen.color = LyraUi.ink
                canvas.drawRoundRect(-45f, -84f, 45f, 84f, 15f, 15f, pen)
                pen.color = LyraUi.background; canvas.drawRoundRect(-39f, -72f, 39f, 72f, 10f, 10f, pen)
                val offset = sin(phase * Math.PI * 2).toFloat() * 7f
                pen.color = Color.rgb(230, 185, 153); canvas.drawRoundRect(-31f, -53f + offset, 31f, 35f + offset, 8f, 8f, pen)
                pen.color = LyraUi.orange; canvas.drawCircle(37f, 45f, 27f, pen)
                pen.color = Color.WHITE; canvas.drawRoundRect(28f, 34f, 33f, 56f, 2f, 2f, pen); canvas.drawRoundRect(40f, 34f, 45f, 56f, 2f, 2f, pen)
            }
            Scene.BROWSE -> {
                pen.style = Paint.Style.FILL; pen.color = Color.WHITE
                canvas.drawRoundRect(-80f, -57f, 80f, 57f, 14f, 14f, pen)
                pen.color = LyraUi.line; canvas.drawRoundRect(-68f, -43f, 68f, -22f, 7f, 7f, pen)
                pen.color = LyraUi.orange; canvas.drawCircle(0f, 16f, 26f, pen)
                pen.style = Paint.Style.STROKE; pen.strokeWidth = 5f; pen.strokeCap = Paint.Cap.ROUND; pen.color = Color.WHITE
                canvas.drawLine(-12f, 16f, -3f, 25f, pen); canvas.drawLine(-3f, 25f, 14f, 7f, pen)
            }
            Scene.INDEPENDENT -> {
                pen.strokeWidth = 10f; pen.color = LyraUi.orange
                canvas.drawCircle(-24f, 0f, 46f, pen); pen.color = Color.rgb(107, 143, 107); canvas.drawCircle(30f, 0f, 30f, pen)
            }
        }
        pen.style = Paint.Style.FILL; pen.color = LyraUi.orange
        val angle = phase * Math.PI * 2
        canvas.drawCircle((cos(angle) * 108).toFloat(), (sin(angle) * 108).toFloat(), 5f, pen)
        pen.color = Color.rgb(107, 143, 107)
        canvas.drawCircle((cos(angle + 2.4) * 91).toFloat(), (sin(angle + 2.4) * 91).toFloat(), 4f, pen)
        canvas.restore()
    }
}
