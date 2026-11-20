// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.sin

/** A small artwork breath, only while visible. Native text and controls never drift. */
class AmbientArtwork(
    private val view: View,
    private val onFrame: ((Float) -> Unit)? = null
) : View.OnAttachStateChangeListener {
    private var attached = false
    private var animator: ValueAnimator? = null
    private val visible = Rect()
    private val context get() = view.context
    private val accessibility get() = context.getSystemService(AccessibilityManager::class.java)
    private val changed = ViewTreeObserver.OnScrollChangedListener { refresh() }
    private val layout = ViewTreeObserver.OnGlobalLayoutListener { refresh() }
    private val touch = AccessibilityManager.TouchExplorationStateChangeListener { refresh() }
    private val power = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refresh() }
    }
    private val scale = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { refresh() }
    }
    init { view.addOnAttachStateChangeListener(this) }
    override fun onViewAttachedToWindow(v: View) {
        if (attached) return
        attached = true
        context.registerReceiver(power, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, scale)
        accessibility.addTouchExplorationStateChangeListener(touch)
        view.viewTreeObserver.addOnScrollChangedListener(changed)
        view.viewTreeObserver.addOnGlobalLayoutListener(layout)
        refresh()
    }
    override fun onViewDetachedFromWindow(v: View) {
        stop()
        if (!attached) return
        attached = false
        context.unregisterReceiver(power)
        context.contentResolver.unregisterContentObserver(scale)
        accessibility.removeTouchExplorationStateChangeListener(touch)
        if (view.viewTreeObserver.isAlive) {
            view.viewTreeObserver.removeOnScrollChangedListener(changed)
            view.viewTreeObserver.removeOnGlobalLayoutListener(layout)
        }
    }
    fun refresh() {
        val active = attached && (view !is ImageView || view.drawable != null) && view.isShown && view.windowVisibility == View.VISIBLE &&
            view.hasWindowFocus() && view.getGlobalVisibleRect(visible) && visible.height() >= LyraUi.dp(context, 24) && Motion.enabled(context)
        if (!active) { stop(); return }
        if (animator != null) return
        val distance = LyraUi.dp(context, 3).toFloat()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 9_000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener {
                val phase = it.animatedValue as Float
                if (onFrame != null) {
                    onFrame.invoke(phase)
                    return@addUpdateListener
                }
                val next = sin(phase * Math.PI * 2).toFloat() * distance
                // Subpixel changes need no new display-list update on every frame.
                if (abs(next - view.translationY) >= context.resources.displayMetrics.density * .12f) view.translationY = next
            }
            start()
        }
    }
    private fun stop() {
        if (animator == null) return
        animator?.cancel()
        animator = null
        if (onFrame == null) view.translationY = 0f else onFrame.invoke(0f)
    }
}
