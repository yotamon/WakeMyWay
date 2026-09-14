package com.wakemyway.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakemyway.core.schedule.WakeOccurrenceId

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmRegistrar.ACTION_FIRE_WAKE) return
        val rawId = intent.getStringExtra(AlarmRegistrar.EXTRA_OCCURRENCE_ID) ?: return
        val occurrenceId = WakeOccurrenceId(rawId)
        val kernel = AlarmKernel(context)

        // Once Android has delivered this occurrence, only execution-safety capabilities matter.
        // Exact-alarm access controls future scheduling and Snooze, not whether an already-delivered
        // wake with reachable Stop/Snooze controls may become active.
        val preflight = kernel.health()
        if (preflight.activeWakeRepairTarget() != AlarmRepairTarget.NONE) {
            kernel.cancelSchedule()
            return
        }

        when (kernel.beginActive(occurrenceId)) {
            BeginActiveResult.STARTED -> {
                kernel.health().activeOccurrence
                    ?.takeIf { it.id == occurrenceId }
                    ?.let { WakeTimingTrace(context).receiver(it) }
                AlarmPlaybackService.start(context, occurrenceId)
            }

            BeginActiveResult.ALREADY_ACTIVE -> AlarmPlaybackService.start(context, occurrenceId)
            BeginActiveResult.STALE -> Unit
        }
    }
}
