package com.wakemyway.app.product.learning

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.io.File
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeLearningPersistenceInstrumentedTest {
    private val context
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun learnedPolicySurvivesRepositoryRecreationAndCorruptionFailsClosed() {
        val suffix = System.nanoTime().toString()
        val historyFile = "learning-instrumented-history-$suffix.json"
        val stateFile = "learning-instrumented-state-$suffix.json"
        val history = WakeHistoryRepository(context, fileName = historyFile)

        repeat(4) { index ->
            history.record(
                entry(
                    id = "stopped-$index",
                    finishedAt = Instant.parse("2026-09-24T05:0${index + 1}:00Z"),
                    reason = WakeHistoryTerminalReason.STOPPED,
                    activation = null,
                ),
            )
        }

        val first = WakeLearningRepository(
            context = context,
            historyRepository = history,
            fileName = stateFile,
        )
        val learned = first.refresh()
        assertTrue(learned.policy.version > 1)

        val recreated = WakeLearningRepository(
            context = context,
            historyRepository = WakeHistoryRepository(context, fileName = historyFile),
            fileName = stateFile,
        )
        assertEquals(learned.policy, recreated.resolvePolicy())

        File(context.noBackupFilesDir, stateFile).writeText("{not-json")
        val corrupted = WakeLearningRepository(
            context = context,
            historyRepository = WakeHistoryRepository(context, fileName = historyFile),
            fileName = stateFile,
        )
        assertEquals(1, corrupted.resolvePolicy().version)
    }

    @Test
    fun resetRemovesOnlyLearnedSnapshotAndPreservesWakeHistory() {
        val suffix = System.nanoTime().toString()
        val historyFile = "learning-reset-history-$suffix.json"
        val stateFile = "learning-reset-state-$suffix.json"
        val history = WakeHistoryRepository(context, fileName = historyFile)

        repeat(4) { index ->
            history.record(
                entry(
                    id = "reset-stopped-$index",
                    finishedAt = Instant.parse("2026-09-24T06:0${index + 1}:00Z"),
                    reason = WakeHistoryTerminalReason.STOPPED,
                    activation = null,
                ),
            )
        }

        val repository = WakeLearningRepository(
            context = context,
            historyRepository = history,
            fileName = stateFile,
        )
        assertTrue(repository.refresh().policy.version > 1)
        assertTrue(File(context.noBackupFilesDir, stateFile).exists())

        val reset = repository.resetToDefault()

        assertEquals(1, reset.policy.version)
        assertFalse(File(context.noBackupFilesDir, stateFile).exists())
        assertEquals(4, history.list().size)
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