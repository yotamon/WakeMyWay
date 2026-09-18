package com.wakemyway.app.product.history

import android.content.Context
import com.wakemyway.app.alarm.WakeTerminalObserver
import com.wakemyway.app.alarm.WakeTerminalReason
import com.wakemyway.app.product.learning.WakeLearningRepository
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
    private val onHistoryChanged: () -> Unit = {},
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
        onHistoryChanged = {
            runCatching {
                WakeLearningRepository(context.applicationContext).refresh()
            }
        },
    )

    @Synchronized
    fun observeRuntimeTransition(
        before: WakeSessionSnapshot,
        input: WakeInput,
        transition: WakeTransition,
        elapsedSinceRuntimeStart: Duration,
    ) {
        if (terminalRecorded) return
        runCatching {
            behaviorTracker.observe(
                before = before,
                input = input,
                transition = transition,
                elapsedSinceAlarm = elapsedSinceRuntimeStart,
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

        val historyReason = when (reason) {
            WakeTerminalReason.COMPLETED -> WakeHistoryTerminalReason.COMPLETED
            WakeTerminalReason.STOPPED -> WakeHistoryTerminalReason.STOPPED
            WakeTerminalReason.SNOOZED -> WakeHistoryTerminalReason.SNOOZED
        }
        if (historyReason == WakeHistoryTerminalReason.SNOOZED && replacement == null) return

        val entry = runCatching {
            val behavior = behaviorTracker.snapshot()
            WakeHistoryEntry(
                sessionId = sessionId,
                occurrenceId = this.occurrence.id,
                scheduleId = this.occurrence.wakeScheduleId,
                occurrenceKind = this.occurrence.kind,
                scheduleRevision = this.occurrence.scheduleRevision,
                scheduledAt = this.occurrence.scheduledAt.toInstant(),
                scheduledLocalDateTime = this.occurrence.scheduledLocalDateTime,
                scheduledZoneId = this.occurrence.scheduledAt.zone,
                startedAt = startedAt,
                finishedAt = clock.instant().atLeast(startedAt),
                terminalReason = historyReason,
                replacementOccurrenceId = replacement?.id,
                behavior = behavior,
                behaviorTimingOrigin = behavior?.let {
                    WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START
                },
            )
        }.getOrNull() ?: return

        val recorded = runCatching { repository.record(entry) }.isSuccess
        if (recorded) {
            terminalRecorded = true
            runCatching(onHistoryChanged)
        }
    }

    private fun Instant.atLeast(minimum: Instant): Instant =
        if (isBefore(minimum)) minimum else this
}
