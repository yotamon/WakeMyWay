package com.wakemyway.app

import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.wakemyway.app.alarm.AlarmScheduleHealth
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.ui.alarms.AlarmEditorResult
import com.wakemyway.app.ui.alarms.AlarmEditorScreen
import com.wakemyway.app.ui.alarms.AlarmsScreen
import com.wakemyway.app.ui.home.TonightScreen
import com.wakemyway.app.ui.home.TonightUiState
import com.wakemyway.app.ui.preparation.TomorrowPlanScreen
import com.wakemyway.app.ui.setup.WakeScheduleMockupScreen
import com.wakemyway.app.ui.setup.WakeSetupCommitResult
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
import com.wakemyway.core.preparation.PreparedWakePlanPreparer
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.preparation.TomorrowContractId
import com.wakemyway.core.schedule.LocalTimeResolution
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
                        wakeTime = "07:30",
                        dateLabel = "Tuesday, 14 Jan",
                        hasOccurrence = true,
                        wakeReady = true,
                        readinessDetail = "Scheduled locally and ready for tomorrow.",
                        hasTomorrowContract = true,
                        tomorrowContractPrepared = true,
                        tomorrowContractText = "Design review at 10:00. You wanted time to shower and eat.",
                        firstMove = "Shower",
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
    fun alarmsOverview() {
        val ready = visualAlarm(
            id = "visual-weekday-ready",
            label = "Morning focus",
            time = LocalTime.of(7, 30),
            days = WEEKDAYS,
            voiceCheckInEnabled = true,
        )
        val attention = visualAlarm(
            id = "visual-gym-attention",
            label = "Gym morning",
            time = LocalTime.of(6, 45),
            days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            voiceCheckInEnabled = false,
        )
        val disabled = visualAlarm(
            id = "visual-weekend-off",
            label = "Weekend",
            time = LocalTime.of(8, 30),
            days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            enabled = false,
            voiceCheckInEnabled = true,
        )
        val health = mapOf(
            ready.id to visualHealth(ready, ready = true),
            attention.id to visualHealth(attention, ready = false),
        )

        captureRoboImage("alarms_overview.png") {
            WakeMyWayTheme {
                AlarmsScreen(
                    alarms = listOf(ready, attention, disabled),
                    healthFor = { alarm -> health[alarm.id] },
                    onAddAlarm = {},
                    onEditAlarm = {},
                    onSetEnabled = { _, _ -> },
                )
            }
        }
    }

    @Test
    fun alarmEditorConfigured() {
        val alarm = visualAlarm(
            id = "visual-editor",
            label = "Morning focus",
            time = LocalTime.of(7, 30),
            days = WEEKDAYS,
            voiceCheckInEnabled = true,
            voiceStyle = VoiceStyle.MOTIVATIONAL,
            snoozeMinutes = 10,
            contractMode = TomorrowContractMode.ALWAYS_PROMPT,
            firstMove = "Open the curtains",
        )

        captureRoboImage("alarm_editor_configured.png") {
            WakeMyWayTheme {
                AlarmEditorScreen(
                    existing = alarm,
                    onBack = {},
                    onSave = { AlarmEditorResult(saved = true) },
                    onDelete = { AlarmEditorResult(saved = true) },
                )
            }
        }
    }

    @Test
    fun wakeSetupWeekly() {
        captureRoboImage("wake_setup_weekly.png") {
            WakeMyWayTheme {
                WakeScheduleMockupScreen(
                    existingSchedule = WakeSchedule(
                        id = WakeScheduleId("visual-weekly"),
                        zoneId = ZoneId.of("Europe/Berlin"),
                        timesByDay = listOf(
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                        ).associateWith { LocalTime.of(7, 30) },
                        revision = 2,
                        completionPolicy = WakeCompletionPolicy.RECURRING,
                    ),
                    onBack = {},
                    onCommit = { WakeSetupCommitResult(true) },
                    onDisable = { WakeSetupCommitResult(true) },
                )
            }
        }
    }

    @Test
    fun tomorrowContract() {
        val occurrence = visualOccurrence()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WakePreparationManager(context).apply {
            clear()
            saveAndPrepare(
                wakeOccurrenceId = occurrence.id,
                rawText = "Design review at 10. I want to be prepared, showered and have a calm breakfast.",
                firstMove = "Shower",
            )
        }
        captureRoboImage("tomorrow_contract.png") {
            WakeMyWayTheme {
                TomorrowPlanScreen(
                    wakeOccurrence = occurrence,
                    onBack = {},
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
                    displayTime = "07:30",
                    displayDate = "Tuesday · 14 Jan",
                )
            }
        }
    }

    @Test
    fun wakeListening() {
        captureRoboImage("wake_listening.png") {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = null,
                    onSnooze = {},
                    onStop = {},
                    displayTime = "07:30",
                    displayDate = "Tuesday · 14 Jan",
                    voiceState = WakeVoiceUiState(
                        mode = WakeVoiceMode.LISTENING,
                        spokenLine = "Morning, Yotam.",
                        speechAvailable = true,
                        voiceInputAvailable = true,
                    ),
                )
            }
        }
    }

    @Test
    fun wakeMoving() {
        captureRoboImage("wake_moving.png") {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = null,
                    onSnooze = {},
                    onStop = {},
                    displayTime = "07:31",
                    displayDate = "Tuesday · 14 Jan",
                    voiceState = WakeVoiceUiState(
                        mode = WakeVoiceMode.MOVING,
                        spokenLine = "Feet on the floor.",
                        speechAvailable = true,
                        voiceInputAvailable = true,
                    ),
                )
            }
        }
    }

    @Test
    fun wakeOriented() {
        captureRoboImage("wake_oriented.png") {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = visualPreparedPlan(),
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

    private fun visualAlarm(
        id: String,
        label: String,
        time: LocalTime,
        days: Set<DayOfWeek>,
        enabled: Boolean = true,
        voiceCheckInEnabled: Boolean,
        voiceStyle: VoiceStyle = VoiceStyle.DEFAULT,
        snoozeMinutes: Long = 5,
        contractMode: TomorrowContractMode = TomorrowContractMode.OPTIONAL,
        firstMove: String? = null,
    ) = AlarmDefinition(
        id = AlarmDefinitionId(id),
        label = label,
        enabled = enabled,
        zoneId = ZoneId.of("Europe/Berlin"),
        schedule = AlarmSchedulePattern.Weekly(days = days, time = time),
        soundId = WakeSoundId.MORNING_LIGHT,
        voiceCheckInEnabled = voiceCheckInEnabled,
        voiceStyle = voiceStyle,
        snoozePolicy = SnoozePolicy(
            enabled = true,
            duration = Duration.ofMinutes(snoozeMinutes),
        ),
        tomorrowContractMode = contractMode,
        firstMoveDefault = firstMove,
        revision = 3,
        createdAt = VISUAL_INSTANT,
        updatedAt = VISUAL_INSTANT,
    )

    private fun visualHealth(
        alarm: AlarmDefinition,
        ready: Boolean,
    ) = AlarmScheduleHealth(
        scheduleId = WakeScheduleId(alarm.id.value),
        enabled = alarm.enabled,
        ready = ready,
        nextOccurrence = null,
        activeOccurrence = null,
    )

    private fun visualPreparedPlan() = PreparedWakePlanPreparer.prepare(
        contract = TomorrowContract(
            id = TomorrowContractId("visual-contract"),
            wakeOccurrenceId = WakeOccurrenceId("visual-wake"),
            rawText = "Design review at 10:00.",
            firstMove = "Shower",
            createdAtEpochMillis = 1L,
        ),
        preparedAtEpochMillis = 2L,
    )

    private fun visualOccurrence(): WakeOccurrence {
        val zone = ZoneId.of("Europe/Berlin")
        val local = LocalDateTime.of(2026, 9, 15, 7, 30)
        return WakeOccurrence(
            id = WakeOccurrenceId("visual-contract-wake"),
            wakeScheduleId = WakeScheduleId("visual-contract-schedule"),
            kind = WakeOccurrenceKind.PRIMARY,
            scheduledLocalDateTime = local,
            scheduledAt = ZonedDateTime.of(local, zone),
            scheduleRevision = 1L,
            localTimeResolution = LocalTimeResolution.EXACT,
        )
    }

    companion object {
        private val VISUAL_INSTANT: Instant = Instant.parse("2026-09-15T05:30:00Z")
        private val WEEKDAYS = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
    }
}
