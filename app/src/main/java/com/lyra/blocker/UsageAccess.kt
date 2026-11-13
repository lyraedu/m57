// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

data class UsageSnapshot(val available: Boolean, val totals: Map<String, Long>, val sampledAt: Long, val problem: String? = null)

object UsageAccess {
    fun granted(context: Context): Boolean = context.getSystemService(AppOpsManager::class.java)
        .checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED

    /** Call on a worker. Android owns event retention; do not persist app-use events. */
    fun today(context: Context): UsageSnapshot {
        val now = System.currentTimeMillis()
        if (!FocusStore(context).usageEnabled || !granted(context)) return UsageSnapshot(false, emptyMap(), now, "Usage access is off")
        return runCatching {
            val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(start - 86_400_000, now)
                ?: return UsageSnapshot(false, emptyMap(), now, "Unlock this phone to read usage")
            val timeline = UsageTimeline(start, now)
            val event = UsageEvents.Event()
            var count = 0
            while (events.hasNextEvent()) {
                if (++count > 200_000) return UsageSnapshot(false, emptyMap(), now, "Android returned too many events; usage is incomplete")
                events.getNextEvent(event)
                timeline.event(event.timeStamp, event.packageName, event.eventType)
            }
            UsageSnapshot(true, timeline.finish(), now)
        }.getOrElse { UsageSnapshot(false, emptyMap(), now, "Android could not provide usage") }
    }
}

class AppLimitStore(context: Context) {
    val prefs = context.getSharedPreferences("m57_limits", Context.MODE_PRIVATE)
    val limits: Map<String, Int> get() = prefs.all.mapNotNull { (pkg, value) ->
        (value as? Int)?.takeIf { it in 1..1440 && BlockPolicy.validPackage(pkg) }?.let { pkg to it }
    }.toMap()
    fun set(pkg: String, minutes: Int) {
        require(BlockPolicy.validPackage(pkg) && minutes in 0..1440)
        if (minutes == 0) prefs.edit().remove(pkg).apply() else prefs.edit().putInt(pkg, minutes).apply()
    }
}
