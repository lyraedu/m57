// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast

/** Explicit authority choices followed by optional, feature-specific permission stories. */
class OnboardingPages(private val activity: Activity, private val page: () -> LinearLayout,
    private val accessible: () -> Boolean, private val footer: (LinearLayout) -> Unit,
    private val back: (() -> Unit) -> Unit, private val complete: () -> Unit) {
    private enum class Step { WELCOME, PERSON, LYRA, VIDEOS, PARENT_PASSWORD, OPEN_SOURCE, ACCESS, USAGE, BACKGROUND, NOTIFICATIONS, READY }
    private val store = ProtectionStore(activity)
    private val personal = listOf(Step.WELCOME, Step.PERSON, Step.LYRA, Step.VIDEOS, Step.OPEN_SOURCE, Step.ACCESS, Step.USAGE, Step.BACKGROUND, Step.NOTIFICATIONS, Step.READY)
    private val parent = listOf(Step.WELCOME, Step.PERSON, Step.LYRA, Step.VIDEOS, Step.PARENT_PASSWORD)
    private val stories = PermissionStories(activity, {
        // A fresh personal choice may grant only the explained capabilities, never replace a parent credential.
        if (store.hasPassword || store.draftFamily || !store.authorityChosen) false else {
            store.prefs.edit().putString("authority", "personal").putBoolean("ai_enabled", store.draftLyra)
                .putBoolean("settings_guard", false).putBoolean("repo_guard", false).apply()
            true
        }
    }, { show() })

    fun show() {
        val flow = if (store.draftFamily) parent else personal
        val index = store.onboardingStep.coerceAtMost(flow.lastIndex)
        val step = flow[index]
        val root = page()
        back { if (index > 0) { store.onboardingStep = index - 1; show() } else activity.finish() }
        root.addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = flow.size; setProgress(index + 1, Motion.enabled(activity))
            progressTintList = android.content.res.ColorStateList.valueOf(LyraUi.moss)
            contentDescription = "Introduction step ${index + 1} of ${flow.size}"
        })
        root.addView(LyraUi.badge(activity, "YOUR CHOICES · ${index + 1}/${flow.size}", LyraUi.mossInk, LyraUi.mossSoft))
        fun heading(title: String, detail: String, art: String) {
            Illustrations.addTo(root, art, 190)
            root.addView(LyraUi.text(activity, title, 31f, bold = true).apply { isAccessibilityHeading = true })
            root.addView(LyraUi.text(activity, detail, 16f, LyraUi.muted))
        }
        when (step) {
            Step.WELCOME -> heading("Your phone.\nYour attention.", "Make room for the things you meant to do. Focus time, fewer endless videos, and browsing boundaries. Free, open source, and yours to choose.", "attention-garden")
            Step.PERSON -> {
                heading("Who is this\nphone for?", "A little help for your child, or a little more space for yourself. Choose the setup that fits.", "personal-controls")
                val group = RadioGroup(activity)
                val family = RadioButton(activity).apply {
                    id = android.view.View.generateViewId(); text = "For my child\nA parent password protects the settings"; minHeight = LyraUi.dp(activity, 88); setTextColor(LyraUi.ink)
                    background = LyraUi.shape(LyraUi.mossSoft, LyraUi.dp(activity, 20).toFloat()); setPadding(LyraUi.dp(activity, 12), LyraUi.dp(activity, 12), LyraUi.dp(activity, 12), LyraUi.dp(activity, 12))
                }
                val individual = RadioButton(activity).apply {
                    id = android.view.View.generateViewId(); text = "For me\nI choose and change my own boundaries"; minHeight = LyraUi.dp(activity, 88); setTextColor(LyraUi.ink)
                    background = LyraUi.shape(LyraUi.apricotSoft, LyraUi.dp(activity, 20).toFloat()); setPadding(LyraUi.dp(activity, 12), LyraUi.dp(activity, 12), LyraUi.dp(activity, 12), LyraUi.dp(activity, 12))
                }
                group.addView(family); group.addView(individual, RadioGroup.LayoutParams(-1, -2).apply { topMargin = LyraUi.dp(activity, 10) })
                if (store.authorityChosen) group.check(if (store.draftFamily) family.id else individual.id)
                group.setOnCheckedChangeListener { _, chosen -> store.draftFamily = chosen == family.id }; root.addView(group)
                root.addView(LyraUi.text(activity, "Both setups work with or without Lyra. Neither needs an M57 account. In family mode, your child can use the timer while settings stay with you.", 14f, LyraUi.muted))
            }
            Step.LYRA -> {
                heading("Bring Lyra along,\nif you like.", "M57 can guide supported AI apps and screens into Lyra’s learning space. Leave this off to use M57 independently.", "learning-detour")
                root.addView(Switch(activity).apply {
                    text = "Include Lyra for AI learning"; textSize = 17f; minHeight = LyraUi.dp(activity, 60); setTextColor(LyraUi.ink)
                    isChecked = store.draftLyra; setOnCheckedChangeListener { _, checked -> store.draftLyra = checked }
                })
                root.addView(LyraUi.card(activity).apply { addView(LyraUi.feature(activity, "◇", "Your other tools stay yours", "Focus, video and website controls work without Lyra. An optional handoff sends only a source-family hint and a request for a new chat.")) })
            }
            Step.VIDEOS -> {
                heading("One more video?\nMake it a choice.", "Choose supported short-video viewers you want a hand with. Ordinary videos and messages use different screens.", "short-video-choice")
                listOf("YouTube Shorts" to store.youtubeEnabled, "Instagram Reels" to store.instagramEnabled).forEach { (label, enabled) ->
                    root.addView(Switch(activity).apply {
                        text = label; textSize = 16f; minHeight = LyraUi.dp(activity, 60); setTextColor(LyraUi.ink); isChecked = enabled
                        setOnCheckedChangeListener { _, value -> if (label == "YouTube Shorts") store.youtubeEnabled = value else store.instagramEnabled = value }
                    })
                }
                root.addView(LyraUi.text(activity, "Choose blocking or gentle reminders in your controls. Recognition needs accessibility and can change with app updates or languages.", 14f, LyraUi.muted))
            }
            Step.PARENT_PASSWORD -> {
                heading("Keep the controls\nwith you.", "Next, choose a private parent password. Then we’ll walk through each Android permission, one at a time.", "personal-controls")
                root.addView(LyraUi.card(activity).apply { addView(LyraUi.feature(activity, "♡", "A phrase only you know", "Keep a private copy away from the child’s phone. Assisted recovery is not available in this preview.")) })
                root.addView(LyraUi.text(activity, "You’ll see what each permission does, what it can access and how to continue without optional tools. Strict-mode settings have their own page.", 15f, LyraUi.muted))
            }
            Step.OPEN_SOURCE -> stories.openSource(root)
            Step.ACCESS -> stories.accessibility(root)
            Step.USAGE -> stories.usage(root)
            Step.BACKGROUND -> stories.background(root)
            Step.NOTIFICATIONS -> stories.notifications(root)
            Step.READY -> {
                heading("Make some room.", "Start with one small focus session. Add the boundaries that help, at your pace.", "ready-for-your-day")
                root.addView(LyraUi.feature(activity, "◎", "Your timer is ready", "No account or Android permission needed to make a start."))
                root.addView(LyraUi.feature(activity, "♡", if (accessible()) "Android accessibility enabled" else "Screen controls are waiting", "Home shows the actual service connection and the state of each tool."))
                root.addView(LyraUi.feature(activity, "◷", if (stories.usageReady()) "Local app-time access ready" else "App-time insights remain optional", "You can review every access choice from Settings later."))
            }
        }
        val waiting = when (step) {
            Step.ACCESS -> !accessible()
            Step.USAGE -> !stories.usageReady()
            Step.BACKGROUND -> !BackgroundAccess.isExempt(activity)
            Step.NOTIFICATIONS -> !stories.notificationsReady()
            else -> false
        }
        val actions = LyraUi.column(activity, 0)
        val feedback = ActionFeedback(activity); root.addView(feedback)
        val last = index == flow.lastIndex
        actions.addView(LyraUi.button(activity, when {
            step == Step.PARENT_PASSWORD -> "Create parent password"
            last -> "Start using M57"
            waiting -> "Not now · continue"
            else -> "Continue"
        }) {
            if (step == Step.PERSON && !store.authorityChosen) { Toast.makeText(activity, "Choose For my child or For me to continue", Toast.LENGTH_LONG).show(); return@button }
            if (last) {
                if (!store.authorityChosen) { store.onboardingStep = 1; show(); return@button }
                runCatching { store.finishOnboarding() }.onSuccess { complete() }
                    .onFailure { feedback.show("Your choices could not be saved. Check free storage and try again.", error = true) }
            } else { store.onboardingStep = index + 1; show() }
        })
        if (index > 0) actions.addView(LyraUi.link(activity, "Previous step") { store.onboardingStep = index - 1; show() })
        footer(actions)
    }
}
