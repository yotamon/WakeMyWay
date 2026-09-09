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

        when (AlarmKernel(context).beginActive(occurrenceId)) {
            BeginActiveResult.STARTED,
            BeginActiveResult.ALREADY_ACTIVE,
            -> AlarmPlaybackService.start(context, occurrenceId)

            BeginActiveResult.STALE -> Unit
        }
    }
}
