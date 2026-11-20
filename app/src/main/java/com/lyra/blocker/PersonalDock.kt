// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker
import android.content.Context
import android.widget.FrameLayout

class PersonalDock @JvmOverloads constructor(context: Context, selected: String = "Focus", choose: (String) -> Unit = {}) : FrameLayout(context) {
    init {
        addView(NavigationDock(context, listOf(DockDestination("Focus", "Focus", M57Icon.FOCUS), DockDestination("Plan", "Plan", M57Icon.PLAN),
            DockDestination("Insights", "Insights", M57Icon.INSIGHTS), DockDestination("Settings", "Settings", M57Icon.SETTINGS)), selected, choose))
    }
}
