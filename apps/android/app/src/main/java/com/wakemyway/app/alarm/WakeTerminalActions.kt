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
 * terminal surface won the race. A stale old surface must never stop playback for a newer wake.
 */
class WakeTerminalActions(context: Context) {
    private val appContext = context.applicationContext
    private val kernel = AlarmKernel(appContext)
    private val trace = WakeTimingTrace(appContext)

    fun stop(occurrenceId: WakeOccurrenceId): Boolean {
        when (activeState(occurrenceId)) {
            ActiveState.ALREADY_TERMINAL -> {
                stopPlaybackComponent()
                return true
            }
            ActiveState.STALE_SURFACE -> return true
            ActiveState.CURRENT -> Unit
        }

        val stopped = runCatching { kernel.stopActive(occurrenceId) }
            .getOrElse {
                return acknowledgePostMutationState(occurrenceId, recordStop = true)
            }
        if (!stopped) return acknowledgePostMutationState(occurrenceId, recordStop = false)

        finishPlaybackAfterStop(occurrenceId)
        return true
    }

    fun snooze(
        occurrenceId: WakeOccurrenceId,
        duration: Duration = DEFAULT_SNOOZE,
    ): Boolean {
        when (activeState(occurrenceId)) {
            ActiveState.ALREADY_TERMINAL -> {
                stopPlaybackComponent()
                return true
            }
            ActiveState.STALE_SURFACE -> return true
            ActiveState.CURRENT -> Unit
        }

        val replacement = runCatching { kernel.snoozeActive(occurrenceId, duration) }
            .getOrNull()
            ?: return acknowledgePostMutationState(occurrenceId, recordStop = false)

        trace.snoozed(occurrenceId)
        trace.expected(
            occurrence = replacement,
            scenario = WakeTimingTrace.SCENARIO_SNOOZE_REPLACEMENT,
            expectFullScreen = kernel.health().fullScreenIntentAllowed,
        )
        stopPlaybackComponent()
        return true
    }

    private fun acknowledgePostMutationState(
        occurrenceId: WakeOccurrenceId,
        recordStop: Boolean,
    ): Boolean = when (activeState(occurrenceId)) {
        ActiveState.CURRENT -> false
        ActiveState.STALE_SURFACE -> true
        ActiveState.ALREADY_TERMINAL -> {
            if (recordStop) trace.stopped(occurrenceId)
            stopPlaybackComponent()
            true
        }
    }

    private fun activeState(occurrenceId: WakeOccurrenceId): ActiveState {
        val activeId = kernel.activeOccurrence()?.id
        return when {
            activeId == null -> ActiveState.ALREADY_TERMINAL
            activeId == occurrenceId -> ActiveState.CURRENT
            else -> ActiveState.STALE_SURFACE
        }
    }

    private fun finishPlaybackAfterStop(occurrenceId: WakeOccurrenceId) {
        trace.stopped(occurrenceId)
        stopPlaybackComponent()
    }

    private fun stopPlaybackComponent() {
        appContext.stopService(Intent(appContext, AlarmPlaybackService::class.java))
    }

    private enum class ActiveState {
        CURRENT,
        ALREADY_TERMINAL,
        STALE_SURFACE,
    }

    companion object {
        val DEFAULT_SNOOZE: Duration = Duration.ofMinutes(5)
    }
}
