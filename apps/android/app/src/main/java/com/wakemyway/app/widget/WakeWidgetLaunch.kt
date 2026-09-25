package com.wakemyway.app.widget

import android.content.Context
import android.content.Intent
import com.wakemyway.app.MainActivity

enum class WakeWidgetDestination {
    HOME,
    ALARMS,
    ALARM_EDITOR,
    TOMORROW_PLAN,
    INSIGHTS,
}

data class WakeWidgetLaunchRequest(
    val destination: WakeWidgetDestination,
    val alarmId: String? = null,
    val repairWake: Boolean = false,
)

object WakeWidgetLaunch {
    private const val ACTION_WIDGET_OPEN = "com.wakemyway.app.action.WIDGET_OPEN"
    private const val EXTRA_DESTINATION = "widget_destination"
    private const val EXTRA_ALARM_ID = "widget_alarm_id"
    private const val EXTRA_REPAIR_WAKE = "widget_repair_wake"

    fun intent(
        context: Context,
        destination: WakeWidgetDestination,
        alarmId: String? = null,
        repairWake: Boolean = false,
    ): Intent = Intent(context, MainActivity::class.java)
        .setAction(ACTION_WIDGET_OPEN)
        .putExtra(EXTRA_DESTINATION, destination.name)
        .putExtra(EXTRA_ALARM_ID, alarmId)
        .putExtra(EXTRA_REPAIR_WAKE, repairWake)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun from(intent: Intent?): WakeWidgetLaunchRequest? {
        if (intent?.action != ACTION_WIDGET_OPEN) return null
        val destination = intent.getStringExtra(EXTRA_DESTINATION)
            ?.let { raw -> WakeWidgetDestination.entries.firstOrNull { it.name == raw } }
            ?: WakeWidgetDestination.HOME
        return WakeWidgetLaunchRequest(
            destination = destination,
            alarmId = intent.getStringExtra(EXTRA_ALARM_ID)?.takeIf(String::isNotBlank),
            repairWake = intent.getBooleanExtra(EXTRA_REPAIR_WAKE, false),
        )
    }
}
