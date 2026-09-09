package com.wakemyway.app.learning

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.core.learning.LearnedWakeProfile
import com.wakemyway.core.learning.WakeOutcomeId
import com.wakemyway.core.runtime.WakePolicy
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeLearningProfileFallbackInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun unsupportedLearningAlgorithmProfileFallsBackToStableDefaultPolicy() {
        val fileName = "wake-learning-unsupported-${System.nanoTime()}.bin"
        val store = PrivateWakeLearningStore(context, fileName)
        try {
            store.writeState(
                StoredWakeLearningState(
                    profile = LearnedWakeProfile(
                        learningAlgorithmVersion = 99,
                        activePolicy = WakePolicy(
                            version = 7,
                            movementPromptDelay = Duration.ofSeconds(5),
                        ),
                        sourceOutcomeIds = listOf(WakeOutcomeId("legacy-source")),
                        explanation = "Legacy learning fixture",
                        updatedAtEpochMillis = 1_800_000_000_000L,
                    ),
                ),
            )

            val snapshot = WakeLearningManager(context, store).snapshot()

            assertEquals(WakePolicy(), snapshot.activePolicy)
            assertEquals(WakeLearningStorageStatus.PROFILE_FALLBACK, snapshot.status)
            assertNull(snapshot.profile)
        } finally {
            store.clear()
        }
    }
}
