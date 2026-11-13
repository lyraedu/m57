// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent

/** Personal focus never writes administrator pause state or redirects arbitrary apps to Lyra. */
class FocusBlockedOverlay(private val service: AccessibilityService) {
    private val surface = OverlaySurface(service)
    private var identity: String? = null
    fun show(pkg: String, reason: String) {
        val key = "$pkg:$reason"
        if (surface.visible && key == identity) return
        hide()
        val card = LyraUi.column(service, 26)
        card.addView(LyraUi.brand(service))
        card.addView(LyraUi.badge(service, "A BOUNDARY YOU CHOSE"))
        card.addView(LyraUi.text(service, "Keep this moment\nfor yourself.", 34f, bold = true))
        card.addView(FocusArt(service, FocusArt.Scene.FOCUS), android.widget.LinearLayout.LayoutParams(-1, LyraUi.dp(service, 210)))
        card.addView(LyraUi.text(service, reason, 20f, bold = true))
        card.addView(LyraUi.text(service, "This app or website is taking a break. Calls, Android navigation and your other apps remain available.", 16f, LyraUi.muted))
        card.addView(LyraUi.button(service, "Back to my day") {
            if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)) hide()
        })
        card.addView(LyraUi.button(service, "View my focus plan", false) {
            runCatching { service.startActivity(Intent(service, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)) }.onSuccess { hide() }
        })
        if (surface.show(card, "M57 focus boundary. $reason")) identity = key
    }
    fun hide() { surface.hide(); identity = null }
}
