// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService

object VpnApps {
    /** Launcher visibility plus the VPN service query. Hidden/nonstandard VPN clients may remain undiscoverable. */
    fun discover(context: Context): Map<String, String> {
        val manager = context.packageManager
        val packages = manager.queryIntentServices(Intent(VpnService.SERVICE_INTERFACE), 0)
            .map { it.serviceInfo.packageName }.toMutableSet()
        packages += manager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
        return packages.take(1000).mapNotNull { pkg ->
            runCatching {
                val info = manager.getPackageInfo(pkg, PackageManager.GET_SERVICES)
                if (info.services.orEmpty().none { VpnAppPolicy.vpnService(context.packageName, pkg, it.permission) }) null
                else pkg to manager.getApplicationLabel(manager.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull()
        }.toMap()
    }
}
