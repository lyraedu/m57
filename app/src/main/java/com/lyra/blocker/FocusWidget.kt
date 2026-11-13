// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A private, action-only widget. Opening the app never starts or pauses protection implicitly. */
class FocusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { render(context, manager, ids) }
    companion object {
        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            render(context, manager, manager.getAppWidgetIds(ComponentName(context, FocusWidget::class.java)))
        }
        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val store = FocusStore(context)
            val session = store.session
            val state = session?.let(store::state)
            val views = RemoteViews(context.packageName, R.layout.focus_widget)
            views.setTextViewText(R.id.widget_title, if (state != null && !state.finished()) "Return to your focus" else "Make a little room.")
            views.setTextViewText(R.id.widget_detail, "${store.todayFocused / 60_000} / ${store.goalMinutes} focus minutes today")
            val intent = PendingIntent.getActivity(context, 57, Intent(context, MainActivity::class.java).putExtra("m57.open_focus", true), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widget_root, intent)
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
