// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInfoDeviceBoundaryTest {
    private fun node(id: String, text: String) = BlockPolicy.Label("com.android.settings:id/$id", text)
    private val controls = listOf(node("button1", "Open"), node("button2", "Uninstall"), node("button3", "Force stop"))

    @Test fun matchesSamsungAppDetailsFromObservedHeaderAndActionRow() {
        val detail = listOf(node("entity_header_title", "M57 Preview")) + controls
        assertTrue(AppInfoPolicy.isOwnAppInfo("com.android.settings", detail, "M57 Preview", "com.lyra.blocker.preview"))
    }

    @Test fun preservesOtherAppsListsAndUntrustedWindows() {
        val other = listOf(node("entity_header_title", "Another app")) + controls
        assertFalse(AppInfoPolicy.isOwnAppInfo("com.android.settings", other, "M57 Preview", "com.lyra.blocker.preview"))
        val list = listOf(node("title", "M57 Preview")) + controls
        assertFalse(AppInfoPolicy.isOwnAppInfo("com.android.settings", list, "M57 Preview", "com.lyra.blocker.preview"))
        val header = listOf(node("entity_header_title", "M57 Preview"))
        assertFalse(AppInfoPolicy.isOwnAppInfo("com.android.settings", header, "M57 Preview", "com.lyra.blocker.preview"))
        assertFalse(AppInfoPolicy.isOwnAppInfo("example.other", header + controls, "M57 Preview", "com.lyra.blocker.preview"))
    }
}
