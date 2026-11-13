// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** A round-by-round visual index; the adjacent phase label carries the accessible text. */
class FocusRounds(context: Context, private val count: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var current = 1
    private var rest = false
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    fun update(round: Int, resting: Boolean) {
        if (round != current || rest != resting) { current = round; rest = resting; invalidate() }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gap = LyraUi.dp(context, 5).toFloat()
        val step = width.toFloat() / count
        for (index in 0 until count) {
            rect.set(index * step + gap / 2, height * .3f, (index + 1) * step - gap / 2, height * .7f)
            val done = index + 1 < current
            val active = index + 1 == current
            paint.color = when { done -> LyraUi.moss; active && rest -> LyraUi.mossInk; active -> LyraUi.actionOrange; else -> LyraUi.line }
            canvas.drawRoundRect(rect, height / 2f, height / 2f, paint)
            if (active) {
                paint.color = if (rest) LyraUi.mossInk else LyraUi.actionOrange
                canvas.drawCircle(rect.centerX(), height * .94f, LyraUi.dp(context, 2).toFloat(), paint)
            }
        }
    }
}
