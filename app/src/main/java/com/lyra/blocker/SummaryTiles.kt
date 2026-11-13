// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.widget.GridLayout
import android.widget.LinearLayout

data class SummaryMetric(val title: String, val value: String, val detail: String, val icon: M57Icon, val green: Boolean = false)

/** Compact, readable local summaries. Large type and narrow windows get one column. */
class SummaryTiles(context: Context, private val metrics: List<SummaryMetric>) : GridLayout(context) {
    private var columns = 0
    init { orientation = HORIZONTAL }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val next = if (w < LyraUi.dp(context, 290) || resources.configuration.fontScale >= 1.35f) 1 else 2
        if (columns != next) populate(next)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
    private fun populate(next: Int) {
        removeAllViews(); columns = next; columnCount = next
        metrics.forEachIndexed { index, item ->
            val tone = if (item.green) LyraUi.mossInk else LyraUi.actionOrange
            val card = LyraUi.column(context, 18).apply {
                background = LyraUi.shape(if (item.green) LyraUi.mossSoft else LyraUi.apricotSoft, LyraUi.dp(context, 22).toFloat())
                addView(M57IconView(context, item.icon, tone), LinearLayout.LayoutParams(LyraUi.dp(context, 24), LyraUi.dp(context, 24)).apply { bottomMargin = LyraUi.dp(context, 14) })
                addView(LyraUi.text(context, item.title, 13f, tone, true))
                addView(LyraUi.text(context, item.value, 28f, LyraUi.ink, true))
                addView(LyraUi.text(context, item.detail, 12f, LyraUi.muted).apply { layoutParams = LinearLayout.LayoutParams(-1, -2) })
                isScreenReaderFocusable = true
            }
            addView(card, LayoutParams(spec(index / next), spec(index % next, 1f)).apply {
                width = 0; height = LayoutParams.WRAP_CONTENT
                setMargins(LyraUi.dp(context, 3), LyraUi.dp(context, 4), LyraUi.dp(context, 3), LyraUi.dp(context, 4))
                setGravity(android.view.Gravity.FILL_HORIZONTAL)
            })
        }
    }
}
