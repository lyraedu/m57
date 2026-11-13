// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar

/** Inline operation state: visible while work runs, persistent when an action fails. */
class ActionFeedback(context: Context) : LinearLayout(context) {
    private val progress = ProgressBar(context).apply {
        indeterminateTintList = ColorStateList.valueOf(LyraUi.actionOrange)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val label = LyraUi.text(context, "", 14f).apply {
        layoutParams = LayoutParams(0, -2, 1f)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(LyraUi.dp(context, 16), LyraUi.dp(context, 14), LyraUi.dp(context, 16), LyraUi.dp(context, 14))
        addView(progress, LayoutParams(LyraUi.dp(context, 22), LyraUi.dp(context, 22)).apply { marginEnd = LyraUi.dp(context, 12) })
        addView(label)
        layoutParams = LayoutParams(-1, -2).apply { topMargin = LyraUi.dp(context, 8); bottomMargin = LyraUi.dp(context, 8) }
        visibility = GONE
    }
    fun show(message: String, busy: Boolean = false, error: Boolean = false) {
        val entering = visibility != VISIBLE
        label.text = message
        label.setTextColor(if (error || busy) LyraUi.actionOrange else LyraUi.mossInk)
        progress.visibility = if (busy) VISIBLE else GONE
        background = LyraUi.shape(if (error || busy) LyraUi.apricotSoft else LyraUi.mossSoft, LyraUi.dp(context, 16).toFloat())
        visibility = VISIBLE
        if (entering) Motion.enter(this)
    }
}
