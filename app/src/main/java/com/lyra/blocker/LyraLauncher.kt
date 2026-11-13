// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager

object LyraLauncher {
    data class InstalledPwa(val packageName: String, val label: String, val destination: String)

    fun installedPwas(context: Context, destination: String): List<InstalledPwa> {
        if (!BlockPolicy.validDestination(destination)) return emptyList()
        val pm = context.packageManager
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(destination)).addCategory(Intent.CATEGORY_BROWSABLE)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return (pm.queryIntentActivities(view, 0) + pm.queryIntentActivities(launcher, 0))
            .filter { it.activityInfo.packageName.startsWith("org.chromium.webapk.") }
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { result ->
                runCatching {
                    val metadata = pm.getApplicationInfo(result.activityInfo.packageName, PackageManager.GET_META_DATA).metaData
                    val target = HandoffPolicy.pwaDestination(destination,
                        metadata?.getString("org.chromium.webapk.shell_apk.startUrl").orEmpty(),
                        metadata?.getString("org.chromium.webapk.shell_apk.scope")) ?: return@runCatching null
                    InstalledPwa(result.activityInfo.packageName, result.loadLabel(pm).toString(), target)
                }.getOrNull()
            }.sortedWith(compareBy<InstalledPwa> { if (Uri.parse(it.destination).host.equals(Uri.parse(destination).host, true)) 0 else 1 }
                .thenBy { it.packageName })
    }

    fun openInChrome(context: Context, destination: String): Boolean {
        if (!BlockPolicy.validDestination(destination)) return false
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destination)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    }
    /** Resolve the installed WebAPK by its URL filter; never infer its generated package ID. */
    fun open(context: Context, store: ProtectionStore, source: String? = null): Boolean {
        if (!BlockPolicy.validDestination(store.destination)) return false
        val destination = Uri.parse(store.destination).buildUpon().apply {
            if (source != null) {
                clearQuery()
                val configured = Uri.parse(store.destination)
                for (key in configured.queryParameterNames.filter { it !in setOf("m57_source", "m57_intent") })
                    for (value in configured.getQueryParameters(key)) appendQueryParameter(key, value)
                appendQueryParameter("m57_source", source)
                appendQueryParameter("m57_intent", "new")
            }
        }.build()
        val base = Intent(Intent.ACTION_VIEW, destination).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pwas = installedPwas(context, store.destination)
        val ordered = pwas.sortedBy { if (it.packageName == store.pwaPackage) 0 else 1 }
        for (pwa in ordered) {
            val target = destination.buildUpon().scheme("https").authority(Uri.parse(pwa.destination).authority).build()
            try {
                context.startActivity(Intent(base).setData(target).setPackage(pwa.packageName))
                return true
            } catch (_: ActivityNotFoundException) {
                // Launcher-only WebAPKs still receive the scoped target as intent data.
                val launch = context.packageManager.getLaunchIntentForPackage(pwa.packageName)
                if (launch != null) try {
                    context.startActivity(launch.apply { data = target; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    return true
                } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
            } catch (_: SecurityException) { }
        }
        try {
            context.startActivity(Intent(base).setPackage("com.android.chrome"))
            return true
        } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
        // A device without Chrome can still have another browser.
        return try { context.startActivity(base); true }
        catch (_: ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    }
}
