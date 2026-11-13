// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.abs

/** Insets, keyboard avoidance and scroll-aware navigation shared by the native pages. */
class PageShell @JvmOverloads constructor(activity: Context, tab: AdminTab? = null, onTab: (AdminTab) -> Unit = {}, customDock: View? = null) : FrameLayout(activity) {
    val content = LyraUi.column(activity)
    private val scroll = ScrollView(activity)
    private val dock = customDock ?: tab?.let { AdminDock(activity, it, onTab) }
    private var footer: View? = null
    private var keyboard = false
    private var shown = true
    private var direction = 0
    private var travel = 0
    private val density = resources.displayMetrics.density

    init {
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(LyraUi.background, Color.rgb(255, 247, 240), Color.rgb(247, 242, 235)))
        content.addView(LyraUi.brand(activity))
        scroll.isVerticalScrollBarEnabled = false
        scroll.isFillViewport = true
        scroll.clipToPadding = false
        val stage = LinearLayout(activity).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; addView(content, LinearLayout.LayoutParams(-1, -2)) }
        stage.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val desired = stage.width.coerceAtMost(LyraUi.dp(activity, 640))
            if (content.layoutParams.width != desired) content.layoutParams = LinearLayout.LayoutParams(desired, -2)
        }
        scroll.addView(stage)
        addView(scroll, LayoutParams(-1, -1))
        dock?.let { navigation ->
            addView(navigation, LayoutParams(-1, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                leftMargin = LyraUi.dp(activity, 20); rightMargin = LyraUi.dp(activity, 20)
                bottomMargin = LyraUi.dp(activity, 12)
            })
            navigation.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateContentPadding() }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val params = navigation.layoutParams as LayoutParams
                val desired = (width - paddingLeft - paddingRight - LyraUi.dp(activity, 40))
                    .coerceAtMost(LyraUi.dp(activity, 480)).coerceAtLeast(0)
                if (params.width != desired) { params.width = desired; navigation.layoutParams = params }
            }
            scroll.setOnScrollChangeListener { _: View, _: Int, y: Int, _: Int, oldY: Int ->
                if (keyboard) return@setOnScrollChangeListener
                val assisted = activity.getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled
                val remaining = (stage.height - scroll.height - y).coerceAtLeast(0)
                if (assisted || y <= 24 * density || remaining <= 72 * density) {
                    travel = 0; setDockVisible(true)
                } else {
                    val delta = y - oldY
                    val nextDirection = delta.compareTo(0)
                    if (nextDirection != 0) {
                        if (nextDirection != direction) travel = 0
                        direction = nextDirection
                        travel += abs(delta)
                        if (travel >= 24 * density) { setDockVisible(delta < 0); travel = 0 }
                    }
                }
            }
        }
        setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            val wasKeyboard = keyboard
            keyboard = insets.isVisible(WindowInsets.Type.ime())
            if (keyboard != wasKeyboard) {
                dock?.animate()?.cancel()
                dock?.visibility = if (keyboard) View.GONE else View.VISIBLE
                dock?.alpha = 1f; dock?.translationY = 0f
                shown = true
                dock?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            }
            updateContentPadding()
            insets
        }
        content.post { Motion.enter(content) }
    }

    private fun updateContentPadding() {
        val occupied = if (keyboard) footer?.height ?: 0 else maxOf(dock?.height ?: 0, footer?.height ?: 0)
        val bottom = occupied + LyraUi.dp(context, if (occupied > 0) 36 else 24)
        content.setPadding(LyraUi.dp(context, 24), LyraUi.dp(context, 24), LyraUi.dp(context, 24), bottom)
    }

    fun setFooter(view: View) {
        footer?.let(::removeView)
        footer = view
        view.setPadding(LyraUi.dp(context, 20), LyraUi.dp(context, 6), LyraUi.dp(context, 20), LyraUi.dp(context, 12))
        view.setBackgroundColor(LyraUi.background)
        addView(view, LayoutParams(-1, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateContentPadding() }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val desired = (width - paddingLeft - paddingRight).coerceAtMost(LyraUi.dp(context, 640))
            if (view.layoutParams.width != desired) view.layoutParams = LayoutParams(desired, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        }
    }

    private fun setDockVisible(visible: Boolean) {
        val navigation = dock ?: return
        if (shown == visible) return
        shown = visible
        navigation.animate().cancel()
        navigation.visibility = View.VISIBLE
        navigation.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        navigation.animate().alpha(if (visible) 1f else 0f)
            .translationY(if (visible) 0f else navigation.height + LyraUi.dp(context, 24).toFloat())
            .setDuration(if (Motion.enabled(context)) 180 else 0).withEndAction { if (!shown) navigation.visibility = View.INVISIBLE }.start()
    }

    override fun onDetachedFromWindow() {
        dock?.animate()?.cancel()
        content.animate().cancel()
        super.onDetachedFromWindow()
    }
}
