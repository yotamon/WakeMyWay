package com.wakemyway.app.alarm

import android.content.Context
import android.content.Intent
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.Duration

/**
 * UI-facing terminal transaction boundary for an active wake.
 *
 * AlarmKernel is the durable authority. The Wake Surface may disappear only after the durable Stop
 * or Snooze mutation succeeds. AlarmPlaybackService is then stopped as the playback executor; its
 * notification-action handlers remain independently safe for terminal actions initiated outside the
 * Activity.
 */
class WakeTerminalActions(context: Context) {
    private val appContext = context.applicationContext
    private val kernel = AlarmKernel(appContext)
    private val trace = WakeTimingTrace(appContext)

    fun stop(occurrenceId: WakeOccurrenceId): Boolean {
        if (!kernel.stopActive(occurrenceId)) return false

        trace.stopped(occurrenceId)
        appContext.stopService(Intent(appContext, AlarmPlaybackService::class.java))
        return true
    }

    fun snooze(
        occurrenceId: WakeOccurrenceId,
        duration: Duration = DEFAULT_SNOOZE,
    ): Boolean {
        val replacement = kernel.snoozeActive(occurrenceId, duration) ?: return false

        trace.snoozed(occurrenceId)
        trace.expected(
            occurrence = replacement,
            scenario = WakeTimingTrace.SCENARIO_SNOOZE_REPLACEMENT,
            expectFullScreen = kernel.health().fullScreenIntentAllowed,
        )
        appContext.stopService(Intent(appContext, AlarmPlaybackService::class.java))
        return true
    }

    companion object {
        val DEFAULT_SNOOZE: Duration = Duration.ofMinutes(5)
    }
}
