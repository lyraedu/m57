// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent

/** Personal focus never writes administrator pause state or redirects arbitrary apps to Lyra. */
class FocusBlockedOverlay(private val service: AccessibilityService) {
    private val surface = OverlaySurface(service)
    private var identity: String? = null
    fun show(pkg: String, reason: String, bedtime: Boolean = false) {
        val key = "$pkg:$reason:$bedtime"
        if (surface.visible && key == identity) return
        hide()
        val card = LyraUi.column(service, 26)
        card.addView(LyraUi.brand(service))
        card.addView(LyraUi.badge(service, if (bedtime) "BEDTIME" else "FOCUS"))
        card.addView(LyraUi.text(service, if (bedtime) "Time to wind down." else "Keep this moment\nfor yourself.", 34f, bold = true))
        card.addView(if (bedtime) BedtimeSky(service).apply { scene(true, 1f) } else FocusArt(service, FocusArt.Scene.FOCUS), android.widget.LinearLayout.LayoutParams(-1, LyraUi.dp(service, 180)))
        card.addView(LyraUi.text(service, reason, 20f, bold = true))
        card.addView(LyraUi.text(service, "This app or website is taking a break. Calls, Android navigation and your other apps remain available.", 16f, LyraUi.muted))
        card.addView(LyraUi.button(service, "Back to my day") {
            if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)) hide()
        })
        card.addView(LyraUi.button(service, if (bedtime) "Open bedtime" else "Open M57", false) {
            runCatching { service.startActivity(Intent(service, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(MainActivity.BEDTIME_EXTRA, bedtime)) }.onSuccess { hide() }
        })
        if (surface.show(card, "M57. $reason")) identity = key
    }
    fun hide() { surface.hide(); identity = null }
}
