package com.wakemyway.app.product.history

import android.content.Context
import com.wakemyway.app.alarm.WakeTerminalObserver
import com.wakemyway.app.alarm.WakeTerminalReason
import com.wakemyway.core.learning.WakeBehaviorEvidenceTracker
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.runtime.WakeSessionSnapshot
import com.wakemyway.core.runtime.WakeTransition
import com.wakemyway.core.schedule.WakeOccurrence
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Best-effort bridge between WakeRuntime observations and durable AlarmKernel terminal facts.
 *
 * The recorder never participates in terminal acknowledgement. Repository failures are swallowed so
 * credential lock, storage corruption, or I/O failure can only result in missing history.
 */
class WakeHistorySessionRecorder internal constructor(
    private val occurrence: WakeOccurrence,
    private val repository: WakeHistoryRepository,
    private val clock: Clock = Clock.systemUTC(),
) : WakeTerminalObserver {
    private val sessionId = WakeSessionId("wake-${occurrence.id.value}")
    private val startedAt = clock.instant()
    private val behaviorTracker = WakeBehaviorEvidenceTracker()
    private var terminalRecorded = false

    constructor(
        context: Context,
        occurrence: WakeOccurrence,
    ) : this(
        occurrence = occurrence,
        repository = WakeHistoryRepository(context.applicationContext),
    )

    @Synchronized
    fun observeRuntimeTransition(
        before: WakeSessionSnapshot,
        input: WakeInput,
        transition: WakeTransition,
        elapsedSinceAlarm: Duration,
    ) {
        if (terminalRecorded) return
        runCatching {
            behaviorTracker.observe(
                before = before,
                input = input,
                transition = transition,
                elapsedSinceAlarm = elapsedSinceAlarm,
            )
        }
    }

    @Synchronized
    override fun onTerminal(
        occurrence: WakeOccurrence,
        reason: WakeTerminalReason,
        replacement: WakeOccurrence?,
    ) {
        if (terminalRecorded || occurrence.id != this.occurrence.id) return
        terminalRecorded = true

        val historyReason = when (reason) {
            WakeTerminalReason.COMPLETED -> WakeHistoryTerminalReason.COMPLETED
            WakeTerminalReason.STOPPED -> WakeHistoryTerminalReason.STOPPED
            WakeTerminalReason.SNOOZED -> WakeHistoryTerminalReason.SNOOZED
        }
        if (historyReason == WakeHistoryTerminalReason.SNOOZED && replacement == null) return

        val entry = runCatching {
            WakeHistoryEntry(
                sessionId = sessionId,
                occurrenceId = this.occurrence.id,
                scheduleId = this.occurrence.wakeScheduleId,
                occurrenceKind = this.occurrence.kind,
                scheduleRevision = this.occurrence.scheduleRevision,
                scheduledAt = this.occurrence.scheduledAt.toInstant(),
                startedAt = startedAt,
                finishedAt = clock.instant().atLeast(startedAt),
                terminalReason = historyReason,
                replacementOccurrenceId = replacement?.id,
                behavior = behaviorTracker.snapshot(),
            )
        }.getOrNull() ?: return

        runCatching { repository.record(entry) }
    }

    private fun Instant.atLeast(minimum: Instant): Instant =
        if (isBefore(minimum)) minimum else this
}
