// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.animation.ValueAnimator
import android.content.Context
import android.os.PowerManager
import android.view.View
import android.view.accessibility.AccessibilityManager

object Motion {
    fun press(view: View) {
        if (!enabled(view.context)) return
        fun scale(value: Float) = android.animation.AnimatorSet().apply {
            playTogether(android.animation.ObjectAnimator.ofFloat(view, View.SCALE_X, value), android.animation.ObjectAnimator.ofFloat(view, View.SCALE_Y, value)); duration = 120
        }
        view.stateListAnimator = android.animation.StateListAnimator().apply {
            addState(intArrayOf(android.R.attr.state_pressed), scale(0.97f)); addState(intArrayOf(), scale(1f))
        }
    }
    fun enabled(context: Context) = ValueAnimator.areAnimatorsEnabled() &&
        !context.getSharedPreferences("m57_experience", Context.MODE_PRIVATE).getBoolean("reduce_motion", false) &&
        !context.getSystemService(PowerManager::class.java).isPowerSaveMode &&
        !context.getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled
    fun enter(view: View) {
        if (!enabled(view.context)) return
        view.alpha = 0f; view.translationY = LyraUi.dp(view.context, 12).toFloat()
        view.animate().alpha(1f).translationY(0f).setDuration(240).start()
    }
    fun reveal(view: View) {
        view.visibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        if (view.visibility == View.VISIBLE) enter(view)
    }
}
