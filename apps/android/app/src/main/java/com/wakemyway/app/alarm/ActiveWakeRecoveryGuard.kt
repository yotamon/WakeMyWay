package com.wakemyway.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.wakemyway.core.schedule.WakeOccurrenceId

/**
 * OS-owned fallback for an already-active wake.
 *
 * The live playback service continually pushes this one-shot guard into the future. While the
 * process is healthy the alarm never fires. If the process dies, the last armed guard survives in
 * AlarmManager and gets one chance to recreate the foreground playback path without waiting for
 * Android's service-restart backoff.
 */
internal object ActiveWakeRecoveryGuard {
    private const val RECOVERY_DEADLINE_MILLIS = 10_000L

    fun arm(context: Context, occurrenceId: WakeOccurrenceId) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + RECOVERY_DEADLINE_MILLIS,
                pendingIntent(context, occurrenceId),
            )
        }
    }

    fun cancel(context: Context, occurrenceId: WakeOccurrenceId) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, occurrenceId))
    }

    private fun pendingIntent(
        context: Context,
        occurrenceId: WakeOccurrenceId,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ActiveWakeRecoveryReceiver::class.java)
            .setAction(ACTION_RECOVER_ACTIVE_WAKE)
            .setData(
                Uri.Builder()
                    .scheme("wakemyway")
                    .authority("active-wake-recovery")
                    .appendPath(occurrenceId.value)
                    .build(),
            )
            .putExtra(AlarmPlaybackService.EXTRA_OCCURRENCE_ID, occurrenceId.value),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    const val ACTION_RECOVER_ACTIVE_WAKE = "com.wakemyway.action.RECOVER_ACTIVE_WAKE"
}

class ActiveWakeRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ActiveWakeRecoveryGuard.ACTION_RECOVER_ACTIVE_WAKE) return
        val rawId = intent.getStringExtra(AlarmPlaybackService.EXTRA_OCCURRENCE_ID) ?: return
        val occurrenceId = WakeOccurrenceId(rawId)
        val kernel = AlarmKernel(context)

        if (kernel.activeOccurrence()?.id != occurrenceId) {
            ActiveWakeRecoveryGuard.cancel(context, occurrenceId)
            return
        }

        if (kernel.health().activeWakeRepairTarget() != AlarmRepairTarget.NONE) {
            ActiveWakeRecoveryGuard.cancel(context, occurrenceId)
            kernel.cancelSchedule()
            context.stopService(Intent(context, AlarmPlaybackService::class.java))
            return
        }

        // The receiver itself is best-effort. If Android temporarily rejects the foreground-service
        // start, re-arm once rather than crashing the alarm process. A healthy service will take
        // over refreshing the guard as soon as recovery succeeds.
        runCatching {
            AlarmPlaybackService.recover(context, occurrenceId)
        }.onFailure {
            ActiveWakeRecoveryGuard.arm(context, occurrenceId)
        }
    }
}
