// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

data class FocusRecord(val id: String, val title: String, val ended: Long, val focused: Long, val completed: Boolean)
data class FocusRoutine(val id: String, val title: String, val days: Set<Int>, val start: Int, val end: Int, val enabled: Boolean)
data class FocusSession(val id: String, val title: String, val kind: String, val wall: Long, val elapsed: Long,
    val boot: Int, val focus: Long, val rest: Long, val rounds: Int, val strict: Boolean)
data class FocusDraft(val kind: String = "timer", val title: String = "", val minutes: String = "25",
    val rest: String = "5", val rounds: String = "4", val committed: Boolean = false)

/** Private, bounded local data. Session completion and history insertion are one atomic write. */
class FocusStore(context: Context) {
    companion object {
        @Volatile var storageHealthy = true
            private set
    }
    private val context = context.applicationContext
    val prefs = context.getSharedPreferences("m57_focus", Context.MODE_PRIVATE)
    private fun boot() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    var blockedApps: Set<String>
        get() = prefs.getStringSet("apps", emptySet())!!.toSet()
        set(value) { require(value.size <= 500 && value.all(BlockPolicy::validPackage)); prefs.edit().putStringSet("apps", value).apply() }
    var blockedSites: Set<String>
        get() = prefs.getStringSet("sites", emptySet())!!.toSet()
        set(value) { require(value.size <= 500 && value.all { com.lyra.blocker.family.HostRules.domain(it) == it }); prefs.edit().putStringSet("sites", value).apply() }
    var allowedSites: Set<String>
        get() = prefs.getStringSet("allowed_sites", emptySet())!!.toSet()
        set(value) { require(value.size <= 500 && value.all { com.lyra.blocker.family.HostRules.domain(it) == it }); prefs.edit().putStringSet("allowed_sites", value).apply() }
    var studyBrowser: Boolean
        get() = prefs.getBoolean("study_browser", false)
        set(value) { prefs.edit().putBoolean("study_browser", value).apply() }
    var goalMinutes: Int
        get() = prefs.getInt("goal", 60).coerceIn(5, 480)
        set(value) { require(value in 5..480); prefs.edit().putInt("goal", value).apply() }
    var usageEnabled: Boolean
        get() = prefs.getBoolean("usage", false)
        set(value) { prefs.edit().putBoolean("usage", value).apply() }
    var draft: FocusDraft
        get() = runCatching {
            val j = JSONObject(prefs.getString("draft", "{}")!!)
            FocusDraft(j.optString("kind", "timer").takeIf { it in setOf("timer", "pomodoro", "stopwatch") } ?: "timer",
                j.optString("title").take(80), j.optString("minutes", "25").take(3),
                j.optString("rest", "5").take(2), j.optString("rounds", "4").take(2), j.optBoolean("committed"))
        }.getOrDefault(FocusDraft())
        set(value) {
            require(value.kind in setOf("timer", "pomodoro", "stopwatch") && value.title.length <= 80 &&
                value.minutes.length <= 3 && value.rest.length <= 2 && value.rounds.length <= 2)
            prefs.edit().putString("draft", JSONObject().put("kind", value.kind).put("title", value.title)
                .put("minutes", value.minutes).put("rest", value.rest).put("rounds", value.rounds)
                .put("committed", value.committed).toString()).apply()
        }
    val session: FocusSession? get() = runCatching {
        val j = JSONObject(prefs.getString("session", "")!!)
        FocusSession(j.getString("id"), j.getString("title"), j.getString("kind"), j.getLong("wall"),
            j.getLong("elapsed"), j.getInt("boot"), j.getLong("focus"), j.getLong("rest"), j.getInt("rounds"), j.getBoolean("strict"))
            .also { FocusClock.state(it.kind, 0, it.focus, it.rest, it.rounds) }
    }.getOrNull()
    fun state(s: FocusSession): FocusClock.State = FocusClock.state(s.kind,
        FocusClock.elapsed(s.wall, s.elapsed, s.boot, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot()), s.focus, s.rest, s.rounds)
    fun start(title: String, kind: String, minutes: Int, rest: Int, rounds: Int, strict: Boolean) {
        check(session == null) { "A session is already running" }
        val focusMs = minutes * 60_000L
        FocusClock.state(kind, 0, focusMs, rest * 60_000L, rounds)
        val j = JSONObject().put("id", UUID.randomUUID().toString()).put("title", title.trim().take(80).ifEmpty { "Time for what matters" })
            .put("kind", kind).put("wall", System.currentTimeMillis()).put("elapsed", SystemClock.elapsedRealtime())
            .put("boot", boot()).put("focus", focusMs).put("rest", rest * 60_000L).put("rounds", rounds).put("strict", strict)
        storageHealthy = prefs.edit().putString("session", j.toString()).commit()
        check(storageHealthy) { "Could not save session" }
        FocusWidget.update(context)
    }
    fun finish(manual: Boolean = false): Boolean {
        val s = session ?: return false
        val status = state(s)
        if (!status.finished() && (!manual || s.strict)) return false
        val records = JSONArray()
        history.take(499).forEach { records.put(recordJson(it)) }
        records.put(recordJson(FocusRecord(s.id, s.title, System.currentTimeMillis(), status.focused(), status.phase() == "done")))
        storageHealthy = prefs.edit().remove("session").putString("history", records.toString()).commit()
        FocusWidget.update(context)
        return storageHealthy
    }
    val history: List<FocusRecord> get() = runCatching {
        val a = JSONArray(prefs.getString("history", "[]"))
        (0 until a.length().coerceAtMost(500)).map { i -> a.getJSONObject(i).let {
            FocusRecord(it.getString("id"), it.getString("title"), it.getLong("ended"), it.getLong("focused"), it.getBoolean("completed"))
        } }.sortedByDescending { it.ended }
    }.getOrDefault(emptyList())
    private fun recordJson(r: FocusRecord) = JSONObject().put("id", r.id).put("title", r.title).put("ended", r.ended).put("focused", r.focused).put("completed", r.completed)
    val todayFocused: Long get() {
        val today = ZonedDateTime.now().toLocalDate()
        val recorded = history.filter { Instant.ofEpochMilli(it.ended).atZone(ZoneId.systemDefault()).toLocalDate() == today }.sumOf { it.focused }
        return recorded + (session?.let { state(it).focused() } ?: 0)
    }
    val routines: List<FocusRoutine> get() = runCatching {
        val a = JSONArray(prefs.getString("routines", "[]"))
        (0 until a.length().coerceAtMost(30)).map { i -> a.getJSONObject(i).let { j ->
            val d = j.getJSONArray("days")
            FocusRoutine(j.getString("id"), j.getString("title"), (0 until d.length()).map { d.getInt(it) }.toSet(), j.getInt("start"), j.getInt("end"), j.getBoolean("enabled"))
        } }
    }.getOrDefault(emptyList())
    fun saveRoutines(values: List<FocusRoutine>) {
        require(values.size <= 30 && values.all { it.title.length in 1..80 && it.days.isNotEmpty() && it.days.all { day -> day in 1..7 } && it.start in 0..1439 && it.end in 0..1439 && it.start != it.end })
        val a = JSONArray()
        values.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("days", JSONArray(it.days.sorted())).put("start", it.start).put("end", it.end).put("enabled", it.enabled)) }
        prefs.edit().putString("routines", a.toString()).apply()
    }
    fun activeRoutine(): FocusRoutine? = routines.firstOrNull { it.enabled && RoutinePolicy.active(it.days, it.start, it.end, ZonedDateTime.now()) }
    fun blockingReason(): String? = session?.let { val s = state(it); if (!s.finished() && s.phase() == "focus") it.title else null } ?: activeRoutine()?.title
    fun clearHistory() { prefs.edit().remove("history").apply(); FocusWidget.update(context) }
    fun export(): String = JSONObject().put("format", "m57-local-history-v1").put("history", JSONArray().apply { history.forEach { put(recordJson(it)) } }).toString(2)
}
