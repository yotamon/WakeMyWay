package com.wakemyway.app.product.learning

import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeCalibrationOutcome
import com.wakemyway.core.runtime.WakePolicy
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
    fun `valid learned policy survives repository recreation`() {
        val historyFile = "restart-history-${System.nanoTime()}.json"
        val learningFile = "restart-learning-${System.nanoTime()}.json"
        val history = WakeHistoryRepository(
            context = context,
            fileName = historyFile,
        )
        repeat(4) { index ->
            history.record(
                entry(
                    id = "restart-stopped-$index",
                    finishedAt = Instant.parse("2026-09-16T08:0${index + 1}:00Z"),
                    reason = WakeHistoryTerminalReason.STOPPED,
                    activation = null,
                ),
            )
        }
        val first = WakeLearningRepository(
            context = context,
            historyRepository = history,
            fileName = learningFile,
        )
        val learned = first.refresh().policy

        val recreated = WakeLearningRepository(
            context = context,
            historyRepository = WakeHistoryRepository(context, fileName = historyFile),
            fileName = learningFile,
        )

        assertEquals(learned, recreated.resolvePolicy())
        assertTrue(recreated.resolvePolicy().version > WakePolicy().version)
    }

    @Test
    fun `corrupt persisted learning state fails closed to stable default`() {
        val learningFile = "corrupt-learning-${System.nanoTime()}.json"
        File(context.noBackupFilesDir, learningFile).writeText("{ definitely-not-valid-json")
        val learning = WakeLearningRepository(
            context = context,
            historyRepository = WakeHistoryRepository(
                context,
                fileName = "corrupt-history-${System.nanoTime()}.json",
            ),
            fileName = learningFile,
        )

        assertEquals(WakePolicy(), learning.resolvePolicy())
        assertEquals(WakePolicy(), learning.state().policy)
    }

    @Test
    fun `unsupported persisted learning schema fails closed to stable default`() {
        val learningFile = "unsupported-learning-${System.nanoTime()}.json"
        File(context.noBackupFilesDir, learningFile).writeText("""{"schemaVersion":999}""")
        val learning = WakeLearningRepository(
            context = context,
            historyRepository = WakeHistoryRepository(
                context,
                fileName = "unsupported-history-${System.nanoTime()}.json",
            ),
            fileName = learningFile,
        )

        assertEquals(WakePolicy(), learning.resolvePolicy())
        assertEquals(WakePolicy(), learning.state().policy)
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
