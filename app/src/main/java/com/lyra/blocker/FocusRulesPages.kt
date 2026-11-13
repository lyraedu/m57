// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import com.lyra.blocker.family.BrowserDomains
import com.lyra.blocker.family.HostRules
import java.util.UUID
import java.util.concurrent.Executors

/** Configuration UI. All writes recheck authority and any active commitment. */
class FocusRulesPages(private val activity: Activity, private val authorised: () -> Boolean, private val navigate: (AdminPage) -> Unit) {
    private val store = FocusStore(activity)
    private val limits = AppLimitStore(activity)
    private val worker = Executors.newSingleThreadExecutor()
    private var generation = 0
    private var dialog: AlertDialog? = null
    fun stop() { generation++; dialog?.dismiss(); dialog = null }
    fun close() { stop(); worker.shutdown() }
    private fun canEdit(): Boolean {
        if (!authorised()) return false
        val s = store.session
        if (s != null && s.strict && !store.state(s).finished()) { toast("This commitment session must finish before changing focus boundaries"); return false }
        return true
    }
    private fun heading(page: LinearLayout, title: String, detail: String) {
        page.addView(LyraUi.text(activity, title, 30f, bold = true)); page.addView(LyraUi.text(activity, detail, 15f, LyraUi.muted))
    }
    fun focus(page: LinearLayout) {
        heading(page, "Give distractions\na place to wait.", "This list applies during your personal focus sessions and enabled routines. It has no effect on administrator pauses or family categories.")
        Illustrations.addTo(page, "one-thing", 160)
        page.addView(LyraUi.text(activity, "Apps to hold", 20f, bold = true))
        val summary = LyraUi.text(activity, "${store.blockedApps.size} apps selected", 14f, LyraUi.muted); page.addView(summary)
        val chosen = store.blockedApps.toMutableSet()
        appList(page) { app, rows ->
            rows.addView(CheckBox(activity).apply {
                text = app.label; textSize = 16f; minHeight = LyraUi.dp(activity, 52); setTextColor(LyraUi.ink); isChecked = app.packageName in chosen
                setOnCheckedChangeListener { _, enabled ->
                    if (!canEdit()) { isChecked = app.packageName in chosen; return@setOnCheckedChangeListener }
                    if (enabled) chosen.add(app.packageName) else chosen.remove(app.packageName)
                    store.blockedApps = chosen.toSet(); summary.text = "${chosen.size} apps selected"
                }
            })
        }
        page.addView(LyraUi.text(activity, "Websites to hold", 20f, bold = true))
        page.addView(LyraUi.text(activity, "One domain per line, such as example.com. Subdomains are included. These rules need a visible address in a supported browser; they are separate from the family DNS VPN.", 14f, LyraUi.muted))
        val blocked = domainField(page, "Blocked focus websites", store.blockedSites)
        val study = Switch(activity).apply {
            text = "Study browsing · only allow listed sites during focus"; minHeight = LyraUi.dp(activity, 60); setTextColor(LyraUi.ink); isChecked = store.studyBrowser
        }; page.addView(study)
        val allowed = domainField(page, "Study websites to allow", store.allowedSites)
        page.addView(LyraUi.text(activity, "Study allow entries take priority over your personal focus-site blocklist. They cannot override administrator family-category rules. Hidden addresses and unsupported browsers are not covered.", 13f, LyraUi.muted))
        page.addView(LyraUi.button(activity, "Save website boundaries") {
            if (!canEdit()) return@button
            val deny = parseDomains(blocked) ?: return@button
            val allow = parseDomains(allowed) ?: return@button
            if (study.isChecked && allow.isEmpty()) { allowed.error = "Add at least one study website first"; return@button }
            store.blockedSites = deny; store.allowedSites = allow; store.studyBrowser = study.isChecked; toast("Website boundaries saved")
        })
        page.addView(LyraUi.text(activity, "Calls, Settings, M57 and your launcher are excluded from the picker. Browsers may be held as whole apps if you choose them; leave them available for website-specific rules.", 13f, LyraUi.muted))
    }
    fun limits(page: LinearLayout) {
        heading(page, "Enough for today.", "Choose an app and a daily allowance. Limits reset at local midnight and need both usage access and accessibility.")
        Illustrations.addTo(page, "daily-allowance", 180)
        val status = LyraUi.text(activity, if (store.usageEnabled && UsageAccess.granted(activity)) "Local usage access is ready" else "Limits are waiting for usage access", 16f, LyraUi.orange, true); page.addView(status)
        page.addView(LyraUi.text(activity, "M57 reads Android's app identities and foreground-time events on this phone. It processes them locally without saving raw events or uploading usage. Estimates may differ in split screen or after Android drops old events. Limits are checked about every 15 seconds while the service is connected.", 14f, LyraUi.muted))
        page.addView(LyraUi.button(activity, "Enable local usage access", false) {
            if (!authorised()) return@button
            dialog = AlertDialog.Builder(activity).setTitle("Use app-time estimates locally?")
                .setMessage("Android will grant access to app-use events. M57 uses those events for your insights and daily limits. It does not store a timeline or send this data away. You can turn this off here or revoke access in Android.")
                .setNegativeButton("Cancel", null).setPositiveButton("Agree & open Android") { _, _ ->
                    if (authorised()) {
                        store.usageEnabled = true
                        runCatching { activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }.onFailure { toast("Open Android Settings → Special app access → Usage access → M57") }
                    }
                }.show().also(LyraUi::styleDialog)
        })
        page.addView(LyraUi.button(activity, "Turn off local usage processing", false) {
            if (canEdit()) { store.usageEnabled = false; navigate(AdminPage.APP_LIMITS) }
        })
        page.addView(LyraUi.text(activity, "Your current limits", 20f, bold = true))
        if (limits.limits.isEmpty()) page.addView(LyraUi.text(activity, "Nothing limited yet. Choose an app below.", 14f, LyraUi.muted))
        limits.limits.forEach { (pkg, minutes) ->
            val name = runCatching { activity.packageManager.getApplicationLabel(activity.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
            page.addView(LyraUi.button(activity, "$name · $minutes min", false) { editLimit(InstalledApp(pkg, name), minutes) })
        }
        page.addView(LyraUi.text(activity, "Choose an app", 20f, bold = true))
        appList(page) { app, rows -> rows.addView(LyraUi.button(activity, app.label, false) { editLimit(app, limits.limits[app.packageName] ?: 30) }) }
    }
    private fun editLimit(app: InstalledApp, minutes: Int) {
        if (!canEdit()) return
        val field = LyraUi.field(EditText(activity)).apply { inputType = InputType.TYPE_CLASS_NUMBER; hint = "Minutes per day · 1–1440"; setText(minutes.toString()) }
        dialog = AlertDialog.Builder(activity).setTitle(app.label).setView(field).setNegativeButton("Cancel", null)
            .setNeutralButton("Remove limit") { _, _ -> if (canEdit()) { limits.set(app.packageName, 0); navigate(AdminPage.APP_LIMITS) } }
            .setPositiveButton("Save", null).show().also { alert ->
                LyraUi.styleDialog(alert)
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value = field.text.toString().toIntOrNull()
                    if (value == null || value !in 1..1440) field.error = "Choose 1–1440 minutes"
                    else if (canEdit()) { limits.set(app.packageName, value); alert.dismiss(); navigate(AdminPage.APP_LIMITS) }
                }
            }
    }
    fun routines(page: LinearLayout) {
        heading(page, "Let a routine\nhold the space.", "Repeat your focus list on selected days. Times follow this phone's timezone, including overnight windows.")
        Illustrations.addTo(page, "routine-rhythm", 180)
        page.addView(LyraUi.text(activity, "Routines need a connected accessibility service. Android power restrictions may delay checks. A routine uses the same app and website list as your focus timer; its schedule is independent of timer breaks.", 14f, LyraUi.muted))
        page.addView(LyraUi.button(activity, "Add a routine") { if (canEdit()) editRoutine(null) })
        if (store.routines.isEmpty()) page.addView(LyraUi.text(activity, "An evening reading hour? A quiet school morning? Start with a routine you can keep.", 16f, LyraUi.muted))
        store.routines.forEach { routine ->
            page.addView(LyraUi.card(activity).apply {
                addView(LyraUi.text(activity, routine.title, 21f, bold = true))
                addView(LyraUi.text(activity, "${time(routine.start)}–${time(routine.end)} · ${routine.days.sorted().joinToString(" ") { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[it - 1] }}", 14f, LyraUi.muted))
                addView(Switch(activity).apply {
                    text = "Routine enabled"; isChecked = routine.enabled; minHeight = LyraUi.dp(activity, 52)
                    setOnCheckedChangeListener { _, enabled -> if (canEdit()) store.saveRoutines(store.routines.map { if (it.id == routine.id) it.copy(enabled = enabled) else it }) else isChecked = routine.enabled }
                })
                addView(LyraUi.button(activity, "Edit routine", false) { if (canEdit()) editRoutine(routine) })
            })
        }
        page.addView(LyraUi.button(activity, "Choose the focus list", false) { navigate(AdminPage.FOCUS_RULES) })
    }
    private fun editRoutine(existing: FocusRoutine?) {
        if (existing == null && store.routines.size >= 30) { toast("You can keep up to 30 routines"); return }
        val body = LyraUi.column(activity, 16)
        val name = LyraUi.field(EditText(activity)).apply { hint = "Routine name"; setSingleLine(true); filters = arrayOf(android.text.InputFilter.LengthFilter(80)); setText(existing?.title.orEmpty()) }; body.addView(name)
        var start = existing?.start ?: 18 * 60
        var end = existing?.end ?: 19 * 60
        lateinit var startButton: android.widget.Button
        lateinit var endButton: android.widget.Button
        startButton = LyraUi.button(activity, "Starts ${time(start)}", false) {
            TimePickerDialog(activity, { _, hour, minute -> start = hour * 60 + minute; startButton.text = "Starts ${time(start)}" }, start / 60, start % 60, android.text.format.DateFormat.is24HourFormat(activity)).show()
        }; body.addView(startButton)
        endButton = LyraUi.button(activity, "Ends ${time(end)}", false) {
            TimePickerDialog(activity, { _, hour, minute -> end = hour * 60 + minute; endButton.text = "Ends ${time(end)}" }, end / 60, end % 60, android.text.format.DateFormat.is24HourFormat(activity)).show()
        }; body.addView(endButton)
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").mapIndexed { i, label ->
            CheckBox(activity).apply { text = label; minHeight = LyraUi.dp(activity, 48); isChecked = i + 1 in (existing?.days ?: (1..5).toSet()); body.addView(this) }
        }
        val scroll = android.widget.ScrollView(activity).apply { addView(body) }
        dialog = AlertDialog.Builder(activity).setTitle(if (existing == null) "New routine" else "Edit routine").setView(scroll).setNegativeButton("Cancel", null)
            .setNeutralButton(if (existing == null) "" else "Delete") { _, _ ->
                if (existing != null && canEdit()) { store.saveRoutines(store.routines.filterNot { it.id == existing.id }); navigate(AdminPage.ROUTINES) }
            }.setPositiveButton("Save", null).show().also { alert ->
                LyraUi.styleDialog(alert)
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val selected = days.mapIndexedNotNull { i, box -> if (box.isChecked) i + 1 else null }.toSet()
                    when {
                        name.text.toString().isBlank() -> name.error = "Give your routine a name"
                        selected.isEmpty() -> toast("Choose at least one day")
                        start == end -> toast("Start and end must be different")
                        canEdit() -> {
                            val routine = FocusRoutine(existing?.id ?: UUID.randomUUID().toString(), name.text.toString().trim(), selected, start, end, existing?.enabled ?: true)
                            store.saveRoutines(store.routines.filterNot { it.id == routine.id } + routine); alert.dismiss(); navigate(AdminPage.ROUTINES)
                        }
                    }
                }
            }
    }
    private fun appList(page: LinearLayout, row: (InstalledApp, LinearLayout) -> Unit) {
        val search = LyraUi.field(EditText(activity)).apply { hint = "Search installed apps"; setSingleLine(true) }; page.addView(search)
        val result = LyraUi.column(activity, 0); result.addView(LyraUi.text(activity, "Finding launcher apps on this phone…", 14f, LyraUi.muted)); page.addView(result)
        val ticket = generation
        worker.execute {
            val apps = runCatching { AppCatalog.load(activity) }
            activity.runOnUiThread {
                if (ticket != generation || activity.isDestroyed) return@runOnUiThread
                fun render(query: String) {
                    result.removeAllViews()
                    if (apps.isFailure) { result.addView(LyraUi.text(activity, "Android could not list apps. Return to this page to try again.", 14f, LyraUi.muted)); return }
                    val filtered = apps.getOrThrow().filter { "${it.label} ${it.packageName}".contains(query.trim(), true) }
                    if (filtered.isEmpty()) result.addView(LyraUi.text(activity, "No matching launcher apps", 14f, LyraUi.muted))
                    filtered.take(60).forEach { row(it, result) }
                    if (filtered.size > 60) result.addView(LyraUi.text(activity, "Showing 60 of ${filtered.size}. Search to find more.", 12f, LyraUi.muted))
                }
                render(search.text.toString())
                search.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
                    override fun afterTextChanged(s: Editable?) {}
                })
            }
        }
    }
    private fun domainField(page: LinearLayout, title: String, values: Set<String>) = LyraUi.field(EditText(activity)).apply {
        page.addView(LyraUi.text(activity, title, 14f, bold = true)); hint = "example.com"; minLines = 3
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        setText(values.sorted().joinToString("\n")); filters = arrayOf(android.text.InputFilter.LengthFilter(100_000)); page.addView(this)
    }
    private fun parseDomains(field: EditText): Set<String>? {
        val raw = field.text.toString().lines().map(String::trim).filter(String::isNotEmpty)
        val domains = raw.map(HostRules::domain)
        if (raw.size > 500 || domains.any { it == null }) { field.error = "Use at most 500 valid domains, one per line"; return null }
        return domains.filterNotNull().toSet()
    }
    private fun time(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
    private fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
}
