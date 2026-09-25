package com.wakemyway.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakemyway.app.widget.WakeWidgetUpdater

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

        // Missing Android capability must fail closed at the registration/execution boundary, not
        // by erasing durable alarm intent. Future occurrences keep their critical plan but lose their
        // OS registration, so Home can still show the alarm and a later reconciliation can repair it.
        // An already-active wake remains a stricter case: unsafe presentation terminates execution.
        if (
            (after.enabledScheduleCount > 0 || after.activeOccurrence != null) &&
            after.repairTarget() != AlarmRepairTarget.NONE
        ) {
            if (after.activeOccurrence != null) {
                kernel.cancelSchedule()
                context.stopService(Intent(context, AlarmPlaybackService::class.java))
            } else {
                kernel.suspendFutureRegistrations()
            }
            after = kernel.health()
        }

        WakeWidgetUpdater.request(context)

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
