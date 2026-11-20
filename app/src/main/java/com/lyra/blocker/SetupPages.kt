// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch

/** Parent-authorised permission guidance. Permission return never restores administrator authority. */
class SetupPages(private val activity: Activity, private val authorised: () -> Boolean,
    private val navigate: (AdminPage) -> Unit, private val footer: (LinearLayout) -> Unit) {
    private val store = ProtectionStore(activity)
    private val stories = PermissionStories(activity, authorised) { navigate(AdminPage.SETUP) }
    private fun advance() { if (authorised()) { store.setupStep = (store.setupStep + 1).coerceAtMost(6); navigate(AdminPage.SETUP) } }
    fun show(page: LinearLayout) {
        val step = store.setupStep
        page.addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 7; setProgress(step + 1, Motion.enabled(activity)); progressTintList = android.content.res.ColorStateList.valueOf(LyraUi.moss)
            contentDescription = "Android access step ${step + 1} of 7"
        })
        page.addView(LyraUi.badge(activity, "ANDROID ACCESS & YOUR CHOICES · ${step + 1}/7", LyraUi.mossInk, LyraUi.mossSoft))
        when (step) {
            0 -> stories.accessibility(page)
            1 -> stories.usage(page)
            2 -> stories.background(page)
            3 -> stories.notifications(page)
            4 -> stories.openSource(page)
            5 -> strict(page)
            else -> {
                Illustrations.addTo(page, "ready-for-your-day", 180)
                page.addView(LyraUi.text(activity, "A little space.\nReady when you are.", 30f, bold = true).apply { isAccessibilityHeading = true })
                page.addView(LyraUi.text(activity, "Your timer is ready. Other tools follow the access and boundaries you choose; Focus shows what is actually connected.", 16f, LyraUi.muted))
                page.addView(SummaryTiles(activity, listOf(
                    SummaryMetric("Screen controls", if (stories.accessible() && LyraAccessibilityService.connected) "Connected" else "Needs access", "Apps, videos and supported addresses", M57Icon.SHIELD, true),
                    SummaryMetric("App-time insights", if (stories.usageReady()) "Ready" else "Optional", "Local estimates and daily limits", M57Icon.INSIGHTS)
                )))
                page.addView(LyraUi.card(activity).apply {
                    addView(LyraUi.feature(activity, "◇", "Website filtering has its own choice", "Choose categories or a DNS provider in browsing controls. Android asks for VPN approval there, only if you use the local DNS filter."))
                })
                page.addView(LyraUi.text(activity, "Review any of these pages later from Settings. Optional access can be declined without losing the timer or the rest of your choices.", 14f, LyraUi.muted))
            }
        }
        val waiting = when (step) {
            0 -> !stories.accessible()
            1 -> !stories.usageReady()
            2 -> !BackgroundAccess.isExempt(activity)
            3 -> !stories.notificationsReady()
            else -> false
        }
        val actions = LyraUi.column(activity, 0)
        actions.addView(LyraUi.button(activity, if (step == 6) if (stories.accessible()) "Finish setup" else "Continue with the timer for now" else if (waiting) "Not now · continue" else "Continue") {
            if (!authorised()) return@button
            if (step == 6) { store.setupComplete = true; navigate(AdminPage.CONTROLS) } else advance()
        })
        if (step > 0) actions.addView(LyraUi.link(activity, "Previous step") { if (authorised()) { store.setupStep = step - 1; navigate(AdminPage.SETUP) } })
        footer(actions)
    }
    private fun strict(page: LinearLayout) {
        Illustrations.addTo(page, "commitment-ribbon", 180)
        page.addView(LyraUi.text(activity, "Make changes\nintentional.", 30f, bold = true).apply { isAccessibilityHeading = true })
        if (!store.familyManaged) {
            page.addView(LyraUi.text(activity, "Personal mode keeps Android’s controls available. You can choose a commitment timer when you want to finish a session before changing your personal focus boundaries.", 16f, LyraUi.muted))
            return
        }
        page.addView(LyraUi.badge(activity, "STRICT MODE · SETTINGS GUARD", LyraUi.mossInk, LyraUi.mossSoft))
        page.addView(LyraUi.text(activity, "An extra parent boundary can return recognised M57 App info and accessibility pages to the password screen. You can authorise 60 seconds of maintenance from Device access.", 16f, LyraUi.muted))
        page.addView(Switch(activity).apply {
            text = "Guard M57’s own settings pages"; minHeight = LyraUi.dp(activity, 64); setTextColor(LyraUi.ink); isChecked = store.settingsGuard
            setOnCheckedChangeListener { _, enabled -> if (authorised()) store.settingsGuard = enabled else isChecked = store.settingsGuard }
        })
        page.addView(LyraUi.card(activity).apply {
            addView(LyraUi.feature(activity, "♡", "What this preview covers", "Recognised M57 App info and accessibility detail pages. Other permission pages, VPN and installation guards are not yet complete in this preview."))
        })
        page.addView(LyraUi.text(activity, "This needs connected accessibility and depends on Android’s visible Settings layout. It is best-effort friction on a normal phone, not guaranteed uninstall prevention.", 14f, LyraUi.muted))
        val manager = activity.getSystemService(DevicePolicyManager::class.java)
        val active = manager.isAdminActive(ManagedProtection.component(activity))
        page.addView(LyraUi.section(activity, "An extra removal step", "Optional Android administrator access adds a deactivation confirmation before uninstalling.", green = true))
        page.addView(LyraUi.badge(activity, if (active) "ANDROID ADMINISTRATOR ENABLED" else "ANDROID ADMINISTRATOR OPTIONAL"))
        page.addView(LyraUi.button(activity, if (active) "Review device access" else "Add the removal confirmation", false) {
            if (!authorised()) return@button
            if (active) { navigate(AdminPage.DEVICE_ACCESS); return@button }
            store.allowAppInfoAccess(); activity.intent.putExtra(PermissionStories.RETURN_KEY, true)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ManagedProtection.component(activity))
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Add a confirmation step before removing M57. No wipe, lock or password policy is requested.")
            }
            runCatching { activity.startActivity(intent) }.onFailure {
                activity.intent.removeExtra(PermissionStories.RETURN_KEY)
                android.widget.Toast.makeText(activity, "This phone could not open administrator settings. You can continue without this optional step.", android.widget.Toast.LENGTH_LONG).show()
            }
        })
        page.addView(LyraUi.text(activity, "Keep your parent password somewhere safe. Assisted recovery and expanded Strict guards still need completion and testing. This guide does not provision device ownership or lock the phone into managed mode.", 14f, LyraUi.muted))
    }
}
