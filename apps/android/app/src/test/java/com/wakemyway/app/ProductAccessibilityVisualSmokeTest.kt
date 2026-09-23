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
