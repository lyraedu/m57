// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast

/** Shared permission stories. An Android handoff never counts as a permission grant. */
class PermissionStories(private val activity: Activity, private val authorised: () -> Boolean, private val refresh: () -> Unit) {
    companion object {
        const val RETURN_KEY = "m57.return_to_setup"
        const val NOTIFICATION_REQUEST = 5711
    }
    private val protection = ProtectionStore(activity)
    private val focus = FocusStore(activity)
    fun accessible() = activity.getSystemService(AccessibilityManager::class.java)
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.packageName == activity.packageName }
    fun usageReady() = focus.usageEnabled && UsageAccess.granted(activity)
    fun notificationsReady() = activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
        activity.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    private fun heading(page: LinearLayout, title: String, detail: String, art: String) {
        Illustrations.addTo(page, art, 170)
        page.addView(LyraUi.text(activity, title, 30f, bold = true).apply { isAccessibilityHeading = true })
        page.addView(LyraUi.text(activity, detail, 16f, LyraUi.muted))
    }
    private fun status(page: LinearLayout, ready: Boolean, yes: String, no: String) {
        page.addView(LyraUi.badge(activity, if (ready) yes else no,
            if (ready) LyraUi.mossInk else LyraUi.actionOrange, if (ready) LyraUi.mossSoft else LyraUi.apricotSoft))
    }
    private fun explain(page: LinearLayout, why: String, data: String, steps: List<String>) {
        page.addView(LyraUi.section(activity, "Why this helps", why, green = true))
        page.addView(LyraUi.card(activity).apply { addView(LyraUi.feature(activity, "♡", "Your access, explained", data)) })
        page.addView(LyraUi.section(activity, "In Android Settings", "Your place in this guide is saved."))
        steps.forEachIndexed { index, step -> page.addView(LyraUi.feature(activity, (index + 1).toString(), step, "")) }
    }
    private fun prepareReturn(): Boolean {
        if (!authorised()) return false
        if (protection.familyManaged) protection.allowAppInfoAccess()
        if (protection.onboarded) activity.intent.putExtra(RETURN_KEY, true)
        return true
    }
    private fun open(intent: Intent) {
        if (!prepareReturn()) return
        runCatching { activity.startActivity(intent) }.onFailure {
            activity.intent.removeExtra(RETURN_KEY)
            Toast.makeText(activity, "This Android page could not open. Follow the steps in Settings.", Toast.LENGTH_LONG).show()
        }
    }
    fun accessibility(page: LinearLayout) {
        val enabled = accessible()
        heading(page, "A hand with\nthe distractions.", "Let M57 recognise the app or supported screen you’re using, so your chosen boundary can appear at the right moment.", "permission-window")
        status(page, enabled, "ANDROID ACCESS ENABLED", "OPTIONAL · SCREEN CONTROLS")
        val agree = CheckBox(activity).apply {
            text = "I understand and agree to this accessibility use"; minHeight = LyraUi.dp(activity, 56); setTextColor(LyraUi.ink)
            isChecked = enabled && protection.consented
        }; page.addView(agree)
        page.addView(LyraUi.button(activity, if (enabled) "Review accessibility access" else "Open Android accessibility") {
            if (!agree.isChecked) { Toast.makeText(activity, "Read the explanation, then tick the agreement", Toast.LENGTH_LONG).show(); return@button }
            if (authorised()) { protection.consent(); open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        explain(page, "App boundaries, supported short-video screens, visible browser rules and routines need a connected service. The timer works without it.",
            "M57 checks app identities and supported titles, controls and address fields, then shows its boundary screen. It does not save message bodies or browsing history, or send these observations away.",
            listOf("Find M57 in Installed apps or Downloaded apps.", "Turn on M57’s accessibility service.", "Read Android’s confirmation, then return here."))
        page.addView(LyraUi.text(activity, "If Android says ‘Restricted settings’, open M57’s App info menu, choose Allow restricted settings, then return to Accessibility. This is Android’s protection for sideloaded apps.", 14f, LyraUi.muted))
        page.addView(LyraUi.button(activity, "Open M57 App info", false) { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}"))) })
    }
    fun usage(page: LinearLayout) {
        heading(page, "See where\nthe time goes.", "A little perspective can help you choose a daily allowance that feels right. App-time insights are your choice.", "daily-allowance")
        status(page, usageReady(), "LOCAL APP-TIME ACCESS READY", "OPTIONAL · INSIGHTS & DAILY LIMITS")
        val agree = CheckBox(activity).apply {
            text = "Use Android app-time estimates locally for insights and limits"; minHeight = LyraUi.dp(activity, 56); setTextColor(LyraUi.ink)
            isChecked = focus.usageEnabled
        }; page.addView(agree)
        page.addView(LyraUi.button(activity, if (usageReady()) "Review Android usage access" else "Enable local usage access") {
            if (!authorised()) return@button
            if (!agree.isChecked) { Toast.makeText(activity, "Choose local processing first, or continue without it", Toast.LENGTH_LONG).show(); return@button }
            val wasEnabled = focus.usageEnabled
            focus.usageEnabled = true
            if (UsageAccess.granted(activity) && !wasEnabled) refresh() else open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        explain(page, "Show estimated app time and apply the daily app limits you choose. Focus timers do not need usage access.",
            "Android supplies app identities and foreground-time events. M57 processes them on this phone without keeping raw event logs or uploading usage. It does not read messages. Estimates may differ from Android’s totals.",
            listOf("Find M57 under Usage access.", "Turn on Permit usage access.", "Return to M57 to check the ready status."))
        if (focus.usageEnabled) page.addView(LyraUi.link(activity, "Turn off local usage processing") { if (authorised()) { focus.usageEnabled = false; refresh() } })
    }
    fun background(page: LinearLayout) {
        val guide = BackgroundHelp.forDevice(Build.MANUFACTURER, Build.BRAND)
        val exempt = BackgroundAccess.isExempt(activity)
        heading(page, "Let M57\nstay ready.", "Some phones put apps to sleep to save power. A small Android setting can help your chosen controls stay available.", "routine-rhythm")
        status(page, exempt, "BATTERY EXEMPTION ALLOWED", "OPTIONAL · BACKGROUND RELIABILITY")
        page.addView(LyraUi.button(activity, if (exempt) "Review Android battery settings" else "Allow background running") {
            if (!prepareReturn()) return@button
            if (exempt) open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            else if (!BackgroundAccess.requestExemption(activity)) {
                activity.intent.removeExtra(RETURN_KEY)
                Toast.makeText(activity, "Open Settings → Apps → M57 → Battery", Toast.LENGTH_LONG).show()
            }
        })
        explain(page, "Reduce interruptions to app and routine checks when you leave M57. Your phone still decides when a service can run.",
            "This changes battery treatment for M57. It does not grant access to photos, the microphone, contacts or messages. You can keep Android’s standard limits and review this later.",
            listOf("Allow M57 to ignore battery optimisation if you choose.", "Check the manufacturer’s extra background controls below.", "Return here; we check Android’s actual exemption state."))
        if (guide != null) {
            page.addView(LyraUi.button(activity, "${guide.manufacturer} background settings", false) {
                if (prepareReturn() && !BackgroundAccess.openManufacturerSettings(activity, guide.vendor)) Toast.makeText(activity, "Use Android’s M57 battery settings instead", Toast.LENGTH_LONG).show()
            })
            page.addView(LyraUi.link(activity, "Extra help for ${guide.manufacturer}") { open(Intent(Intent.ACTION_VIEW, Uri.parse(guide.url))) })
        }
    }
    fun notifications(page: LinearLayout) {
        heading(page, "Know when\nM57 is there.", "Allow M57 to show its own protection-status messages, including when the optional local website filter is running.", "attention-garden")
        status(page, notificationsReady(), "M57 NOTIFICATIONS ALLOWED", "OPTIONAL · M57 STATUS MESSAGES")
        page.addView(LyraUi.button(activity, if (notificationsReady()) "Review notification settings" else "Allow M57 notifications") {
            if (!prepareReturn()) return@button
            if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
            else activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
        })
        explain(page, "Understand the state of M57’s background protection. You can choose the controls you need without allowing these messages.",
            "This permits M57 to send notifications. It does not let M57 read other apps’ notifications. Notification organising would be a separate optional tool with its own explanation and access.",
            listOf("Choose Allow on Android’s notification prompt.", "Review M57’s channels if you want finer control.", "Return here to check what Android allowed."))
    }
    fun openSource(page: LinearLayout) {
        heading(page, "Yours to use.\nOpen to improve.", "An app has a recipe: the code that makes it work. M57 shares that recipe, so people can check it and help make it better.", "privacy-drawer")
        page.addView(LyraUi.badge(activity, "FULLY FREE · OPEN SOURCE", LyraUi.mossInk, LyraUi.mossSoft))
        page.addView(SummaryTiles(activity, listOf(SummaryMetric("Every included feature", "Free", "No premium switches", M57Icon.FOCUS),
            SummaryMetric("Your attention", "No ads", "No ad-driven feed", M57Icon.SHIELD, true))))
        page.addView(LyraUi.section(activity, "You don’t need to be technical", "Use M57 like any other app. Others can inspect the code, suggest improvements and help fix problems.", green = true))
        page.addView(LyraUi.feature(activity, "◇", "No account to get started", "M57 works independently. Bringing Lyra along is optional."))
        page.addView(LyraUi.feature(activity, "♡", "A community can keep improving it", "Source and license notices explain how people can inspect, change and share the app."))
        page.addView(LyraUi.button(activity, "See M57’s source code", false) { open(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SOURCE_URL))) })
    }
}
