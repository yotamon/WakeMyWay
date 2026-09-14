package com.wakemyway.app.alarm

import android.content.Context
import android.content.Intent
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.Duration

/**
 * UI-facing terminal transaction boundary for an active wake.
 *
 * AlarmKernel is the durable authority. The Wake Surface may disappear only after the durable Stop
 * or Snooze mutation succeeds, or when this occurrence is already no longer active because another
 * terminal surface won the race. AlarmPlaybackService is then stopped as the playback executor; its
 * notification-action handlers remain independently safe for actions initiated outside the Activity.
 */
class WakeTerminalActions(context: Context) {
    private val appContext = context.applicationContext
    private val kernel = AlarmKernel(appContext)
    private val trace = WakeTimingTrace(appContext)

    fun stop(occurrenceId: WakeOccurrenceId): Boolean {
        // A notification action or another retained surface may already have completed this exact
        // occurrence. Treat that as acknowledged success for the stale UI, but never mutate a newer
        // active occurrence from this old surface.
        if (kernel.activeOccurrence()?.id != occurrenceId) return true

        val stopped = runCatching { kernel.stopActive(occurrenceId) }
            .getOrElse {
                // Some terminal transitions can commit durable state before a later best-effort
                // future-registration step fails. If this occurrence is no longer authoritative,
                // the user's Stop intent has already been durably satisfied.
                if (kernel.activeOccurrence()?.id != occurrenceId) {
                    finishPlaybackAfterStop(occurrenceId)
                    return true
                }
                return false
            }
        if (!stopped) return kernel.activeOccurrence()?.id != occurrenceId

        finishPlaybackAfterStop(occurrenceId)
        return true
    }

    fun snooze(
        occurrenceId: WakeOccurrenceId,
        duration: Duration = DEFAULT_SNOOZE,
    ): Boolean {
        if (kernel.activeOccurrence()?.id != occurrenceId) return true

        val replacement = runCatching { kernel.snoozeActive(occurrenceId, duration) }
            .getOrNull()
            ?: return kernel.activeOccurrence()?.id != occurrenceId

        trace.snoozed(occurrenceId)
        trace.expected(
            occurrence = replacement,
            scenario = WakeTimingTrace.SCENARIO_SNOOZE_REPLACEMENT,
            expectFullScreen = kernel.health().fullScreenIntentAllowed,
        )
        appContext.stopService(Intent(appContext, AlarmPlaybackService::class.java))
        return true
    }

    private fun finishPlaybackAfterStop(occurrenceId: WakeOccurrenceId) {
        trace.stopped(occurrenceId)
        appContext.stopService(Intent(appContext, AlarmPlaybackService::class.java))
    }

    companion object {
        val DEFAULT_SNOOZE: Duration = Duration.ofMinutes(5)
    }
}
