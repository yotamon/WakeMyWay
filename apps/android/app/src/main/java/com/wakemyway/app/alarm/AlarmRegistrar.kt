package com.wakemyway.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wakemyway.app.MainActivity
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId

class AlarmRegistrar(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun register(occurrence: WakeOccurrence) {
        check(canScheduleExactAlarms()) { "Exact alarm capability is unavailable" }
        val triggerAtMillis = occurrence.scheduledAt.toInstant().toEpochMilli()
        val showIntent = PendingIntent.getActivity(
            context,
            SHOW_ALARM_REQUEST_CODE,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            operationFor(occurrence.id),
        )
    }

    fun cancel(occurrenceId: WakeOccurrenceId) {
        alarmManager.cancel(operationFor(occurrenceId))
    }

    private fun operationFor(occurrenceId: WakeOccurrenceId): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(occurrenceId),
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE_WAKE)
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun requestCode(occurrenceId: WakeOccurrenceId): Int =
        occurrenceId.value.hashCode() and Int.MAX_VALUE

    companion object {
        const val ACTION_FIRE_WAKE = "com.wakemyway.action.FIRE_WAKE"
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
        private const val SHOW_ALARM_REQUEST_CODE = 7001
    }
}
