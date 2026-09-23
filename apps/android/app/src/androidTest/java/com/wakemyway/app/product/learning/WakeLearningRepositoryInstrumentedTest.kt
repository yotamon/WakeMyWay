package com.wakemyway.app.product.learning

import android.content.Context
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
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeLearningRepositoryInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun learnedPolicySurvivesRepositoryRecreationAndResetPreservesHistory() {
        val token = System.nanoTime().toString()
        val historyFile = "learning-device-history-${token}.json"
        val learningFile = "learning-device-state-${token}.json"
        val history = WakeHistoryRepository(context, fileName = historyFile)

        repeat(4) { index ->
            history.record(
                entry(
                    id = "device-stopped-${index}",
                    finishedAt = Instant.parse("2026-09-24T06:0${index + 1}:00Z"),
                ),
            )
        }

        val first = WakeLearningRepository(
            context = context,
            historyRepository = history,
            fileName = learningFile,
        )
        assertEquals(2, first.refresh().policy.version)

        val recreatedHistory = WakeHistoryRepository(context, fileName = historyFile)
        val recreated = WakeLearningRepository(
            context = context,
            historyRepository = recreatedHistory,
            fileName = learningFile,
        )

        assertEquals(2, recreated.resolvePolicy().version)

        val reset = recreated.resetToDefault()

        assertEquals(1, reset.policy.version)
        assertEquals(4, recreatedHistory.list().size)
        assertEquals(1, recreated.resolvePolicy().version)
    }

    @Test
    fun corruptLearnedStateFailsClosedToStableDefault() {
        val token = System.nanoTime().toString()
        val learningFile = "learning-device-corrupt-${token}.json"
        File(context.noBackupFilesDir, learningFile).writeText("{not-json")

        val repository = WakeLearningRepository(
            context = context,
            fileName = learningFile,
        )

        assertEquals(1, repository.resolvePolicy().version)
    }

    private fun entry(
        id: String,
        finishedAt: Instant,
    ) = WakeHistoryEntry(
        sessionId = WakeSessionId("session-${id}"),
        occurrenceId = WakeOccurrenceId(id),
        scheduleId = WakeScheduleId("weekday"),
        occurrenceKind = WakeOccurrenceKind.PRIMARY,
        scheduleRevision = 1,
        scheduledAt = finishedAt.minus(Duration.ofMinutes(2)),
        startedAt = finishedAt.minus(Duration.ofMinutes(1)),
        finishedAt = finishedAt,
        terminalReason = WakeHistoryTerminalReason.STOPPED,
        behavior = WakeBehaviorObservation(
            policyVersion = 1,
            timeToFirstEngagement = Duration.ofSeconds(8),
            timeToMeaningfulMovement = null,
            timeToActivationCompletion = null,
            maxInterventionDepth = 2,
        ),
        behaviorTimingOrigin = WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
    )
}
