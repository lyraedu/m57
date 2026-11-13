// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar

/** Live entry guidance, not an entropy estimate. No password text is retained by this view. */
class PassphraseChecklist(context: Context) : LinearLayout(context) {
    private val length = LyraUi.text(context, "", 14f, LyraUi.mossInk, true)
    private val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 12; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val checks = LyraUi.text(context, "", 13f, LyraUi.muted)
    private val confirmation = LyraUi.text(context, "", 14f, LyraUi.muted, true)
    private var matched = false
    init {
        orientation = VERTICAL
        setPadding(LyraUi.dp(context, 16), LyraUi.dp(context, 16), LyraUi.dp(context, 16), LyraUi.dp(context, 8))
        background = LyraUi.shape(LyraUi.mossSoft, LyraUi.dp(context, 18).toFloat())
        addView(length); addView(bar, LayoutParams(-1, LyraUi.dp(context, 10)).apply { bottomMargin = LyraUi.dp(context, 14) })
        addView(checks); addView(confirmation)
        layoutParams = LayoutParams(-1, -2).apply { topMargin = LyraUi.dp(context, 6); bottomMargin = LyraUi.dp(context, 12) }
    }
    fun update(phrase: String, repeated: String) {
        val count = phrase.codePointCount(0, phrase.length)
        val problem = PasswordCredential.problem(phrase)
        val ready = problem == null
        val match = ready && repeated == phrase
        length.text = when {
            count > 128 -> context.getString(R.string.password_too_long, count)
            count >= 12 -> context.getString(R.string.password_length_ready, count)
            else -> context.getString(R.string.password_length_progress, count)
        }
        length.setTextColor(if (count in 12..128) LyraUi.mossInk else LyraUi.actionOrange)
        bar.progressTintList = ColorStateList.valueOf(if (ready) LyraUi.moss else LyraUi.orange)
        bar.setProgress(count.coerceAtMost(12), Motion.enabled(context))
        checks.text = if (phrase.isEmpty()) context.getString(R.string.password_phrase_tip)
            else problem ?: context.getString(R.string.password_basic_checks_ready)
        confirmation.text = when {
            repeated.isEmpty() -> context.getString(R.string.password_confirmation_prompt)
            match -> context.getString(R.string.password_confirmation_ready)
            phrase.startsWith(repeated) -> context.getString(R.string.password_confirmation_typing)
            else -> context.getString(R.string.password_confirmation_not_yet)
        }
        confirmation.setTextColor(if (match) LyraUi.mossInk else if (repeated.isNotEmpty() && !phrase.startsWith(repeated)) LyraUi.actionOrange else LyraUi.muted)
        if (match && !matched) Motion.enter(confirmation)
        matched = match
    }
}
