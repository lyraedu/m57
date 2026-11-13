// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.content.res.ColorStateList
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch

/** Editable plan, kept separate from a running session. Every start validates the complete plan. */
class FocusSetup(private val activity: Activity, private val focus: FocusStore, private val connected: Boolean,
    private val rules: () -> Unit, private val begin: (FocusDraft, () -> Unit) -> Unit) {
    fun addTo(root: LinearLayout) {
        val draft = focus.draft
        var mode = FocusMode.values().first { it.key == draft.kind }
        root.addView(LyraUi.section(activity, "Find your rhythm", "A short start counts, too."))
        val config = LyraUi.card(activity).apply { background = LyraUi.shape(LyraUi.sand, LyraUi.dp(activity, 24).toFloat()) }
        val intention = input("What are you making room for?", draft.title, 80, false)
        val time = input("Focus minutes · 1–480", draft.minutes, 3)
        val rest = input("Rest minutes · 1–60", draft.rest, 2)
        val rounds = input("Rounds · 2–12", draft.rounds, 2)
        val committed = Switch(activity).apply {
            text = "Commit until the timer ends"; minHeight = LyraUi.dp(activity, 56); setTextColor(LyraUi.ink)
            isChecked = draft.committed && mode != FocusMode.STOPWATCH
            buttonTintList = ColorStateList.valueOf(LyraUi.actionOrange)
        }
        fun snapshot() = FocusDraft(mode.key, intention.second.text.toString(), time.second.text.toString(),
            rest.second.text.toString(), rounds.second.text.toString(), committed.isChecked)
        val preview = LyraUi.text(activity, "", 15f, LyraUi.mossInk, true).apply {
            setPadding(LyraUi.dp(activity, 16), LyraUi.dp(activity, 16), LyraUi.dp(activity, 16), LyraUi.dp(activity, 16))
            background = LyraUi.shape(LyraUi.mossSoft, LyraUi.dp(activity, 16).toFloat())
        }
        val feedback = LyraUi.text(activity, "", 14f, LyraUi.actionOrange).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE; visibility = View.GONE
        }
        fun changed() {
            focus.draft = snapshot()
            feedback.visibility = View.GONE
            val minutes = time.second.text.toString().toIntOrNull()
            val breaks = rest.second.text.toString().toIntOrNull()
            val count = rounds.second.text.toString().toIntOrNull()
            preview.text = when {
                mode == FocusMode.STOPWATCH -> "Count up at your pace. Finish when you’re ready, up to 8 hours."
                minutes == null || minutes !in 1..480 -> "Choose your focus time to see the plan."
                mode == FocusMode.TIMER -> "$minutes minutes for one thing. A clear beginning and ending."
                breaks == null || breaks !in 1..60 || count == null || count !in 2..12 -> "Choose a rest length and 2–12 rounds."
                else -> "${minutes * count} minutes focused + ${breaks * (count - 1)} minutes of rest. $count rounds, ending after focus."
            }
        }
        val picker = FocusModePicker(activity, mode) { selected ->
            if (Motion.enabled(activity)) TransitionManager.beginDelayedTransition(root, AutoTransition().apply { duration = 200 })
            mode = selected
            config.visibility = if (mode == FocusMode.STOPWATCH) View.GONE else View.VISIBLE
            time.first.visibility = if (mode == FocusMode.STOPWATCH) View.GONE else View.VISIBLE
            rest.first.visibility = if (mode == FocusMode.POMODORO) View.VISIBLE else View.GONE
            rounds.first.visibility = rest.first.visibility
            committed.isEnabled = mode != FocusMode.STOPWATCH
            if (!committed.isEnabled) committed.isChecked = false
            changed()
        }
        root.addView(picker)
        root.addView(LyraUi.section(activity, "Make room for…", "Name one small intention.", green = true))
        root.addView(intention.first)
        val presets = LinearLayout(activity)
        listOf(15, 25, 45).forEach { value -> presets.addView(LyraUi.button(activity, "$value min", false) {
            time.second.setText(value.toString()); time.second.setSelection(time.second.length())
            if (Motion.enabled(activity)) Motion.enter(preview)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = LyraUi.dp(activity, 5) }) }
        time.first.addView(presets, 0)
        config.addView(time.first); config.addView(rest.first); config.addView(rounds.first)
        root.addView(config)
        time.first.visibility = if (mode == FocusMode.STOPWATCH) View.GONE else View.VISIBLE
        rest.first.visibility = if (mode == FocusMode.POMODORO) View.VISIBLE else View.GONE
        rounds.first.visibility = rest.first.visibility; committed.isEnabled = mode != FocusMode.STOPWATCH
        config.visibility = if (mode == FocusMode.STOPWATCH) View.GONE else View.VISIBLE
        root.addView(preview)
        root.addView(LyraUi.section(activity, "A little support", "Your focus boundaries stay separate from family protection."))
        root.addView(LyraUi.card(activity).apply {
            addView(LyraUi.feature(activity, "◎", "${focus.blockedApps.size} apps · ${focus.blockedSites.size} sites",
                if (connected) "Your chosen distractions wait during focus." else "Timer only for now. Android accessibility is needed to hold apps and sites."))
            addView(LyraUi.button(activity, "Choose focus apps and sites", false, rules))
        })
        root.addView(committed)
        root.addView(LyraUi.text(activity, "Commitment removes early finish inside M57. Android’s controls and calls stay available. Choose a duration that feels manageable.", 13f, LyraUi.muted))
        root.addView(feedback)
        root.addView(LyraUi.button(activity, "Begin my session") {
            val chosen = snapshot()
            val minutes = if (mode == FocusMode.STOPWATCH) 480 else chosen.minutes.toIntOrNull()
            val breaks = if (mode == FocusMode.POMODORO) chosen.rest.toIntOrNull() else 0
            val count = if (mode == FocusMode.POMODORO) chosen.rounds.toIntOrNull() else 1
            val valid = minutes != null && minutes in 1..480 && breaks != null && count != null &&
                (mode != FocusMode.POMODORO || (breaks in 1..60 && count in 2..12)) &&
                runCatching { FocusClock.state(mode.key, 0, minutes * 60_000L, breaks * 60_000L, count) }.isSuccess
            if (!valid) {
                feedback.text = "Choose valid minutes and rounds, with no more than 8 hours altogether."
                feedback.visibility = View.VISIBLE; feedback.requestFocus(); return@button
            }
            begin(chosen) {
                runCatching { focus.start(chosen.title, chosen.kind, minutes!!, breaks!!, count!!, chosen.committed) }
                    .onFailure { feedback.text = "Your session could not be saved. Check free storage and try again."; feedback.visibility = View.VISIBLE }
            }
        })
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
            override fun afterTextChanged(s: Editable?) {}
        }
        listOf(intention, time, rest, rounds).forEach { it.second.addTextChangedListener(watcher) }
        committed.setOnCheckedChangeListener { _, _ -> changed() }
        changed()
    }

    private fun input(label: String, value: String, length: Int, number: Boolean = true): Pair<LinearLayout, EditText> {
        val group = LyraUi.column(activity, 0)
        val editor = LyraUi.field(EditText(activity)).apply {
            id = View.generateViewId(); hint = label; setSingleLine(true)
            inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(length)); setText(value)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        group.addView(LyraUi.text(activity, label, 13f, LyraUi.muted, true).apply { labelFor = editor.id })
        group.addView(editor)
        return group to editor
    }
}
