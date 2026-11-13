// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.content.Intent

data class InstalledApp(val packageName: String, val label: String)

object AppCatalog {
    fun protectedPackages(context: Context): Set<String> {
        val manager = context.packageManager
        val homes = manager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
        val dialer = context.getSystemService(android.telecom.TelecomManager::class.java)?.defaultDialerPackage
        return (homes + listOfNotNull(context.packageName, "com.android.settings", "com.android.systemui", "com.android.permissioncontroller",
            "com.google.android.permissioncontroller", dialer, ProtectionStore(context).pwaPackage.takeIf { it.isNotEmpty() })).toSet()
    }
    /** Launcher-visible apps only. Excludes system navigation, settings, M57 and its current destination. */
    fun load(context: Context): List<InstalledApp> {
        val manager = context.packageManager
        val blocked = protectedPackages(context)
        return manager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(manager).toString()) }
            .distinctBy { it.packageName }.filter { it.packageName !in blocked }.sortedBy { it.label.lowercase() }
    }
}
