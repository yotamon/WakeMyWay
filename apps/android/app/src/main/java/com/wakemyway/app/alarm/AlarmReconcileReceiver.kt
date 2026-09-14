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
        var after = kernel.reconcile(afterBoot = afterBoot)

        // A future occurrence still needs exact-alarm capability plus a controllable presentation
        // path. An already-active occurrence only needs the presentation path: losing permission to
        // schedule another exact alarm must not silence a wake that the user can still Stop safely.
        val unsafe = when {
            after.activeOccurrence != null ->
                after.activeWakeRepairTarget() != AlarmRepairTarget.NONE
            after.nextOccurrence != null ->
                after.repairTarget() != AlarmRepairTarget.NONE
            else -> false
        }
        if (unsafe) {
            val hadActiveExecution = after.activeOccurrence != null
            kernel.cancelSchedule()
            if (hadActiveExecution) {
                // cancelSchedule() invalidates durable authority first; stopping the component then
                // releases any currently playing MediaPlayer/ToneGenerator immediately.
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
