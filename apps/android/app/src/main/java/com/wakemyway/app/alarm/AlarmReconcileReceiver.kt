package com.wakemyway.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReconcileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kernel = AlarmKernel(context)
        val before = kernel.health()
        val trackedOccurrence = before.nextOccurrence ?: before.activeOccurrence
        val afterBoot = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        val after = kernel.reconcile(afterBoot = afterBoot)

        trackedOccurrence?.let { occurrence ->
            WakeTimingTrace(context).reconciled(
                occurrenceId = occurrence.id,
                reason = intent.action ?: "UNKNOWN_RECONCILE_ACTION",
                wakeReadyAfter = after.ready,
            )
        }
    }
}
