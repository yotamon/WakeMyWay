package com.wakemyway.app

import com.github.takahirom.roborazzi.captureRoboImage
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.voice.WakeVoiceMode
import com.wakemyway.app.voice.WakeVoiceUiState
import com.wakemyway.core.preparation.PreparedWakePlanPreparer
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.preparation.TomorrowContractId
import com.wakemyway.core.schedule.WakeOccurrenceId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Non-canonical responsive smoke renders.
 *
 * These intentionally live under a screenshots subdirectory so the approved 393×852 golden hash
 * contract remains stable while CI still proves compact-device compositions render successfully.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "en-rUS-w360dp-h640dp-night-mdpi",
)
class WakeResponsiveVisualSmokeTest {
    @Test
    fun compactListeningWakeRenders() {
        captureRoboImage("responsive/wake_listening_360x640.png") {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = null,
                    onSnooze = {},
                    onStop = {},
                    displayTime = "07:30",
                    displayDate = "Tuesday · 14 Jan",
                    voiceState = WakeVoiceUiState(
                        mode = WakeVoiceMode.LISTENING,
                        spokenLine = "Morning. Tell me you're with me.",
                        speechAvailable = true,
                        voiceInputAvailable = true,
                    ),
                )
            }
        }
    }

    @Test
    fun compactOrientedWakeRenders() {
        captureRoboImage("responsive/wake_oriented_360x640.png") {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = compactPreparedPlan(),
                    onSnooze = {},
                    onStop = {},
                    displayTime = "07:32",
                    displayDate = "Tuesday · 14 Jan",
                    voiceState = WakeVoiceUiState(
                        mode = WakeVoiceMode.ORIENTING,
                        spokenLine = "Good morning.",
                        speechAvailable = true,
                        voiceInputAvailable = true,
                    ),
                )
            }
        }
    }

    private fun compactPreparedPlan() = PreparedWakePlanPreparer.prepare(
        contract = TomorrowContract(
            id = TomorrowContractId("compact-contract"),
            wakeOccurrenceId = WakeOccurrenceId("compact-wake"),
            rawText = "Design review at 10:00. Arrive calm and prepared.",
            firstMove = "Shower",
            createdAtEpochMillis = 1L,
        ),
        preparedAtEpochMillis = 2L,
    )
}
