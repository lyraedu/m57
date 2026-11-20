// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import java.util.concurrent.Executor

/** Local app-time breakdown inspired by Zenith's period browsing, app rows and animated totals. */
class AppUsagePanel(
    private val activity: Activity,
    private val worker: Executor,
    private val accessSettings: () -> Unit,
    private val appDetail: (String, String) -> Unit
) : LinearLayout(activity) {
    private enum class Period(val label: String, val start: Long, val end: Long) {
        TODAY("Today", 0, 0), YESTERDAY("Yesterday", 1, 1), WEEK("7 days", 6, 0)
    }

    private data class AppTime(val name: String, val icon: Drawable?, val duration: Long)

    private val results = LyraUi.column(activity, 0)
    private var selection = Period.TODAY
    private var request = 0
    private var expanded = false
    private val periodButtons = mutableListOf<Button>()

    init {
        orientation = VERTICAL
        addView(LyraUi.text(activity, "App use", 24f, bold = true).apply { isAccessibilityHeading = true })
        addView(LyraUi.text(activity, "Keep usage access on and use M57 for at least 3 days to get a clearer picture. Android may provide only part of the earlier history.", 14f, LyraUi.muted))
        if (!FocusStore(activity).usageEnabled || !UsageAccess.granted(activity)) {
            addView(LyraUi.text(activity, "Enable Android usage access to see local foreground estimates.", 14f, LyraUi.muted))
            addView(LyraUi.button(activity, "Review usage access", false, accessSettings))
        } else {
            val periods = LinearLayout(activity).apply { orientation = HORIZONTAL; setBaselineAligned(false) }
            Period.entries.forEach { period ->
                val button = LyraUi.button(activity, period.label, false) {
                    if (selection != period) {
                        selection = period
                        expanded = false
                        load()
                    }
                }
                val inset = LyraUi.dp(activity, 4)
                button.setPaddingRelative(inset, LyraUi.dp(activity, 12), inset, LyraUi.dp(activity, 12))
                button.textSize = 13f
                periodButtons.add(button)
                periods.addView(button, LayoutParams(0, -2, 1f).apply {
                    marginEnd = LyraUi.dp(activity, if (period == Period.WEEK) 0 else 4)
                })
            }
            addView(periods)
            addView(results)
            load()
        }
    }

    private fun load() {
        val period = selection
        periodButtons.forEachIndexed { index, button ->
            val selected = index == period.ordinal
            button.isSelected = selected
            button.setTextColor(if (selected) LyraUi.mossInk else LyraUi.ink)
            button.background = android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x19000000),
                LyraUi.shape(if (selected) LyraUi.mossSoft else android.graphics.Color.WHITE, LyraUi.dp(activity, 18).toFloat(), LyraUi.line), null)
        }
        val ticket = ++request
        results.removeAllViews()
        results.addView(LyraUi.text(activity, "Reading Android usage…", 14f, LyraUi.muted).apply {
            accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
        })
        worker.execute {
            val snapshot = UsageAccess.period(activity, period.start, period.end)
            val totals = snapshot.totals.filter { (pkg, duration) -> pkg != activity.packageName && duration > 0 }
            val total = totals.values.sum()
            val manager = activity.packageManager
            val apps = totals.entries.sortedByDescending { it.value }.take(40).map { (pkg, duration) ->
                val info = runCatching { manager.getApplicationInfo(pkg, 0) }.getOrNull()
                AppTime(info?.let { runCatching { manager.getApplicationLabel(it).toString() }.getOrNull() } ?: pkg,
                    info?.let { runCatching { manager.getApplicationIcon(it) }.getOrNull() }, duration)
            }
            activity.runOnUiThread {
                if (!isAttachedToWindow || ticket != request || activity.isDestroyed) return@runOnUiThread
                if (snapshot.available && FocusStore(activity).usageEnabled && UsageAccess.granted(activity))
                    showApps(period, total, apps)
                else {
                    results.removeAllViews()
                    results.addView(LyraUi.text(activity, snapshot.problem ?: "Usage access is off", 14f, LyraUi.muted))
                    results.addView(LyraUi.button(activity, "Retry", false) { load() })
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        request++
        super.onDetachedFromWindow()
    }

    private fun showApps(period: Period, total: Long, apps: List<AppTime>) {
        results.removeAllViews()
        if (apps.isEmpty()) {
            results.addView(LyraUi.text(activity, "Android returned no foreground time for this period.", 14f, LyraUi.muted))
            return
        }
        results.addView(LyraUi.card(activity).apply {
            background = LyraUi.shape(LyraUi.mossSoft, LyraUi.dp(activity, 26).toFloat())
            addView(LyraUi.text(activity, duration(total), 36f, bold = true))
            addView(LyraUi.text(activity, "Estimated foreground time · ${period.label.lowercase()}", 14f, LyraUi.mossInk))
            Motion.enter(this)
        })
        apps.take(if (expanded) 40 else 6).forEach { app ->
            val share = (100.0 * app.duration / total).toInt()
            val row = LyraUi.card(activity)
            val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
            app.icon?.let { icon ->
                header.addView(ImageView(activity).apply {
                    setImageDrawable(icon)
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LayoutParams(LyraUi.dp(activity, 34), LyraUi.dp(activity, 34)).apply { marginEnd = LyraUi.dp(activity, 12) })
            }
            header.addView(LyraUi.text(activity, app.name, 17f, bold = true), LayoutParams(0, -2, 1f))
            header.addView(LyraUi.text(activity, duration(app.duration), 14f, LyraUi.muted))
            row.addView(header)
            row.addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progressTintList = ColorStateList.valueOf(LyraUi.mossInk)
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                post { if (isAttachedToWindow) setProgress(share.coerceAtLeast(1), Motion.enabled(activity)) }
            }, LayoutParams(-1, LyraUi.dp(activity, 18)))
            row.isFocusable = true
            header.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            row.contentDescription = "${app.name}, ${duration(app.duration)}, ${if (share == 0) "less than 1" else share} percent of recorded time"
            row.setOnClickListener { appDetail(app.name, "${duration(app.duration)} of estimated foreground time · ${period.label}. ${if (share == 0) "Less than 1%" else "$share%"} of the foreground time shown.") }
            Motion.press(row)
            results.addView(row)
        }
        if (apps.size > 6) results.addView(LyraUi.button(activity, if (expanded) "Show fewer apps" else "Show more apps", false) {
            expanded = !expanded
            showApps(period, total, apps)
        })
    }

    private fun duration(millis: Long): String {
        val minutes = millis / 60_000
        return when {
            minutes < 1 -> "<1 min"
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0L -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }
    }
}
