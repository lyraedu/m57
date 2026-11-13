// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout

enum class FocusMode(val key: String, val title: String, val detail: String, val icon: M57Icon) {
    TIMER("timer", "Timer", "Countdown", M57Icon.CLOCK),
    POMODORO("pomodoro", "Focus + rest", "Pomodoro", M57Icon.PLAN),
    STOPWATCH("stopwatch", "Count up", "Stopwatch", M57Icon.FOCUS)
}

class FocusModePicker(context: Context, initial: FocusMode, private val choose: (FocusMode) -> Unit) : LinearLayout(context) {
    private var selected = initial
    init { orientation = HORIZONTAL; render() }
    private fun render() {
        removeAllViews()
        FocusMode.values().forEach { mode ->
            val active = selected == mode
            val green = mode == FocusMode.POMODORO
            val ink = if (green) LyraUi.mossInk else LyraUi.actionOrange
            addView(LinearLayout(context).apply {
                orientation = VERTICAL; gravity = Gravity.CENTER; minimumHeight = LyraUi.dp(context, 112)
                setPadding(LyraUi.dp(context, 5), LyraUi.dp(context, 14), LyraUi.dp(context, 5), LyraUi.dp(context, 12))
                background = RippleDrawable(ColorStateList.valueOf(0x20D56A3A), LyraUi.shape(
                    if (active) if (green) LyraUi.mossSoft else LyraUi.apricotSoft else Color.WHITE,
                    LyraUi.dp(context, 18).toFloat(), if (active) ink else LyraUi.line), null)
                addView(M57IconView(context, mode.icon, if (active) ink else LyraUi.muted), LayoutParams(LyraUi.dp(context, 26), LyraUi.dp(context, 26)))
                addView(LyraUi.text(context, mode.title, 13f, if (active) ink else LyraUi.ink, true).apply {
                    gravity = Gravity.CENTER; layoutParams = LayoutParams(-1, -2).apply { topMargin = LyraUi.dp(context, 10); bottomMargin = LyraUi.dp(context, 5) }
                })
                addView(LyraUi.text(context, mode.detail, 12f, LyraUi.muted).apply { gravity = Gravity.CENTER; layoutParams = LayoutParams(-1, -2) })
                isClickable = true; isFocusable = true; isSelected = active; contentDescription = "${mode.title}. ${mode.detail}"
                descendantFocusability = FOCUS_BLOCK_DESCENDANTS
                accessibilityDelegate = object : View.AccessibilityDelegate() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                        super.onInitializeAccessibilityNodeInfo(host, info); info.className = "android.widget.RadioButton"; info.isCheckable = true; info.isChecked = active
                    }
                }
                Motion.press(this)
                setOnClickListener {
                    if (selected != mode) { selected = mode; render(); choose(mode) }
                }
            }, LayoutParams(0, -2, 1f).apply { marginStart = LyraUi.dp(context, 3); marginEnd = LyraUi.dp(context, 3) })
        }
    }
}
