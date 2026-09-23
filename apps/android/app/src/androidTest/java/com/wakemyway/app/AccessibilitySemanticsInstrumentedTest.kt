package com.wakemyway.app

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.app.ui.alarms.AlarmsScreen
import com.wakemyway.app.ui.navigation.ConsumerTab
import com.wakemyway.app.ui.navigation.WmwConsumerScaffold
import com.wakemyway.app.ui.onboarding.OnboardingScreen
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.voice.WakeVoiceMode
import com.wakemyway.app.voice.WakeVoiceUiState
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listeningWakeKeepsCriticalSafetyActionsAccessible() {
        composeRule.setContent {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = null,
                    onSnooze = {},
                    onStop = {},
                    displayTime = "07:30",
                    displayDate = "Tuesday · 14 Jan",
                    snoozeMinutes = 5,
                    voiceState = WakeVoiceUiState(
                        mode = WakeVoiceMode.LISTENING,
                        spokenLine = "Morning.",
                        speechAvailable = true,
                        voiceInputAvailable = true,
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Snooze 5 min")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("Stop alarm")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun orientedWakeKeepsFirstMoveBeforeSafetyFooter() {
        composeRule.setContent {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = null,
                    defaultFirstMove = "Open the curtains",
                    onSnooze = {},
                    onStop = {},
                    onFirstMoveConfirmed = {},
                    displayTime = "07:32",
                    displayDate = "Tuesday · 14 Jan",
                    snoozeMinutes = 5,
                    voiceState = WakeVoiceUiState(
                        mode = WakeVoiceMode.ORIENTING,
                        spokenLine = "Good morning.",
                        speechAvailable = true,
                        voiceInputAvailable = true,
                    ),
                )
            }
        }

        val firstMove = composeRule.onNodeWithText("I’m doing it")
            .assertIsDisplayed()
            .assertHasClickAction()
            .fetchSemanticsNode()
            .boundsInRoot
        val snooze = composeRule.onNodeWithText("Snooze 5 min")
            .assertIsDisplayed()
            .assertHasClickAction()
            .fetchSemanticsNode()
            .boundsInRoot
        composeRule.onNodeWithText("Stop alarm")
            .assertIsDisplayed()
            .assertHasClickAction()

        assertTrue(
            "First Move must remain before the Snooze/Stop safety footer in the wake flow",
            firstMove.bottom <= snooze.top,
        )
    }

    @Test
    fun consumerNavigationExposesClickableTabsAndSelectedDestination() {
        composeRule.setContent {
            WakeMyWayTheme {
                WmwConsumerScaffold(
                    selectedTab = ConsumerTab.INSIGHTS,
                    onTabSelected = {},
                ) { }
            }
        }

        composeRule.onNodeWithText("Home").assertHasClickAction()
        composeRule.onNodeWithText("Alarms").assertHasClickAction()
        composeRule.onNodeWithText("Insights")
            .assertHasClickAction()
            .assertIsSelected()
        composeRule.onNodeWithText("Profile").assertHasClickAction()
    }

    @Test
    fun onboardingKeepsSkipAndPrimaryActionAccessible() {
        composeRule.setContent {
            WakeMyWayTheme {
                OnboardingScreen(
                    onComplete = {},
                    onSkip = {},
                )
            }
        }

        composeRule.onNodeWithText("Skip")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("Continue")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun alarmListKeepsAddAndAlarmSpecificEnableControlAccessible() {
        val alarm = AlarmDefinition(
            id = AlarmDefinitionId("accessibility-morning"),
            label = "Morning focus",
            enabled = true,
            zoneId = ZoneId.of("Europe/Berlin"),
            schedule = AlarmSchedulePattern.Weekly(
                days = setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                ),
                time = LocalTime.of(7, 30),
            ),
            soundId = WakeSoundId.MORNING_LIGHT,
            voiceCheckInEnabled = true,
            voiceStyle = VoiceStyle.DEFAULT,
            snoozePolicy = SnoozePolicy(
                enabled = true,
                duration = Duration.ofMinutes(5),
            ),
            tomorrowContractMode = TomorrowContractMode.OPTIONAL,
            revision = 1,
            createdAt = Instant.parse("2026-09-24T05:00:00Z"),
            updatedAt = Instant.parse("2026-09-24T05:00:00Z"),
        )

        composeRule.setContent {
            WakeMyWayTheme {
                AlarmsScreen(
                    alarms = listOf(alarm),
                    healthFor = { null },
                    onAddAlarm = {},
                    onEditAlarm = {},
                    onSetEnabled = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Add alarm")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithContentDescription("Enable Morning focus alarm")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
