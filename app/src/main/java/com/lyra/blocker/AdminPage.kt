// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

enum class AdminTab(val title: String) { OVERVIEW("Overview"), CONTROLS("Controls"), SETTINGS("Settings") }

enum class AdminPage(val title: String, val tab: AdminTab) {
    OVERVIEW("Overview", AdminTab.OVERVIEW),
    CONTROLS("Controls", AdminTab.CONTROLS),
    SETTINGS("Settings", AdminTab.SETTINGS),
    FOCUS_RULES("Focus apps & sites", AdminTab.CONTROLS),
    APP_LIMITS("Daily app limits", AdminTab.CONTROLS),
    ROUTINES("Routines", AdminTab.CONTROLS),
    SETUP("Set up M57", AdminTab.SETTINGS),
    AI("AI redirection", AdminTab.CONTROLS),
    APP_RULES("App & website rules", AdminTab.CONTROLS),
    META("Meta AI", AdminTab.CONTROLS),
    SHORT_VIDEOS("Short videos", AdminTab.CONTROLS),
    FAMILY("Family browsing", AdminTab.CONTROLS),
    FAMILY_CATEGORY("Family category", AdminTab.CONTROLS),
    FAMILY_EXCEPTIONS("Domain exceptions", AdminTab.CONTROLS),
    FAMILY_SOURCES("List sources", AdminTab.CONTROLS),
    DEVICE_ACCESS("Device access", AdminTab.SETTINGS),
    PAUSE("Protection schedule", AdminTab.OVERVIEW),
    CUSTOM_PAUSE("Custom end time", AdminTab.OVERVIEW);

    val parent: AdminPage? get() = when (this) {
        AI, SHORT_VIDEOS, FAMILY, FOCUS_RULES, APP_LIMITS, ROUTINES -> CONTROLS
        APP_RULES, META -> AI
        FAMILY_CATEGORY, FAMILY_EXCEPTIONS, FAMILY_SOURCES -> FAMILY
        DEVICE_ACCESS -> SETTINGS
        SETUP -> OVERVIEW
        PAUSE -> OVERVIEW
        CUSTOM_PAUSE -> PAUSE
        else -> null
    }
}

data class ControlCategory(val page: AdminPage, val icon: String, val description: String, val keywords: String) {
    fun matches(query: String): Boolean {
        val haystack = "${page.title} $description $keywords".lowercase(java.util.Locale.ROOT)
        return query.lowercase(java.util.Locale.ROOT).trim().split(Regex("\\s+"))
            .all { it.isBlank() || haystack.contains(it) }
    }
}

val controlCategories = listOf(
    ControlCategory(AdminPage.FOCUS_RULES, "◎", "Chosen apps, websites and study browsing during focus", "timer study allowlist personal focus"),
    ControlCategory(AdminPage.APP_LIMITS, "◷", "Daily app allowances with local usage access", "time daily limits usage statistics"),
    ControlCategory(AdminPage.ROUTINES, "▦", "Repeat your focus list on selected days and times", "routine schedule weekly overnight"),
    ControlCategory(AdminPage.AI, "↗", "Lyra destination, countdown, PWA and app rules", "chatgpt gemini claude models artificial intelligence redirect"),
    ControlCategory(AdminPage.SHORT_VIDEOS, "Ⅱ", "YouTube Shorts and supported full-screen Reels controls", "instagram snapchat josh doomscroll video"),
    ControlCategory(AdminPage.FAMILY, "♡", "Website categories, local DNS VPN and browser protection", "adult content malware stevenblack hosts lists sync"),
    ControlCategory(AdminPage.DEVICE_ACCESS, "⌘", "Accessibility, App info and removal protection", "settings permissions uninstall device administrator"),
)
