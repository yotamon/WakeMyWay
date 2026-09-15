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

        // An alarm that can make noise but cannot expose immediate controls is not safe to start.
        // These presentation capabilities are global Android capabilities, so losing them invalidates
        // every enabled schedule rather than only the occurrence that happened to fire first.
        val preflight = kernel.health()
        if (preflight.repairTarget() != AlarmRepairTarget.NONE) {
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
            BeginActiveResult.CONFLICT,
            BeginActiveResult.STALE,
            -> Unit
        }
    }
}
