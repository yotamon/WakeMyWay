package com.wakemyway.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Lightweight event-driven refresh bridge.
 *
 * The launcher owns Glance lifecycle execution. Product mutation paths only request a standard
 * AppWidget update broadcast; they never retain widget state or start a long-running worker.
 */
object WakeWidgetUpdater {
    fun request(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, WakeMyWayWidgetReceiver::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        appContext.sendBroadcast(
            Intent(appContext, WakeMyWayWidgetReceiver::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
        )
    }
}
