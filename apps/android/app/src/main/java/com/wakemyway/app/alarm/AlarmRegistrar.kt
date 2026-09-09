package com.wakemyway.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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

        // M1 debug builds initially keyed PendingIntents by a 32-bit hash. Remove that
        // pre-release identity before registering the collision-free URI form so upgrades
        // cannot leave two OS alarms for the same logical occurrence.
        cancelLegacy(occurrence.id)

        val triggerAtMillis = occurrence.scheduledAt.toInstant().toEpochMilli()
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setData(intentIdentity("show", occurrence.id))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            operationFor(occurrence.id),
        )
    }

    fun cancel(occurrenceId: WakeOccurrenceId) {
        alarmManager.cancel(operationFor(occurrenceId))
        cancelLegacy(occurrenceId)
    }

    private fun operationFor(occurrenceId: WakeOccurrenceId): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE_WAKE)
            .setData(intentIdentity("fire", occurrenceId))
            .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelLegacy(occurrenceId: WakeOccurrenceId) {
        val legacy = PendingIntent.getBroadcast(
            context,
            occurrenceId.value.hashCode() and Int.MAX_VALUE,
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_FIRE_WAKE)
                .putExtra(EXTRA_OCCURRENCE_ID, occurrenceId.value),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        legacy?.let(alarmManager::cancel)
    }

    private fun intentIdentity(kind: String, occurrenceId: WakeOccurrenceId): Uri =
        Uri.Builder()
            .scheme("wakemyway")
            .authority("alarm")
            .appendPath(kind)
            .appendPath(occurrenceId.value)
            .build()

    companion object {
        const val ACTION_FIRE_WAKE = "com.wakemyway.action.FIRE_WAKE"
        const val EXTRA_OCCURRENCE_ID = "occurrence_id"
    }
}
