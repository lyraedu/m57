// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker.family

import android.content.Context

class FamilyStore(context: Context) {
    val prefs = context.applicationContext.getSharedPreferences("family_protection", Context.MODE_PRIVATE)
    var enabled: Set<String>
        get() = prefs.getStringSet("categories", emptySet())!!.filter { FamilyCategory.byKey(it) != null }.toSet()
        set(value) { require(value.all { FamilyCategory.byKey(it) != null }); prefs.edit().putStringSet("categories", value.toSet()).apply() }
    var browserEnabled: Boolean
        get() = prefs.getBoolean("browser_enabled", false)
        set(value) { prefs.edit().putBoolean("browser_enabled", value).apply() }
    var vpnRequested: Boolean
        get() = prefs.getBoolean("vpn_requested", false)
        set(value) { prefs.edit().putBoolean("vpn_requested", value).apply() }
    var resolverMode: String
        get() = prefs.getString("resolver_mode", ResolverPolicy.NETWORK).orEmpty()
        set(value) { require(value in ResolverPolicy.MODES); prefs.edit().putString("resolver_mode", value).apply() }
    var blockVpnApps: Boolean
        get() = prefs.getBoolean("block_vpn_apps", false)
        set(value) { prefs.edit().putBoolean("block_vpn_apps", value).apply() }
    val vpnConfigured: Boolean get() = resolverMode != ResolverPolicy.NETWORK || enabled.isNotEmpty() || block.isNotEmpty()
    var allow: Set<String>
        get() = prefs.getStringSet("allow_domains", emptySet())!!.toSet()
        set(value) { prefs.edit().putStringSet("allow_domains", value.toSet()).apply() }
    var block: Set<String>
        get() = prefs.getStringSet("block_domains", emptySet())!!.toSet()
        set(value) { prefs.edit().putStringSet("block_domains", value.toSet()).apply() }
    var blockedUrls: Set<String>
        get() = prefs.getStringSet("blocked_urls", emptySet())!!.toSet()
        set(value) { require(value.size <= 500 && value.all(BrowserUrlPolicy::validRule)); prefs.edit().putStringSet("blocked_urls", value.toSet()).apply() }
    fun lastSync(category: FamilyCategory) = prefs.getLong("sync_${category.key}", 0)
    fun count(category: FamilyCategory) = prefs.getInt("count_${category.key}", 0)
    fun error(category: FamilyCategory) = prefs.getString("error_${category.key}", "").orEmpty()
    fun source(category: FamilyCategory) = prefs.getString("source_${category.key}", "").orEmpty()
    fun vpnMessage() = prefs.getString("vpn_message", "Not started").orEmpty()
    fun vpnMessage(value: String) { prefs.edit().putString("vpn_message", value).apply() }
}
