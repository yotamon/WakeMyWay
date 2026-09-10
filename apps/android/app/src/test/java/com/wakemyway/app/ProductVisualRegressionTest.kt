package com.wakemyway.app

import com.github.takahirom.roborazzi.captureRoboImage
import com.wakemyway.app.ui.home.TonightScreen
import com.wakemyway.app.ui.home.TonightUiState
import com.wakemyway.app.ui.setup.WakeSetupCommitResult
import com.wakemyway.app.ui.setup.WakeSetupScreen
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "en-rUS-w393dp-h852dp-night-mdpi",
)
class ProductVisualRegressionTest {
    @Test
    fun tonightReady() {
        captureRoboImage("tonight_ready.png") {
            WakeMyWayTheme {
                TonightScreen(
                    state = TonightUiState(
                        wakeTime = "08:00",
                        dateLabel = "Thursday · Sep 10",
                        hasOccurrence = true,
                        wakeReady = true,
                        readinessDetail = "Exact alarm, wake screen, and Stop/Snooze controls are ready locally.",
                        hasTomorrowContract = true,
                        tomorrowContractPrepared = true,
                    ),
                    onOpenWakeSetup = {},
                    onOpenTomorrowPlan = {},
                    onOpenWakeLab = {},
                )
            }
        }
    }

    @Test
    fun tonightEmpty() {
        captureRoboImage("tonight_empty.png") {
            WakeMyWayTheme {
                TonightScreen(
                    state = TonightUiState(
                        wakeTime = "--:--",
                        dateLabel = "Tomorrow",
                        hasOccurrence = false,
                        wakeReady = false,
                        readinessDetail = "Set a wake time to prepare tomorrow.",
                        hasTomorrowContract = false,
                        tomorrowContractPrepared = false,
                    ),
                    onOpenWakeSetup = {},
                    onOpenTomorrowPlan = {},
                    onOpenWakeLab = {},
                )
            }
        }
    }

    @Test
    fun tonightNeedsFullScreenWakeAccess() {
        captureRoboImage("tonight_needs_full_screen_access.png") {
            WakeMyWayTheme {
                TonightScreen(
                    state = TonightUiState(
                        wakeTime = "08:00",
                        dateLabel = "Thursday · Sep 10",
                        hasOccurrence = true,
                        wakeReady = false,
                        readinessDetail = "Allow full-screen alarms so Wake My Way can open the wake screen when your phone is locked.",
                        wakeRepairActionLabel = "Allow full-screen alarms",
                        hasTomorrowContract = false,
                        tomorrowContractPrepared = false,
                    ),
                    onOpenWakeSetup = {},
                    onOpenTomorrowPlan = {},
                    onOpenWakeLab = {},
                )
            }
        }
    }

    @Test
    fun wakeSetupWeekly() {
        captureRoboImage("wake_setup_weekly.png") {
            WakeMyWayTheme {
                WakeSetupScreen(
                    existingSchedule = WakeSchedule(
                        id = WakeScheduleId("visual-weekly"),
                        zoneId = ZoneId.of("Europe/Berlin"),
                        timesByDay = mapOf(
                            DayOfWeek.MONDAY to LocalTime.of(7, 45),
                            DayOfWeek.TUESDAY to LocalTime.of(7, 45),
                            DayOfWeek.WEDNESDAY to LocalTime.of(8, 15),
                            DayOfWeek.THURSDAY to LocalTime.of(7, 45),
                            DayOfWeek.FRIDAY to LocalTime.of(8, 0),
                        ),
                        revision = 2,
                    ),
                    onBack = {},
                    onCommit = { WakeSetupCommitResult(true) },
                    onDisable = { WakeSetupCommitResult(true) },
                )
            }
        }
    }

    @Test
    fun wakeEmerging() {
        captureRoboImage("wake_emerging.png") {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = null,
                    onSnooze = {},
                    onStop = {},
                    displayTime = "08:00",
                )
            }
        }
    }
}
