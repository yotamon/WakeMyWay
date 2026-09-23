package com.wakemyway.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.wakemyway.app.ui.alarms.AlarmEditorResult
import com.wakemyway.app.ui.alarms.AlarmEditorScreen
import com.wakemyway.app.ui.onboarding.OnboardingScreen
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
 * Non-canonical accessibility smoke renders.
 *
 * These protect compact-screen + large-text composition from regressions without expanding the
 * curated visual-golden contract. The images live below screenshots/responsive/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "en-rUS-w360dp-h640dp-night-mdpi",
)
class ProductAccessibilityVisualSmokeTest {
    @Test
    fun onboardingLargeTextCompactRenders() {
        captureRoboImage("responsive/onboarding_large_text_360x640.png") {
            LargeText {
                WakeMyWayTheme {
                    OnboardingScreen(
                        onComplete = {},
                        onSkip = {},
                    )
                }
            }
        }
    }

    @Test
    fun alarmEditorLargeTextCompactRenders() {
        captureRoboImage("responsive/alarm_editor_large_text_360x640.png") {
            LargeText {
                WakeMyWayTheme {
                    AlarmEditorScreen(
                        existing = null,
                        onBack = {},
                        onSave = { AlarmEditorResult(saved = true) },
                    )
                }
            }
        }
    }

    @Test
    fun wakeListeningLargeTextCompactRenders() {
        captureRoboImage("responsive/wake_listening_large_text_360x640.png") {
            LargeText {
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
    }

    @Test
    fun wakeOrientedLargeTextCompactRenders() {
        captureRoboImage("responsive/wake_oriented_large_text_360x640.png") {
            LargeText {
                WakeMyWayTheme {
                    WakeSurface(
                        preparedPlan = accessibilityPreparedPlan(),
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
    }
}

@Composable
private fun LargeText(content: @Composable () -> Unit) {
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = 1.6f,
        ),
    ) {
        content()
    }
}

private fun accessibilityPreparedPlan() = PreparedWakePlanPreparer.prepare(
    contract = TomorrowContract(
        id = TomorrowContractId("accessibility-contract"),
        wakeOccurrenceId = WakeOccurrenceId("accessibility-wake"),
        rawText = "Design review at 10:00. Arrive calm and prepared.",
        firstMove = "Open the curtains and start the shower",
        createdAtEpochMillis = 1L,
    ),
    preparedAtEpochMillis = 2L,
)
