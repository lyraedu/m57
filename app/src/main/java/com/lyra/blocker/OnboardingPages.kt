// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.Toast

class OnboardingPages(private val activity: Activity, private val page: () -> LinearLayout,
    private val accessible: () -> Boolean, private val footer: (LinearLayout) -> Unit,
    private val back: (() -> Unit) -> Unit, private val complete: () -> Unit) {
    private val store = ProtectionStore(activity)
    fun show() {
        val root = page()
        val step = store.onboardingStep
        back { if (step > 0) { store.onboardingStep = step - 1; show() } else activity.finish() }
        root.addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 6; progress = step + 1; progressTintList = android.content.res.ColorStateList.valueOf(LyraUi.orange)
            contentDescription = "Setup step ${step + 1} of 6"
        })
        root.addView(LyraUi.badge(activity, "A LITTLE SPACE FOR YOU · ${step + 1}/6"))
        val scenes = listOf(FocusArt.Scene.FOCUS, FocusArt.Scene.INDEPENDENT, FocusArt.Scene.INDEPENDENT,
            FocusArt.Scene.SCROLL, FocusArt.Scene.BROWSE, FocusArt.Scene.FOCUS)
        val artwork = listOf("attention-garden", "personal-controls", "learning-detour", "short-video-choice", "permission-window", "ready-for-your-day")
        if (!Illustrations.addTo(root, artwork[step], 215)) root.addView(FocusArt(activity, scenes[step]), LinearLayout.LayoutParams(-1, LyraUi.dp(activity, 215)))
        fun heading(title: String, detail: String) {
            root.addView(LyraUi.text(activity, title, 31f, bold = true)); root.addView(LyraUi.text(activity, detail, 16f, LyraUi.muted))
        }
        when (step) {
            0 -> heading("Your phone.\nYour attention.", "Make room for the things you meant to do. Focus time, fewer endless videos, and browsing boundaries. Free, open source, and yours to choose.")
            1 -> {
                heading("Who is this phone for?", "A little help for your child, or a little more space for yourself. Choose the setup that fits.")
                val group = android.widget.RadioGroup(activity)
                val personal = android.widget.RadioButton(activity).apply { id = android.view.View.generateViewId(); text = "For me\nI choose and change my own boundaries"; minHeight = LyraUi.dp(activity, 80); setTextColor(LyraUi.ink) }
                val family = android.widget.RadioButton(activity).apply { id = android.view.View.generateViewId(); text = "For my child\nA parent password protects the settings"; minHeight = LyraUi.dp(activity, 80); setTextColor(LyraUi.ink) }
                group.addView(family); group.addView(personal)
                if (store.authorityChosen) group.check(if (store.draftFamily) family.id else personal.id)
                group.setOnCheckedChangeListener { _, id -> store.draftFamily = id == family.id }; root.addView(group)
                root.addView(LyraUi.text(activity, "Parents and caregivers can protect settings with a private password. Your child can still use the focus timer. Both setups work with or without Lyra, and neither needs an M57 account.", 14f, LyraUi.muted))
            }
            2 -> {
                heading("Use Lyra for AI learning?", "Optional: when a supported AI app or AI screen opens, M57 can guide it into Lyra's learning space. Leave this off for independent app, video and browsing controls.")
                root.addView(Switch(activity).apply {
                    text = "Include Lyra for AI learning"; textSize = 17f; minHeight = LyraUi.dp(activity, 60)
                    isChecked = store.draftLyra; setOnCheckedChangeListener { _, checked -> store.draftLyra = checked }
                })
                root.addView(LyraUi.text(activity, "No Lyra account is needed for M57. When redirection is on, the handoff sends only the AI source family and a request for a new chat. Using Lyra is a separate choice.", 14f, LyraUi.muted))
            }
            3 -> {
                heading("One more video?\nMake it a choice.", "Recognise supported full-screen short-video viewers while leaving ordinary videos and messages available. You can choose blocking or a gentle reminder later.")
                listOf("YouTube Shorts" to store.youtubeEnabled, "Instagram Reels" to store.instagramEnabled).forEach { (label, enabled) ->
                    root.addView(Switch(activity).apply {
                        text = label; textSize = 16f; minHeight = LyraUi.dp(activity, 56); isChecked = enabled
                        setOnCheckedChangeListener { _, value -> if (label == "YouTube Shorts") store.youtubeEnabled = value else store.instagramEnabled = value }
                    })
                }
                root.addView(LyraUi.text(activity, "App updates and translations can change detection. Check the controls you enable on this phone.", 13f, LyraUi.muted))
            }
            4 -> {
                if (store.draftFamily) {
                    heading("Keep the controls\nwith you.", "Next, choose a private parent password. Then we will guide you through the Android access needed for the controls you choose.")
                    root.addView(LyraUi.text(activity, "Use a long phrase you can remember, and keep a private copy somewhere safe. Your child should not know it. There is no password-reset service in this version.", 15f, LyraUi.muted))
                    root.addView(LyraUi.text(activity, "M57 explains each permission before opening Android Settings. Optional website filtering and app-time insights have their own access choices.", 14f, LyraUi.muted))
                } else {
                heading("A permission\nwith a purpose.", "Accessibility recognises app identity and supported screen controls, then displays M57's boundary screen. It also runs focus and routine checks while connected.")
                root.addView(LyraUi.badge(activity, if (accessible()) "ACCESSIBILITY ENABLED" else "ACCESSIBILITY NOT ENABLED"))
                root.addView(LyraUi.button(activity, "What can M57 see?", false) {
                    android.app.AlertDialog.Builder(activity).setTitle("Your access, explained").setMessage(activity.getString(R.string.accessibility_description))
                        .setPositiveButton("Understood", null).show().also(LyraUi::styleDialog)
                })
                val agree = CheckBox(activity).apply { text = "I understand and agree to this local accessibility use"; minHeight = LyraUi.dp(activity, 56) }; root.addView(agree)
                root.addView(LyraUi.button(activity, "Open Android accessibility settings") {
                    if (!agree.isChecked) { Toast.makeText(activity, "Read the explanation and tick the agreement first", Toast.LENGTH_LONG).show(); return@button }
                    // Authority is selected explicitly before enabling a service. No password settings are replaced.
                    if (!store.hasPassword) store.prefs.edit().putString("authority", if (store.draftFamily) "family" else "personal")
                        .putBoolean("ai_enabled", store.draftLyra).putBoolean("settings_guard", false).putBoolean("repo_guard", false).apply()
                    store.consent()
                    runCatching { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        .onFailure { Toast.makeText(activity, "Open Android Settings → Accessibility → M57", Toast.LENGTH_LONG).show() }
                })
                root.addView(LyraUi.text(activity, "You can continue without this permission and use the timer. App, video, browser and routine enforcement need the service. Usage access, DNS VPN and device administrator are separate optional permissions requested when you use those features.", 14f, LyraUi.muted))
                }
            }
            5 -> {
                heading("Make some room.", if (store.draftFamily) "You are ready to choose your parent password and finish the guided protection setup." else "Start with one small focus session. Your settings, history and goals stay on this phone.")
                root.addView(LyraUi.feature(activity, "01", if (store.draftLyra) "Lyra redirection enabled" else "Independent M57", "You can change this in protection settings."))
                root.addView(LyraUi.feature(activity, "02", if (accessible()) "Accessibility enabled" else "Timer ready; protection needs access", "Check connection health from Home whenever you need it."))
                root.addView(LyraUi.feature(activity, "03", "No ads. No paid switches.", "Source code and license notices are always available."))
            }
        }
        val actions = LyraUi.column(activity, 0)
        actions.addView(LyraUi.button(activity, if (step == 5) if (store.draftFamily) "Create parent password" else "Start using M57" else if (step == 4 && !store.draftFamily && !accessible()) "Continue with timer only" else "Continue") {
            if (step == 1 && !store.authorityChosen) { Toast.makeText(activity, "Choose For my child or For me to continue", Toast.LENGTH_LONG).show(); return@button }
            if (step == 5) { store.finishOnboarding(); complete() } else { store.onboardingStep = step + 1; show() }
        })
        if (step > 0) actions.addView(LyraUi.link(activity, "Previous step") { store.onboardingStep = step - 1; show() })
        footer(actions)
    }
}
