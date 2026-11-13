// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.Toast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

/** Account-free personal experience. Administrator configuration remains behind the owning guard. */
class PersonalPages(private val activity: Activity, private val page: (String) -> LinearLayout,
    private val admin: (AdminPage) -> Unit, private val accessible: () -> Boolean, private val licenses: () -> Unit) {
    private val store = ProtectionStore(activity)
    private val focus = FocusStore(activity)
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var generation = 0
    private var dialog: AlertDialog? = null
    var currentTab = "Home"
        private set
    fun stop() { generation++; handler.removeCallbacksAndMessages(null); dialog?.dismiss(); dialog = null }
    fun close() { stop(); worker.shutdown() }
    fun show(tab: String = currentTab) {
        stop(); currentTab = tab
        focus.session?.let { if (focus.state(it).finished()) focus.finish() }
        val root = page(tab)
        when (tab) {
            "Home" -> home(root)
            "Focus" -> timer(root)
            "Plan" -> plan(root)
            "Insights" -> insights(root)
            else -> settings(root)
        }
    }
    private fun title(root: LinearLayout, text: String, detail: String) {
        root.addView(LyraUi.text(activity, text, 32f, bold = true).apply { isAccessibilityHeading = true }); root.addView(LyraUi.text(activity, detail, 15f, LyraUi.muted))
    }
    private fun card(root: LinearLayout, symbol: String, title: String, detail: String, action: () -> Unit) {
        root.addView(LyraUi.routeCard(activity, symbol, title, detail, action))
    }
    private fun home(root: LinearLayout) {
        val active = focus.session
        val todayFocused = focus.todayFocused
        root.addView(LyraUi.badge(activity, if (active != null) "TIME YOU CHOSE" else "YOUR ATTENTION BELONGS TO YOU"))
        title(root, if (store.familyManaged) "A calmer phone.\nA clearer day." else "Room for\nwhat matters.",
            if (store.familyManaged) "Check the boundaries on this phone. Parent controls are one password away." else "Focus time, fewer endless videos, and boundaries you choose.")
        if (!FocusStore.storageHealthy) root.addView(LyraUi.text(activity, "M57 could not save the latest focus change. Check free storage, and export your history before closing the app.", 15f, LyraUi.orange))
        if (store.familyManaged) {
            protectionSummary(root)
            root.addView(LyraUi.button(activity, "Parent controls") { admin(AdminPage.OVERVIEW) })
        }
        if (active != null) card(root, "◷", "Your session is under way", "${active.title} · return to your timer") { show("Focus") }
        else root.addView(LyraUi.button(activity, "Start a focus session") { show("Focus") })
        root.addView(LyraUi.section(activity, "A little space, today", "Small moments add up.", green = true))
        root.addView(SummaryTiles(activity, listOf(
            SummaryMetric("Time for yourself", "${todayFocused / 60_000} min", "Focused today", M57Icon.FOCUS),
            SummaryMetric("Your daily direction", "${focus.goalMinutes} min", "A goal, without pressure", M57Icon.PLAN, true)
        )))
        val art = FocusArt(activity).apply { progress = todayFocused.toFloat() / (focus.goalMinutes * 60_000f) }
        if (!Illustrations.addTo(root, "attention-garden", 230)) root.addView(art, LinearLayout.LayoutParams(-1, LyraUi.dp(activity, 230)))
        if (!store.familyManaged) protectionSummary(root)
        root.addView(LyraUi.section(activity, "Choose what helps", "Each boundary has its own controls."))
        card(root, "Ⅱ", "Break the scroll", "Short-video controls with a blocking or reminder choice.") { admin(AdminPage.SHORT_VIDEOS) }
        card(root, "◎", "Make a plan", "Choose focus apps, daily limits and recurring routines.") { show("Plan") }
        card(root, "◇", "Browse with boundaries", "Choose local website categories and domain exceptions.") { admin(AdminPage.FAMILY) }
        if (store.aiEnabled) root.addView(LyraUi.button(activity, "Open Lyra  ↗", false) {
            if (!LyraLauncher.open(activity, store)) toast("Install a browser to open Lyra")
        })
        root.addView(LyraUi.link(activity, "How these controls work") { preview() })
    }
    private fun protectionSummary(root: LinearLayout) {
        val connected = accessible() && LyraAccessibilityService.connected
        val family = com.lyra.blocker.family.FamilyStore(activity)
        val rules = com.lyra.blocker.family.FamilyRepository.get(activity)
        val limits = AppLimitStore(activity).limits
        val enabledVideos = listOfNotNull("Shorts".takeIf { store.youtubeEnabled }, "Reels".takeIf { store.instagramEnabled })
        fun readiness(enabled: Boolean): String = when {
            !enabled -> "Off"
            store.paused -> "Paused"
            !connected -> "Needs Android access"
            else -> "On"
        }
        root.addView(LyraUi.card(activity).apply {
            addView(LyraUi.badge(activity, if (accessible() && LyraAccessibilityService.connected) "ACCESSIBILITY CONNECTED" else "PROTECTION NEEDS ATTENTION"))
            addView(LyraUi.text(activity, when {
                store.paused -> "Protection is paused."
                !accessible() -> "The timer works. App protection needs Android access."
                !LyraAccessibilityService.connected -> "Access is enabled. Waiting for Android to connect M57."
                else -> "Your chosen screen controls are ready."
            }, 18f, bold = true))
            addView(LyraUi.text(activity, "Short videos · ${readiness(enabledVideos.isNotEmpty() || store.extraVideosEnabled)}${if (enabledVideos.isNotEmpty()) " · ${enabledVideos.joinToString(" / ")}" else ""}", 14f))
            val websites = when {
                !family.vpnConfigured && family.blockedUrls.isEmpty() -> "Not set up"
                store.paused -> "Paused"
                family.resolverMode != com.lyra.blocker.family.ResolverPolicy.NETWORK && com.lyra.blocker.family.FamilyVpnService.running -> "DNS provider active · browser rules separate"
                rules.index.size() == 0 && family.blockedUrls.isEmpty() -> "Waiting for website lists or VPN setup"
                com.lyra.blocker.family.FamilyVpnService.running || (family.browserEnabled && connected) -> "On for configured websites"
                else -> "Needs setup"
            }
            addView(LyraUi.text(activity, "Website filtering · $websites", 14f))
            addView(LyraUi.text(activity, "App limits · ${if (limits.isEmpty()) "None chosen" else if (!focus.usageEnabled || !UsageAccess.granted(activity) || !connected) "Needs usage access" else if (store.paused) "Paused" else "${limits.size} daily allowances"}", 14f))
            addView(LyraUi.text(activity, "Lyra · ${if (!store.aiEnabled) "Off · M57 works independently" else readiness(true)}", 14f))
            if (!connected) addView(LyraUi.button(activity, "Finish Android access", false) { admin(AdminPage.DEVICE_ACCESS) })
            else if (store.paused) addView(LyraUi.button(activity, "Review protection pause", false) { admin(AdminPage.PAUSE) })
        })
    }
    private fun timer(root: LinearLayout) {
        val session = focus.session
        if (session != null) { running(root, session); return }
        title(root, "One thing\nat a time.", "Pick a timer, name your intention, and put the distractions on hold.")
        Illustrations.addTo(root, "one-thing", 180)
        FocusSetup(activity, focus, accessible() && LyraAccessibilityService.connected,
            rules = { admin(AdminPage.FOCUS_RULES) }) { draft, start ->
            val launch = { start(); if (focus.session != null) show("Focus"); Unit }
            if (draft.committed) confirm("Keep this commitment?", "There is no early-end button for this session. Choose a realistic duration; calls and Android controls stay available.", launch)
            else launch()
        }.addTo(root)
    }
    private fun running(root: LinearLayout, session: FocusSession) {
        title(root, session.title, if (session.strict) "A commitment you made to yourself." else "Stay with one thing. You can finish early if you need to.")
        val art = FocusArt(activity).apply { showMark = false }
        root.addView(android.widget.FrameLayout(activity).apply {
            addView(art, android.widget.FrameLayout.LayoutParams(-1, -1))
            addView(android.widget.ImageView(activity).apply {
                setImageResource(R.drawable.m57_logo); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, android.widget.FrameLayout.LayoutParams(LyraUi.dp(activity, 88), LyraUi.dp(activity, 88), Gravity.CENTER))
        }, LinearLayout.LayoutParams(-1, LyraUi.dp(activity, 240)))
        root.addView(LyraUi.text(activity, if (session.kind == "stopwatch") "TIME YOU’VE MADE" else "TIME LEFT IN THIS ROUND", 11f, LyraUi.muted, true).apply { gravity = Gravity.CENTER })
        val clock = LyraUi.text(activity, "", 52f, bold = true).apply { gravity = Gravity.CENTER }; root.addView(clock)
        val phase = LyraUi.text(activity, "", 17f, LyraUi.actionOrange, true).apply {
            gravity = Gravity.CENTER; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPadding(LyraUi.dp(activity, 14), LyraUi.dp(activity, 14), LyraUi.dp(activity, 14), LyraUi.dp(activity, 14))
        }; root.addView(phase)
        val rounds = FocusRounds(activity, session.rounds)
        if (session.kind == "pomodoro") root.addView(rounds, LinearLayout.LayoutParams(-1, LyraUi.dp(activity, 24)))
        val detail = LyraUi.text(activity, "", 14f, LyraUi.muted).apply { gravity = Gravity.CENTER }; root.addView(detail)
        val tick = object : Runnable {
            override fun run() {
                val state = focus.state(session)
                if (state.finished()) { focus.finish(); show("Insights"); return }
                val value = if (session.kind == "stopwatch") state.focused() else state.remaining()
                clock.text = formatTime(value)
                val next = if (state.phase() == "break") "Take a breath · break ${state.round()}" else if (session.kind == "pomodoro") "Focus · round ${state.round()} of ${session.rounds}" else "Your focus time"
                val resting = state.phase() == "break"
                if (phase.text.toString() != next) {
                    phase.text = next
                    phase.setTextColor(if (resting) LyraUi.mossInk else LyraUi.actionOrange)
                    phase.background = LyraUi.shape(if (resting) LyraUi.mossSoft else LyraUi.apricotSoft, LyraUi.dp(activity, 18).toFloat())
                    Motion.enter(phase)
                }
                art.resting = resting; rounds.update(state.round(), resting)
                detail.text = "${state.focused() / 60_000} minutes focused${if (store.paused) " · administrator protection paused" else ""}"
                art.progress = if (session.kind == "stopwatch") 0.7f else (state.remaining().toFloat() / (if (state.phase() == "break") session.rest else session.focus)).coerceIn(0f, 1f)
                handler.postDelayed(this, 1000)
            }
        }; handler.post(tick)
        root.addView(LyraUi.section(activity, "Let the rest wait", "A quieter phone, while you’re here.", green = true))
        root.addView(SummaryTiles(activity, listOf(
            SummaryMetric("Focus list", "${focus.blockedApps.size} apps", "Chosen in your plan", M57Icon.FOCUS),
            SummaryMetric("Website boundaries", "${focus.blockedSites.size} sites", if (focus.studyBrowser) "Study allowlist also active" else "Supported browser addresses", M57Icon.BROWSER, true)
        )))
        root.addView(LyraUi.text(activity, if (session.kind == "pomodoro") "Your chosen apps return during each scheduled break. Family browsing and short-video settings keep their own rules." else "Your chosen focus apps and sites return when this session ends.", 15f, LyraUi.muted))
        if (!session.strict) root.addView(LyraUi.button(activity, "Finish session", false) {
            confirm("Finish this session?", "Save the time you focused and release this session's focus list. Other protection stays active.") { if (focus.finish(manual = true)) show("Insights") }
        })
        root.addView(LyraUi.link(activity, "Back to my day") { show("Home") })
    }
    private fun plan(root: LinearLayout) {
        title(root, "Fewer decisions.\nMore intention.", "Set boundaries once, then let your day unfold.")
        Illustrations.addTo(root, "routine-rhythm", 180)
        root.addView(LyraUi.section(activity, "Your boundaries", "Choose the distractions you want a hand with."))
        card(root, "◎", "Focus list", "${focus.blockedApps.size} apps · ${focus.blockedSites.size} websites · ${if (focus.studyBrowser) "study allowlist on" else "study allowlist off"}") { admin(AdminPage.FOCUS_RULES) }
        card(root, "◷", "Daily app limits", "${AppLimitStore(activity).limits.size} chosen apps. Reset at local midnight.") { admin(AdminPage.APP_LIMITS) }
        card(root, "▦", "Routines", "${focus.routines.count { it.enabled }} enabled. Repeat on the days you choose.") { admin(AdminPage.ROUTINES) }
        root.addView(LyraUi.section(activity, "Your rhythm", "Make a little room each day.", green = true))
        root.addView(LyraUi.card(activity).apply {
            background = LyraUi.shape(LyraUi.mossSoft, LyraUi.dp(activity, 24).toFloat())
            addView(LyraUi.text(activity, "A daily focus goal", 21f, bold = true))
            addView(LyraUi.text(activity, "A direction for your day, without streaks or penalties.", 14f, LyraUi.muted))
            val goal = field(this, "Goal minutes · 5–480", focus.goalMinutes.toString(), true)
            addView(LyraUi.button(activity, "Save my goal", false) {
                val value = goal.text.toString().toIntOrNull()
                if (value == null || value !in 5..480) goal.error = "Choose 5–480 minutes" else { focus.goalMinutes = value; FocusWidget.update(activity); toast("Goal saved") }
            })
        })
        root.addView(LyraUi.text(activity, "Focus lists and routines use accessibility. Website rules need a visible address in a supported browser. A daily limit also needs local usage access. Connection health is shown on Home.", 14f, LyraUi.muted))
        if (store.familyManaged) root.addView(LyraUi.badge(activity, "BOUNDARY CHANGES NEED ADMINISTRATOR ACCESS"))
    }
    private fun insights(root: LinearLayout) {
        title(root, "A little perspective.", "Your own focus history and optional app-use estimates. Kept on this phone.")
        Illustrations.addTo(root, if (focus.history.isEmpty()) "first-page" else "local-history", 170)
        val history = focus.history
        val minutes = focus.todayFocused / 60_000
        root.addView(LyraUi.card(activity).apply {
            addView(LyraUi.text(activity, "$minutes min", 40f, bold = true))
            addView(LyraUi.text(activity, "For what matters, today", 15f, LyraUi.muted))
            addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = focus.goalMinutes; progress = minutes.toInt().coerceAtMost(max)
                progressTintList = android.content.res.ColorStateList.valueOf(LyraUi.orange)
                contentDescription = "$minutes of ${focus.goalMinutes} goal minutes"
            })
        })
        root.addView(LyraUi.section(activity, "The past seven days", "Focus time, without scores or streaks.", green = true))
        val today = LocalDate.now()
        (6 downTo 0).forEach { offset ->
            val day = today.minusDays(offset.toLong())
            val total = history.filter { Instant.ofEpochMilli(it.ended).atZone(ZoneId.systemDefault()).toLocalDate() == day }.sumOf { it.focused } / 60_000
            val row = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(LyraUi.text(activity, day.format(DateTimeFormatter.ofPattern("EEE d")), 12f, LyraUi.muted), LinearLayout.LayoutParams(LyraUi.dp(activity, 70), -2))
            row.addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply { max = focus.goalMinutes; progress = total.toInt().coerceAtMost(max); progressTintList = android.content.res.ColorStateList.valueOf(LyraUi.orange) }, LinearLayout.LayoutParams(0, LyraUi.dp(activity, 24), 1f))
            row.addView(LyraUi.text(activity, "  $total m", 12f), LinearLayout.LayoutParams(LyraUi.dp(activity, 60), -2)); root.addView(row)
        }
        root.addView(LyraUi.section(activity, "Recent sessions", "A record of the time you chose."))
        if (history.isEmpty()) root.addView(LyraUi.text(activity, "Your first session starts the story. No scores, no pressure.", 15f, LyraUi.muted))
        history.take(12).forEach { record -> root.addView(LyraUi.feature(activity, if (record.completed) "✓" else "◷", record.title,
            "${record.focused / 60_000} minutes · ${if (record.completed) "completed" else "ended"} · ${DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(record.ended))}")) }
        root.addView(LyraUi.section(activity, "App use today", "Optional estimates from Android. Kept here.", green = true))
        if (!focus.usageEnabled || !UsageAccess.granted(activity)) {
            root.addView(LyraUi.text(activity, "App-use insights are optional. Android usage access provides app identities and time estimates. M57 processes them locally and does not upload or keep event logs.", 14f, LyraUi.muted))
            root.addView(LyraUi.button(activity, "Review usage access", false) { admin(AdminPage.APP_LIMITS) })
        } else {
            val result = LyraUi.column(activity, 0); result.addView(LyraUi.text(activity, "Reading local Android estimates…", 14f, LyraUi.muted)); root.addView(result)
            val ticket = generation
            worker.execute {
                val snapshot = UsageAccess.today(activity)
                val apps = runCatching { AppCatalog.load(activity).associate { it.packageName to it.label } }.getOrDefault(emptyMap())
                activity.runOnUiThread {
                    if (ticket != generation || activity.isDestroyed) return@runOnUiThread
                    result.removeAllViews()
                    if (!snapshot.available) result.addView(LyraUi.text(activity, snapshot.problem ?: "Usage unavailable", 14f, LyraUi.muted))
                    else {
                        val rows = snapshot.totals.entries.filter { it.value >= 60_000 && it.key != activity.packageName }.sortedByDescending { it.value }.take(8)
                        if (rows.isEmpty()) result.addView(LyraUi.text(activity, "No app-time estimate above one minute today.", 14f, LyraUi.muted))
                        rows.forEach { (pkg, time) -> result.addView(LyraUi.feature(activity, "◷", apps[pkg] ?: pkg, "${time / 60_000} minutes estimated")) }
                    }
                }
            }
        }
        root.addView(LyraUi.text(activity, "Foreground estimates can differ from Android's Digital Wellbeing, especially in split screen. Sessions are credited to the day they end. M57 retains the most recent 500 sessions.", 12f, LyraUi.muted))
        root.addView(LyraUi.button(activity, "Export focus history", false) {
            runCatching { activity.startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE, "m57-focus-history.json"), EXPORT_REQUEST) }.onFailure { toast("This phone has no document picker") }
        })
        root.addView(LyraUi.link(activity, "Delete focus history") {
            confirm("Delete saved focus history?", "This removes completed and early-ended session records from M57. A running session, settings, and files you already exported remain.") { focus.clearHistory(); show("Insights") }
        })
    }
    private fun settings(root: LinearLayout) {
        title(root, "Make it yours.", if (store.familyManaged) "Personal tools are yours. Protection settings belong to the administrator." else "Every control is included. No account or subscription required.")
        Illustrations.addTo(root, "privacy-drawer", 170)
        card(root, "◎", if (store.familyManaged) "Parent controls" else "Protection settings", "Short videos, browsing, optional Lyra, device access and pauses.") { admin(AdminPage.OVERVIEW) }
        root.addView(Switch(activity).apply {
            text = "Reduce motion"; minHeight = LyraUi.dp(activity, 56); setTextColor(LyraUi.ink)
            val prefs = activity.getSharedPreferences("m57_experience", Activity.MODE_PRIVATE)
            isChecked = prefs.getBoolean("reduce_motion", false)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("reduce_motion", checked).apply(); show("Settings") }
        })
        card(root, "◉", "See what M57 does", "Interactive introductions to focus, scrolling and browsing.") { preview() }
        card(root, "◇", "Privacy on this phone", "Know what is stored, what permissions grant, and how to delete history.") {
            dialog = AlertDialog.Builder(activity).setTitle("Your data, here")
                .setMessage("M57 keeps settings, password hashes in family mode, routines, limits and up to 500 focus-session records in private app storage. Android backups are disabled.\n\nOptional usage access is processed locally; raw app-use events, messages, browser history and DNS queries are not stored by M57. Focus history exports are files you control.\n\nFamily-list updates contact the selected hosts sources. Optional Lyra redirection opens your configured HTTPS destination with a source-family hint and new-chat request.\n\nDelete focus history in Insights. Revoke optional permissions in Android Settings; administrator-managed devices may first need their administrator to release policies.")
                .setPositiveButton("Close", null).show().also(LyraUi::styleDialog)
        }
        root.addView(LyraUi.button(activity, "About M57", false) { UiLinks.about(activity) })
        root.addView(LyraUi.button(activity, "Source, build & licenses", false, licenses))
        root.addView(LyraUi.button(activity, "Report a problem", false) {
            if (!store.familyManaged) store.allowAppInfoAccess()
            runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lyraedu/m57/issues"))) }.onFailure { toast("Install a browser to open the issue tracker") }
        })
        root.addView(LyraUi.text(activity, "M57 ${BuildConfig.VERSION_NAME} · Android 14+\nOriginal code: AGPL-3.0-only. Inherited components retain their notices.", 12f, LyraUi.muted))
    }
    private fun preview(index: Int = -1) {
        if (index < 0) {
            dialog = AlertDialog.Builder(activity).setTitle("A little guide to M57")
                .setItems(featureLessons.map { it.title }.toTypedArray()) { _, selected -> preview(selected) }
                .setNegativeButton("Close", null).show().also(LyraUi::styleDialog)
            return
        }
        val lesson = featureLessons[index]
        val body = LyraUi.column(activity, 20)
        Illustrations.addTo(body, lesson.artwork, 200)
        body.addView(LyraUi.badge(activity, "${index + 1} OF ${featureLessons.size} · HOW M57 HELPS"))
        body.addView(LyraUi.text(activity, lesson.explanation, 17f))
        body.addView(LyraUi.divider(activity))
        body.addView(LyraUi.text(activity, lesson.access, 14f, LyraUi.muted))
        val scroll = android.widget.ScrollView(activity).apply { addView(body) }
        dialog = AlertDialog.Builder(activity).setTitle(lesson.title).setView(scroll).setNegativeButton("All guides") { _, _ -> preview() }
            .setNeutralButton("Open feature") { _, _ -> lesson.destination?.let(admin) ?: show(lesson.personalTab) }
            .setPositiveButton(if (index < featureLessons.lastIndex) "Next" else "Got it") { _, _ -> if (index < featureLessons.lastIndex) preview(index + 1) }.show().also(LyraUi::styleDialog)
    }
    fun exportTo(uri: Uri) {
        val data = focus.export()
        worker.execute {
            val result = runCatching { activity.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(data) } ?: error("No output stream") }
            activity.runOnUiThread { if (!activity.isDestroyed) toast(if (result.isSuccess) "Focus history exported" else "Export could not be written") }
        }
    }
    private fun field(root: LinearLayout, hint: String, value: String, number: Boolean): EditText {
        root.addView(LyraUi.text(activity, hint, 13f, LyraUi.muted, true))
        return LyraUi.field(EditText(activity)).apply { this.hint = hint; setText(value); setSingleLine(true); inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; filters = arrayOf(android.text.InputFilter.LengthFilter(80)); root.addView(this) }
    }
    private fun confirm(title: String, message: String, action: () -> Unit) {
        dialog = AlertDialog.Builder(activity).setTitle(title).setMessage(message).setNegativeButton("Cancel", null).setPositiveButton("Continue") { _, _ -> action() }.show().also(LyraUi::styleDialog)
    }
    private fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    companion object {
        const val EXPORT_REQUEST = 5709
        fun formatTime(ms: Long): String {
            val seconds = ((ms + 999) / 1000).coerceAtLeast(0)
            return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%02d:%02d".format(seconds / 60, seconds % 60)
        }
    }
}
