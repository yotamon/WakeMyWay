package com.wakemyway.app.learning

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeFeedbackRating
import com.wakemyway.core.learning.WakeLearningDecision
import com.wakemyway.core.learning.WakeOutcomeDeriver
import com.wakemyway.core.learning.WakeTimeline
import com.wakemyway.core.learning.WakeTimelineFact
import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeSessionId
import java.io.File
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeLearningStoreInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun repeatedOutcomesPersistLearnedPolicyAcrossManagerInstances() {
        val fileName = "wake-learning-instrumented-${System.nanoTime()}.bin"
        val store = PrivateWakeLearningStore(context, fileName)
        try {
            val manager = WakeLearningManager(context, store)
            repeat(4) { index ->
                manager.recordOutcome(slowMovementOutcome("persist-$index", policyVersion = 1, startedAt = 1_800_000_000_000L + index))
            }

            val learned = manager.snapshot()
            assertEquals(2, learned.activePolicy.version)
            assertEquals(Duration.ofSeconds(10), learned.activePolicy.movementPromptDelay)
            assertEquals(WakeLearningStorageStatus.LEARNED, learned.status)

            val reloaded = WakeLearningManager(context, PrivateWakeLearningStore(context, fileName)).snapshot()
            assertEquals(learned.activePolicy, reloaded.activePolicy)
            assertEquals(4, reloaded.outcomes.size)
            assertEquals(learned.profile, reloaded.profile)
        } finally {
            store.clear()
        }
    }

    @Test
    fun resetRestoresStableDefaultPolicyAndClearsDerivedHistory() {
        val fileName = "wake-learning-reset-${System.nanoTime()}.bin"
        val store = PrivateWakeLearningStore(context, fileName)
        try {
            val manager = WakeLearningManager(context, store)
            repeat(4) { index ->
                manager.recordOutcome(slowMovementOutcome("reset-$index", 1, 1_800_000_000_000L + index))
            }
            assertEquals(2, manager.activePolicy().version)

            manager.reset()

            val reset = manager.snapshot()
            assertEquals(WakePolicy(), reset.activePolicy)
            assertTrue(reset.outcomes.isEmpty())
            assertEquals(WakeLearningStorageStatus.DEFAULT, reset.status)
        } finally {
            store.clear()
        }
    }

    @Test
    fun corruptDerivedStateFallsBackToDefaultWithoutBlockingWakePolicySelection() {
        val fileName = "wake-learning-corrupt-${System.nanoTime()}.bin"
        val file = File(File(context.noBackupFilesDir, "wake-learning"), fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        try {
            val snapshot = WakeLearningManager(
                context,
                PrivateWakeLearningStore(context, fileName),
            ).snapshot()

            assertEquals(WakePolicy(), snapshot.activePolicy)
            assertEquals(WakeLearningStorageStatus.STORAGE_FALLBACK, snapshot.status)
            assertTrue(snapshot.outcomes.isEmpty())
        } finally {
            file.delete()
        }
    }

    @Test
    fun deviceProtectedStorageIsRejected() {
        val deviceProtected = context.createDeviceProtectedStorageContext()
        try {
            PrivateWakeLearningStore(deviceProtected, "forbidden-learning.bin")
            fail("Wake Learning must reject device-protected storage")
        } catch (_: IllegalArgumentException) {
            // Expected. Derived/private learning state is not part of Direct Boot alarm authority.
        }
    }

    @Test
    fun delayedCalibrationUpdatePreservesActivationCompletionDisagreement() {
        val fileName = "wake-learning-calibration-${System.nanoTime()}.bin"
        val store = PrivateWakeLearningStore(context, fileName)
        try {
            val manager = WakeLearningManager(context, store)
            val outcome = WakeOutcomeDeriver.derive(
                timeline = timeline("calibration", 1, 1_800_000_000_000L, movementSeconds = 35),
            )
            val recorded = manager.recordOutcome(outcome)
            assertTrue(recorded.decision is WakeLearningDecision.NoChange)

            val calibrated = manager.applyCalibration(
                outcomeId = outcome.id,
                calibration = WakeCalibration.RETURNED_TO_BED,
                annoyance = WakeFeedbackRating(3),
                agency = WakeFeedbackRating(3),
            )!!

            val stored = calibrated.snapshot.outcomes.single()
            assertTrue(stored.activationCompleted)
            assertEquals(WakeCalibration.RETURNED_TO_BED, stored.calibration)
            assertEquals(com.wakemyway.core.learning.ConfirmedWakeSuccess.NOT_CONFIRMED, stored.confirmedWakeSuccess)
        } finally {
            store.clear()
        }
    }

    private fun slowMovementOutcome(
        id: String,
        policyVersion: Int,
        startedAt: Long,
    ) = WakeOutcomeDeriver.derive(
        timeline = timeline(id, policyVersion, startedAt, movementSeconds = 90),
        calibration = WakeCalibration.GOT_UP,
        annoyance = WakeFeedbackRating(2),
        agency = WakeFeedbackRating(4),
    )

    private fun timeline(
        id: String,
        policyVersion: Int,
        startedAt: Long,
        movementSeconds: Long,
    ) = WakeTimeline(
        sessionId = WakeSessionId(id),
        policyVersion = policyVersion,
        startedAtEpochMillis = startedAt,
        terminalOutcome = WakeOutcome.COMPLETED,
        facts = listOf(
            WakeTimelineFact.EngagementObserved(Duration.ofSeconds(10)),
            WakeTimelineFact.InterventionDepth(Duration.ofSeconds(12), 1),
            WakeTimelineFact.MotionObserved(Duration.ofSeconds(movementSeconds), MotionEvidenceKind.SUSTAINED_MOVEMENT),
            WakeTimelineFact.ActivationCompleted(Duration.ofSeconds(movementSeconds + 20)),
        ),
    )
}
