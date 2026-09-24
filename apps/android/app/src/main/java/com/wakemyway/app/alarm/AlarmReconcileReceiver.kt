package com.wakemyway.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReconcileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kernel = AlarmKernel(context)
        val before = kernel.health()
        val trackedOccurrence = before.nextOccurrence ?: before.activeOccurrence
        val afterBoot = shouldRunBootRecovery(
            action = intent.action,
            hasActiveOccurrence = before.activeOccurrence != null,
        )
        val recalculateFuture = intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        var after = kernel.reconcile(
            afterBoot = afterBoot,
            recalculateFuture = recalculateFuture,
        )

        // Presentation permissions/special access are global Android capabilities. If they have
        // disappeared, no enabled WakeMyWay alarm is safe to preserve because any one could later
        // produce critical audio without reachable Stop/Snooze controls.
        if (
            (after.enabledScheduleCount > 0 || after.activeOccurrence != null) &&
            after.repairTarget() != AlarmRepairTarget.NONE
        ) {
            val hadActiveExecution = after.activeOccurrence != null
            kernel.cancelSchedule()
            if (hadActiveExecution) {
                context.stopService(Intent(context, AlarmPlaybackService::class.java))
            }
            after = kernel.health()
        }

        trackedOccurrence?.let { occurrence ->
            WakeTimingTrace(context).reconciled(
                occurrenceId = occurrence.id,
                reason = intent.action ?: "UNKNOWN_RECONCILE_ACTION",
                wakeReadyAfter = after.ready,
            )
        }
    }
}

internal fun shouldRunBootRecovery(
    action: String?,
    hasActiveOccurrence: Boolean,
): Boolean = when (action) {
    Intent.ACTION_LOCKED_BOOT_COMPLETED -> true
    // LOCKED_BOOT_COMPLETED is the authoritative boot-recovery boundary for this direct-boot-aware
    // receiver. If BOOT_COMPLETED arrives later while a wake is already active, that execution
    // started after boot and must not be advanced as stale boot state.
    Intent.ACTION_BOOT_COMPLETED -> !hasActiveOccurrence
    else -> false
}
