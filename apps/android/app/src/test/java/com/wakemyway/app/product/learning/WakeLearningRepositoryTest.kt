package com.wakemyway.app.product.learning

import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeCalibrationOutcome
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Duration
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WakeLearningRepositoryTest {
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `repeated incomplete activation creates and resolves one bounded learned policy`() {
        val history = WakeHistoryRepository(
            context = context,
            fileName = "learning-history-${System.nanoTime()}.json",
        )
        repeat(4) { index ->
            history.record(
                entry(
                    id = "stopped-$index",
                    finishedAt = Instant.parse("2026-09-16T06:0${index + 1}:00Z"),
                    reason = WakeHistoryTerminalReason.STOPPED,
                    activation = null,
                ),
            )
        }
        val learning = WakeLearningRepository(
            context = context,
            historyRepository = history,
            fileName = "learning-state-${System.nanoTime()}.json",
        )

        val state = learning.refresh()

        assertTrue(state.changedOnLatestRefresh)
        assertEquals(2, state.policy.version)
        assertEquals(4, state.policy.maxEscalationLevel)
        assertNotNull(state.lastAdjustment)
        assertEquals(state.policy, learning.resolvePolicy())
    }

    @Test
    fun `unsupported persisted schema fails closed to stable default`() {
        val fileName = "unsupported-learning-${System.nanoTime()}.json"
        File(context.noBackupFilesDir, fileName).writeText(
            """{"schemaVersion":999,"policy":{}}""",
        )
        val learning = WakeLearningRepository(
            context = context,
            fileName = fileName,
        )

        assertEquals(1, learning.resolvePolicy().version)
        assertEquals(1, learning.state().policy.version)
    }

    @Test
    fun `reset restores default policy without deleting source history`() {
        val history = WakeHistoryRepository(
            context = context,
            fileName = "reset-history-${System.nanoTime()}.json",
        )
        repeat(4) { index ->
            history.record(
                entry(
                    id = "reset-stopped-$index",
                    finishedAt = Instant.parse("2026-09-16T08:0${index + 1}:00Z"),
                    reason = WakeHistoryTerminalReason.STOPPED,
                    activation = null,
                ),
            )
        }
        val learning = WakeLearningRepository(
            context = context,
            historyRepository = history,
            fileName = "reset-state-${System.nanoTime()}.json",
        )

        assertEquals(2, learning.refresh().policy.version)

        val reset = learning.resetToDefault()

        assertEquals(1, reset.policy.version)
        assertEquals(4, history.list().size)
        assertEquals(1, learning.resolvePolicy().version)
    }

    @Test
    fun `return to bed calibration can strengthen activation after repeated false positives`() {
        val history = WakeHistoryRepository(
            context = context,
            fileName = "calibration-history-${System.nanoTime()}.json",
        )
        repeat(4) { index ->
            val entry = entry(
                id = "completed-$index",
                finishedAt = Instant.parse("2026-09-16T07:0${index + 1}:00Z"),
                reason = WakeHistoryTerminalReason.COMPLETED,
                activation = Duration.ofSeconds(40),
            )
            history.record(entry)
            history.attachCalibration(
                entry.occurrenceId,
                WakeCalibration(
                    if (index < 3) {
                        WakeCalibrationOutcome.RETURNED_TO_BED
                    } else {
                        WakeCalibrationOutcome.GOT_UP
                    },
                ),
            )
        }
        val learning = WakeLearningRepository(
            context = context,
            historyRepository = history,
            fileName = "calibration-state-${System.nanoTime()}.json",
        )

        val state = learning.refresh()

        assertTrue(state.changedOnLatestRefresh)
        assertEquals(2, state.policy.version)
        assertEquals(5, state.policy.activationThreshold)
    }

    private fun entry(
        id: String,
        finishedAt: Instant,
        reason: WakeHistoryTerminalReason,
        activation: Duration?,
    ) = WakeHistoryEntry(
        sessionId = WakeSessionId("session-$id"),
        occurrenceId = WakeOccurrenceId(id),
        scheduleId = WakeScheduleId("weekday"),
        occurrenceKind = WakeOccurrenceKind.PRIMARY,
        scheduleRevision = 1,
        scheduledAt = finishedAt.minus(Duration.ofMinutes(2)),
        startedAt = finishedAt.minus(Duration.ofMinutes(1)),
        finishedAt = finishedAt,
        terminalReason = reason,
        behavior = WakeBehaviorObservation(
            policyVersion = 1,
            timeToFirstEngagement = Duration.ofSeconds(8),
            timeToMeaningfulMovement = activation?.dividedBy(2),
            timeToActivationCompletion = activation,
            maxInterventionDepth = if (activation == null) 2 else 1,
        ),
        behaviorTimingOrigin = WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
    )
}
