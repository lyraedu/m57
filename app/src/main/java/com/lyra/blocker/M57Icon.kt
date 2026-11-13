// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

enum class M57Icon { HOME, FOCUS, PLAN, INSIGHTS, SETTINGS, SHIELD, BROWSER, CLOCK, PAUSE, ARROW, INFO }

/** Original lightweight icons; no bitmap decode or path allocation during drawing. */
class M57IconView @JvmOverloads constructor(context: Context, private val icon: M57Icon = M57Icon.FOCUS,
    color: Int = LyraUi.actionOrange) : View(context) {
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val box = RectF(3f, 4f, 21f, 21f)
    private val home = Path().apply { moveTo(3f, 11f); lineTo(12f, 3f); lineTo(21f, 11f); moveTo(5f, 10f); lineTo(5f, 21f); lineTo(10f, 21f); lineTo(10f, 15f); lineTo(14f, 15f); lineTo(14f, 21f); lineTo(19f, 21f); lineTo(19f, 10f) }
    private val shield = Path().apply { moveTo(12f, 2.5f); lineTo(20f, 6f); lineTo(19f, 14f); quadTo(18f, 19f, 12f, 22f); quadTo(6f, 19f, 5f, 14f); lineTo(4f, 6f); close() }
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); canvas.save(); canvas.scale(width / 24f, height / 24f)
        when (icon) {
            M57Icon.HOME -> canvas.drawPath(home, pen)
            M57Icon.FOCUS -> { canvas.drawCircle(12f, 12f, 9f, pen); canvas.drawCircle(12f, 12f, 4f, pen); canvas.drawLine(15f, 9f, 21f, 3f, pen) }
            M57Icon.PLAN -> { canvas.drawRoundRect(box, 3f, 3f, pen); canvas.drawLine(3f, 9f, 21f, 9f, pen); canvas.drawLine(8f, 2f, 8f, 6f, pen); canvas.drawLine(16f, 2f, 16f, 6f, pen); canvas.drawLine(7f, 14f, 10f, 14f, pen); canvas.drawLine(14f, 14f, 17f, 14f, pen); canvas.drawLine(7f, 18f, 10f, 18f, pen) }
            M57Icon.INSIGHTS -> { canvas.drawLine(4f, 21f, 21f, 21f, pen); canvas.drawLine(6f, 17f, 6f, 12f, pen); canvas.drawLine(12f, 17f, 12f, 5f, pen); canvas.drawLine(18f, 17f, 18f, 9f, pen) }
            M57Icon.SETTINGS -> { for (i in 0..2) { val y = 6f + i * 6; val x = if (i == 1) 16f else 8f; canvas.drawLine(3f, y, x - 3f, y, pen); canvas.drawLine(x + 3f, y, 21f, y, pen); canvas.drawCircle(x, y, 2.5f, pen) } }
            M57Icon.SHIELD -> { canvas.drawPath(shield, pen); canvas.drawLine(8f, 12f, 11f, 15f, pen); canvas.drawLine(11f, 15f, 16f, 9f, pen) }
            M57Icon.BROWSER -> { canvas.drawRoundRect(box, 3f, 3f, pen); canvas.drawLine(3f, 9f, 21f, 9f, pen); canvas.drawCircle(7f, 6.5f, 0.6f, pen); canvas.drawLine(8f, 15f, 11f, 18f, pen); canvas.drawLine(11f, 18f, 17f, 12f, pen) }
            M57Icon.CLOCK -> { canvas.drawCircle(12f, 12f, 9f, pen); canvas.drawLine(12f, 6f, 12f, 12f, pen); canvas.drawLine(12f, 12f, 16f, 14f, pen) }
            M57Icon.PAUSE -> { canvas.drawRoundRect(5f, 4f, 9f, 20f, 1.5f, 1.5f, pen); canvas.drawRoundRect(15f, 4f, 19f, 20f, 1.5f, 1.5f, pen) }
            M57Icon.ARROW -> { canvas.drawLine(5f, 19f, 19f, 5f, pen); canvas.drawLine(9f, 5f, 19f, 5f, pen); canvas.drawLine(19f, 5f, 19f, 15f, pen) }
            M57Icon.INFO -> { canvas.drawCircle(12f, 12f, 9f, pen); canvas.drawLine(12f, 11f, 12f, 17f, pen); canvas.drawCircle(12f, 7f, 0.6f, pen) }
        }
        canvas.restore()
    }
}
