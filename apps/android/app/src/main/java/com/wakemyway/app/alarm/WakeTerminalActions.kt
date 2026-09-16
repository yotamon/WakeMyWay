package com.wakemyway.app.alarm

import android.content.Context
import android.content.Intent
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.Duration

enum class WakeTerminalReason {
    COMPLETED,
    STOPPED,
    SNOOZED,
}

/**
 * Non-authoritative observer invoked only after AlarmKernel has durably committed a terminal action.
 * Observer failures are deliberately isolated from the terminal transaction result.
 */
interface WakeTerminalObserver {
    fun onTerminal(
        occurrence: WakeOccurrence,
        reason: WakeTerminalReason,
        replacement: WakeOccurrence? = null,
    )

    companion object {
        val NONE: WakeTerminalObserver = object : WakeTerminalObserver {
            override fun onTerminal(
                occurrence: WakeOccurrence,
                reason: WakeTerminalReason,
                replacement: WakeOccurrence?,
            ) = Unit
        }
    }
}

/**
 * UI-facing terminal transaction boundary for an active wake.
 *
 * AlarmKernel is the durable authority. The Wake Surface may disappear only after the durable Stop
 * or Snooze mutation succeeds, or when this occurrence is already no longer active because another
 * terminal surface won the race. A stale old surface must never stop playback for a newer wake.
 *
 * Phase B adds per-alarm Snooze policy. The policy is read from device-protected critical state so
 * the exact same terminal behavior is available before first unlock.
 */
class WakeTerminalActions internal constructor(
    context: Context,
    private val kernel: AlarmKernel = AlarmKernel(context.applicationContext),
    private val trace: WakeTimingTrace = WakeTimingTrace(context.applicationContext),
    private val observer: WakeTerminalObserver = WakeTerminalObserver.NONE,
) {
    private val appContext = context.applicationContext

    fun stop(
        occurrenceId: WakeOccurrenceId,
        reason: WakeTerminalReason = WakeTerminalReason.STOPPED,
    ): Boolean {
        require(reason != WakeTerminalReason.SNOOZED) {
            "Stop cannot be recorded as Snoozed"
        }
        val activeOccurrence = kernel.activeOccurrence()
        when (activeState(occurrenceId, activeOccurrence)) {
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
        notifyObserver(requireNotNull(activeOccurrence), reason)
        return true
    }

    fun snooze(
        occurrenceId: WakeOccurrenceId,
        duration: Duration? = null,
    ): Boolean {
        val activeOccurrence = kernel.activeOccurrence()
        when (activeState(occurrenceId, activeOccurrence)) {
            ActiveState.ALREADY_TERMINAL -> {
                stopPlaybackComponent()
                return true
            }
            ActiveState.STALE_SURFACE -> return true
            ActiveState.CURRENT -> Unit
        }

        val policy = kernel.activePolicy(occurrenceId)
        if (policy != null && !policy.snoozeEnabled) return false
        val resolvedDuration = duration ?: policy?.snoozeDuration ?: DEFAULT_SNOOZE

        val replacement = runCatching { kernel.snoozeActive(occurrenceId, resolvedDuration) }
            .getOrNull()
            ?: return acknowledgePostMutationState(occurrenceId, recordStop = false)

        trace.snoozed(occurrenceId)
        trace.expected(
            occurrence = replacement,
            scenario = WakeTimingTrace.SCENARIO_SNOOZE_REPLACEMENT,
            expectFullScreen = kernel.health().fullScreenIntentAllowed,
        )
        stopPlaybackComponent()
        notifyObserver(
            occurrence = requireNotNull(activeOccurrence),
            reason = WakeTerminalReason.SNOOZED,
            replacement = replacement,
        )
        return true
    }

    private fun acknowledgePostMutationState(
        occurrenceId: WakeOccurrenceId,
        recordStop: Boolean,
    ): Boolean = when (activeState(occurrenceId, kernel.activeOccurrence())) {
        ActiveState.CURRENT -> false
        ActiveState.STALE_SURFACE -> true
        ActiveState.ALREADY_TERMINAL -> {
            if (recordStop) trace.stopped(occurrenceId)
            stopPlaybackComponent()
            true
        }
    }

    private fun activeState(
        occurrenceId: WakeOccurrenceId,
        activeOccurrence: WakeOccurrence?,
    ): ActiveState = when {
        activeOccurrence == null -> ActiveState.ALREADY_TERMINAL
        activeOccurrence.id == occurrenceId -> ActiveState.CURRENT
        else -> ActiveState.STALE_SURFACE
    }

    private fun finishPlaybackAfterStop(occurrenceId: WakeOccurrenceId) {
        trace.stopped(occurrenceId)
        stopPlaybackComponent()
    }

    private fun notifyObserver(
        occurrence: WakeOccurrence,
        reason: WakeTerminalReason,
        replacement: WakeOccurrence? = null,
    ) {
        runCatching {
            observer.onTerminal(
                occurrence = occurrence,
                reason = reason,
                replacement = replacement,
            )
        }
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
