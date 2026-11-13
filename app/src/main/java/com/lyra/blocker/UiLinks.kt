// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object UiLinks {
    const val ABOUT = "https://lyra-dev.vercel.app/m57"
    fun about(activity: Activity) {
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ABOUT))) }
            .onFailure { Toast.makeText(activity, "Open $ABOUT in a browser", Toast.LENGTH_LONG).show() }
    }
}
