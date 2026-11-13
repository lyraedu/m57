// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout

data class DockDestination(val id: String, val title: String, val icon: M57Icon)

/** Shared labelled navigation for personal and parent spaces. */
class NavigationDock @JvmOverloads constructor(context: Context, destinations: List<DockDestination> = emptyList(),
    selected: String = "", choose: (String) -> Unit = {}) : LinearLayout(context) {
    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(LyraUi.dp(context, 7), LyraUi.dp(context, 7), LyraUi.dp(context, 7), LyraUi.dp(context, 7))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.WHITE, LyraUi.background, LyraUi.sand)).apply {
            cornerRadius = LyraUi.dp(context, 28).toFloat(); setStroke(LyraUi.dp(context, 1), LyraUi.line)
        }
        elevation = LyraUi.dp(context, 5).toFloat()
        destinations.forEach { destination ->
            val active = selected == destination.id
            val green = destination.icon == M57Icon.INSIGHTS || destination.icon == M57Icon.PLAN
            val tone = if (green) LyraUi.mossInk else LyraUi.actionOrange
            addView(LinearLayout(context).apply {
                orientation = VERTICAL; gravity = Gravity.CENTER; minimumHeight = LyraUi.dp(context, 64)
                setPadding(LyraUi.dp(context, 3), LyraUi.dp(context, 8), LyraUi.dp(context, 3), LyraUi.dp(context, 6))
                background = RippleDrawable(ColorStateList.valueOf(0x22D56A3A),
                    LyraUi.shape(if (active) if (green) LyraUi.mossSoft else LyraUi.apricotSoft else Color.TRANSPARENT, LyraUi.dp(context, 20).toFloat()), null)
                addView(M57IconView(context, destination.icon, if (active) tone else LyraUi.muted), LayoutParams(LyraUi.dp(context, 24), LyraUi.dp(context, 24)))
                addView(LyraUi.text(context, destination.title, 11f, if (active) tone else LyraUi.muted, active).apply {
                    gravity = Gravity.CENTER; layoutParams = LayoutParams(-1, -2).apply { topMargin = LyraUi.dp(context, 5) }
                })
                isSelected = active; isClickable = true; isFocusable = true
                descendantFocusability = FOCUS_BLOCK_DESCENDANTS; contentDescription = destination.title
                accessibilityDelegate = object : View.AccessibilityDelegate() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                        super.onInitializeAccessibilityNodeInfo(host, info); info.className = "android.widget.Button"; info.isSelected = active
                    }
                }
                Motion.press(this)
                setOnClickListener { choose(destination.id) }
            }, LayoutParams(0, -2, 1f).apply { marginStart = LyraUi.dp(context, 2); marginEnd = LyraUi.dp(context, 2) })
        }
    }
}
