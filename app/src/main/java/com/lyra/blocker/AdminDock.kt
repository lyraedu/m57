// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker
import android.content.Context
import android.widget.FrameLayout

class AdminDock @JvmOverloads constructor(context: Context, selected: AdminTab = AdminTab.CONTROLS, choose: (AdminTab) -> Unit = {}) : FrameLayout(context) {
    init {
        addView(NavigationDock(context, AdminTab.values().map { DockDestination(it.name, it.title,
            when (it) { AdminTab.CONTROLS -> M57Icon.SHIELD; AdminTab.SETTINGS -> M57Icon.SETTINGS }) }, selected.name) { choose(AdminTab.valueOf(it)) })
    }
}
