// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.LinearLayout
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// The live ring, shifting sky and countdown draw on Zenith's BedtimeStatusProgress design.
class BedtimeCard(context: Context, private val schedule: BedtimeSchedule, private val protectionReady: Boolean) : LinearLayout(context) {
    companion object {
        fun time(context: Context, minutes: Int): String {
            val local = LocalTime.of(minutes / 60, minutes % 60)
            val locale = context.resources.configuration.locales[0]
            val skeleton = if (android.text.format.DateFormat.is24HourFormat(context)) "Hm" else "hm"
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            return local.format(DateTimeFormatter.ofPattern(pattern, locale))
        }

        fun summary(context: Context, schedule: BedtimeSchedule): String =
            if (schedule.enabled) "${time(context, schedule.start)} – ${time(context, schedule.end)} · ${schedule.apps.size} apps"
            else "Off · choose when to wind down"
    }

    private val sky = BedtimeSky(context)
    private val headline = LyraUi.text(context, "", 30f, bold = true)
    private val detail = LyraUi.text(context, "", 14f, LyraUi.muted)
    private val refresh = object : Runnable {
        override fun run() {
            val now = ZonedDateTime.now()
            val window = schedule.window(now)
            val active = window?.active(now) == true
            headline.text = when {
                window == null -> "Bedtime is off"
                active -> "Settling in for the night"
                else -> "Evening ahead"
            }
            detail.text = if (window == null) "Choose your schedule and which apps can wait." else {
                val edge = if (active) window.end() else window.start()
                val minutes = ((Duration.between(now, edge).seconds + 59) / 60).coerceAtLeast(1)
                val remaining = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
                val day = edge.format(DateTimeFormatter.ofPattern("EEE"))
                "${if (active) "Ends" else "Starts"} in $remaining · $day ${time(context, edge.hour * 60 + edge.minute)}"
            }
            val length = if (window != null) Duration.between(window.start(), window.end()).seconds.coerceAtLeast(1) else 1L
            val progress = if (active && window != null) Duration.between(window.start(), now).seconds.toFloat() / length else 0f
            sky.scene(active, progress)
            if (window != null) postDelayed(this, 60_050L - now.second * 1000L - now.nano / 1_000_000L)
        }
    }

    init {
        orientation = VERTICAL
        val padding = LyraUi.dp(context, 18)
        setPadding(padding, padding, padding, padding)
        background = LyraUi.shape(0xFFEAE9F5.toInt(), LyraUi.dp(context, 30).toFloat())
        addView(sky, LayoutParams(-1, LyraUi.dp(context, 190)))
        addView(headline)
        addView(detail)
        if (schedule.enabled && schedule.apps.isEmpty()) addView(LyraUi.text(context, "Schedule only. Add apps to hold during bedtime.", 14f, LyraUi.muted))
        if (schedule.enabled && schedule.apps.isNotEmpty() && !protectionReady)
            addView(LyraUi.text(context, "App limits for bedtime need active protection and a connected accessibility service.", 14f, LyraUi.muted))
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        removeCallbacks(refresh)
        if (hasWindowFocus) refresh.run()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(refresh)
        refresh.run()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(refresh)
        super.onDetachedFromWindow()
    }
}

class BedtimeSky(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val track = Path()
    private val arc = Path()
    private var phase = 0f
    private var nightBlend = 0f
    private var progress = 0f
    private var transition: ValueAnimator? = null
    private val colors = ArgbEvaluator()
    private val motion = AmbientArtwork(this, onFrame = {
        phase = it
        invalidate()
    })

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun scene(active: Boolean, fraction: Float) {
        val targetNight = if (active) 1f else 0f
        val targetProgress = fraction.coerceIn(0f, 1f)
        transition?.cancel()
        if (!isAttachedToWindow || !hasWindowFocus() || !Motion.enabled(context)) {
            nightBlend = targetNight
            progress = targetProgress
            invalidate()
            return
        }
        if (nightBlend == targetNight && progress == targetProgress) return
        val previousNight = nightBlend
        val previousProgress = progress
        transition = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 540
            interpolator = android.view.animation.PathInterpolator(.4f, 0f, .2f, 1f)
            addUpdateListener {
                val step = it.animatedValue as Float
                nightBlend = previousNight + (targetNight - previousNight) * step
                progress = previousProgress + (targetProgress - previousProgress) * step
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        transition?.cancel()
        transition = null
        super.onDetachedFromWindow()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        motion.refresh()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) * .72f
        val t = phase * 2 * PI
        val backdrop = blend(0xFFFFE2CA.toInt(), 0xFF27254B.toInt())
        val glow = blend(0xFF9D482C.toInt(), 0xFFDFCCFF.toInt())
        val star = blend(0xFFA05838.toInt(), 0xFFF8E4BA.toInt())

        paint.style = Paint.Style.FILL
        paint.color = backdrop
        val corner = LyraUi.dp(context, 26).toFloat()
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), corner, corner, paint)
        for (i in 0..11) {
            val x = width * (.07f + (i * 17 % 83) / 100f)
            val y = height * (.12f + (i * 29 % 72) / 100f)
            if (kotlin.math.hypot(x - cx, y - cy) < radius * 1.2f) continue
            paint.color = star
            paint.alpha = (150 + 80 * sin(t + i)).toInt()
            val size = LyraUi.dp(context, 2) * (1f + (i % 3) * .28f)
            canvas.drawCircle(x, y, size, paint)
        }

        track.rewind()
        arc.rewind()
        for (step in 0..120) {
            val angle = step * 2 * PI / 120 - PI / 2
            val r = radius * (1 + .034 * sin(angle * 10 + t))
            val x = cx + (cos(angle) * r).toFloat()
            val y = cy + (sin(angle) * r).toFloat()
            if (step == 0) { track.moveTo(x, y); arc.moveTo(x, y) }
            else {
                track.lineTo(x, y)
                if (step <= progress * 120) arc.lineTo(x, y)
            }
        }
        track.close()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = LyraUi.dp(context, 4).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = glow
        paint.alpha = 90
        canvas.drawPath(track, paint)
        paint.alpha = 255
        canvas.drawPath(arc, paint)

        paint.style = Paint.Style.FILL
        paint.color = blend(0xFFBA542F.toInt(), 0xFFF8E4BA.toInt())
        val body = radius * (.43f + .03f * sin(t).toFloat())
        canvas.drawCircle(cx, cy, body, paint)
        if (nightBlend > 0f) {
            paint.color = backdrop
            canvas.drawCircle(cx + body * (1.9f - 1.38f * nightBlend), cy - body * .31f, body * .9f, paint)
        }
    }

    private fun blend(day: Int, night: Int) = if (nightBlend == 0f) day else if (nightBlend == 1f) night
        else colors.evaluate(nightBlend, day, night) as Int
}
