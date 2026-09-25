package com.wakemyway.app.widget

import android.content.Context
import android.text.format.DateFormat
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakePreparationStatus
import com.wakemyway.app.product.AlarmDefinitionRepository
import com.wakemyway.app.product.AppAppearance
import com.wakemyway.app.product.ConsumerPreferencesRepository
import com.wakemyway.app.product.followup.needsMorningSafetyCheck
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class WakeWidgetReadiness {
    READY,
    NEEDS_ATTENTION,
    NONE,
    UNAVAILABLE,
}

enum class WakeWidgetPlanState {
    READY,
    AVAILABLE,
    NONE,
}

enum class WakeWidgetPrimaryAction {
    OPEN_HOME,
    CREATE_ALARM,
    FIX_WAKE,
    PREPARE_TOMORROW,
    MORNING_CHECK_IN,
    OPEN_WAKE,
}

data class WakeWidgetAlarmSummary(
    val id: String,
    val time: String,
    val schedule: String,
    val enabled: Boolean,
    val ready: Boolean,
)

data class WakeWidgetSnapshot(
    val time: String,
    val dateLabel: String,
    val readiness: WakeWidgetReadiness,
    val planState: WakeWidgetPlanState,
    val primaryAction: WakeWidgetPrimaryAction,
    val pendingMorningCheckInOccurrenceId: String?,
    val upcomingAlarms: List<WakeWidgetAlarmSummary>,
    val appearance: AppAppearance,
    val activeWake: Boolean,
) {
    companion object {
        fun unavailable(appearance: AppAppearance = AppAppearance.DAYLIGHT) = WakeWidgetSnapshot(
            time = "--:--",
            dateLabel = "Open WakeMyWay to refresh",
            readiness = WakeWidgetReadiness.UNAVAILABLE,
            planState = WakeWidgetPlanState.NONE,
            primaryAction = WakeWidgetPrimaryAction.OPEN_HOME,
            pendingMorningCheckInOccurrenceId = null,
            upcomingAlarms = emptyList(),
            appearance = appearance,
            activeWake = false,
        )

        fun preview() = WakeWidgetSnapshot(
            time = "07:30",
            dateLabel = "Sat, 26 Sep",
            readiness = WakeWidgetReadiness.READY,
            planState = WakeWidgetPlanState.READY,
            primaryAction = WakeWidgetPrimaryAction.OPEN_HOME,
            pendingMorningCheckInOccurrenceId = null,
            upcomingAlarms = listOf(
                WakeWidgetAlarmSummary(
                    id = "preview-weekdays",
                    time = "08:00",
                    schedule = "Weekdays",
                    enabled = true,
                    ready = true,
                ),
                WakeWidgetAlarmSummary(
                    id = "preview-weekend",
                    time = "09:30",
                    schedule = "Weekends",
                    enabled = false,
                    ready = false,
                ),
            ),
            appearance = AppAppearance.WARM_SUNRISE,
            activeWake = false,
        )
    }
}

/**
 * Privacy-minimized, read-only projection for the launcher surface.
 *
 * The projector never persists scheduling state and never infers Wake Ready independently from the
 * Alarm Kernel. Any private Tomorrow Contract text is reduced to a state bit before it reaches the
 * widget model.
 */
class WakeWidgetSnapshotProjector(
    context: Context,
    private val localeProvider: () -> Locale = Locale::getDefault,
) {
    private val appContext = context.applicationContext
    private val alarmKernel = AlarmKernel(appContext)
    private val alarmRepository = AlarmDefinitionRepository(appContext)
    private val preparationManager = WakePreparationManager(appContext)
    private val preferencesRepository = ConsumerPreferencesRepository(appContext)
    private val historyRepository = WakeHistoryRepository(appContext)

    fun project(): WakeWidgetSnapshot {
        val appearance = runCatching { preferencesRepository.get().appearance }
            .getOrDefault(AppAppearance.DAYLIGHT)
        return runCatching { projectTrusted(appearance) }
            .getOrElse { WakeWidgetSnapshot.unavailable(appearance) }
    }

    private fun projectTrusted(appearance: AppAppearance): WakeWidgetSnapshot {
        val health = alarmKernel.health()
        val alarms = alarmRepository.list()
            .sortedWith(
                compareByDescending<AlarmDefinition> { it.enabled }
                    .thenBy { alarm ->
                        alarmKernel.health(WakeScheduleId(alarm.id.value))
                            ?.nextOccurrence
                            ?.scheduledAt
                            ?.toInstant()
                            ?: Instant.MAX
                    },
            )
        val active = health.activeOccurrence
        val next = health.nextOccurrence
        val nextAlarm = next?.let { occurrence ->
            alarms.firstOrNull { it.id.value == occurrence.wakeScheduleId.value }
                ?: alarmRepository.get(AlarmDefinitionId(occurrence.wakeScheduleId.value))
        }
        val nextReady = next?.let { occurrence ->
            alarmKernel.health(occurrence.wakeScheduleId)?.ready
        } ?: false
        val preparationStatus = if (next != null) {
            runCatching { preparationManager.snapshotFor(next.id).status }.getOrNull()
        } else {
            null
        }
        val planState = when {
            next == null ||
                nextAlarm == null ||
                nextAlarm.tomorrowContractMode == TomorrowContractMode.DISABLED ->
                WakeWidgetPlanState.NONE
            preparationStatus == WakePreparationStatus.READY -> WakeWidgetPlanState.READY
            else -> WakeWidgetPlanState.AVAILABLE
        }
        val pendingMorningCheckIn = historyRepository.list()
            .firstOrNull { it.needsMorningSafetyCheck() }
            ?.occurrenceId
            ?.value

        val locale = localeProvider()
        val timeFormatter = DateTimeFormatter.ofPattern(
            if (DateFormat.is24HourFormat(appContext)) "HH:mm" else "h:mm a",
            locale,
        )
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", locale)

        val primaryAction = selectWakeWidgetPrimaryAction(
            activeWake = active != null,
            hasNextWake = next != null,
            nextWakeReady = nextReady,
            pendingMorningCheckIn = pendingMorningCheckIn != null,
            planState = planState,
        )

        return WakeWidgetSnapshot(
            time = (active ?: next)?.scheduledAt?.format(timeFormatter) ?: "--:--",
            dateLabel = (active ?: next)?.scheduledAt?.format(dateFormatter)
                ?: "No wake scheduled",
            readiness = when {
                active != null -> WakeWidgetReadiness.READY
                next == null -> WakeWidgetReadiness.NONE
                nextReady -> WakeWidgetReadiness.READY
                else -> WakeWidgetReadiness.NEEDS_ATTENTION
            },
            planState = planState,
            primaryAction = primaryAction,
            pendingMorningCheckInOccurrenceId = pendingMorningCheckIn,
            upcomingAlarms = alarms.take(MAX_UPCOMING_ALARMS).map { alarm ->
                alarm.toWidgetSummary(locale, timeFormatter)
            },
            appearance = appearance,
            activeWake = active != null,
        )
    }

    private fun AlarmDefinition.toWidgetSummary(
        locale: Locale,
        timeFormatter: DateTimeFormatter,
    ): WakeWidgetAlarmSummary =
        WakeWidgetAlarmSummary(
            id = id.value,
            time = schedule.time.format(timeFormatter),
            schedule = schedule.widgetScheduleLabel(locale),
            enabled = enabled,
            ready = enabled && alarmKernel.health(WakeScheduleId(id.value))?.ready == true,
        )

    private fun AlarmSchedulePattern.widgetScheduleLabel(locale: Locale): String = when (this) {
        is AlarmSchedulePattern.OneShot ->
            date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))

        is AlarmSchedulePattern.Weekly -> {
            val weekdays = setOf(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY,
            )
            when (days) {
                java.time.DayOfWeek.entries.toSet() -> "Every day"
                weekdays -> "Weekdays"
                setOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY) -> "Weekends"
                else -> days
                    .sortedBy(java.time.DayOfWeek::getValue)
                    .joinToString(" · ") { day ->
                        day.getDisplayName(TextStyle.SHORT, locale)
                    }
            }
        }
    }

    private companion object {
        const val MAX_UPCOMING_ALARMS = 2
    }
}


internal fun selectWakeWidgetPrimaryAction(
    activeWake: Boolean,
    hasNextWake: Boolean,
    nextWakeReady: Boolean,
    pendingMorningCheckIn: Boolean,
    planState: WakeWidgetPlanState,
): WakeWidgetPrimaryAction = when {
    activeWake -> WakeWidgetPrimaryAction.OPEN_WAKE
    hasNextWake && !nextWakeReady -> WakeWidgetPrimaryAction.FIX_WAKE
    pendingMorningCheckIn -> WakeWidgetPrimaryAction.MORNING_CHECK_IN
    !hasNextWake -> WakeWidgetPrimaryAction.CREATE_ALARM
    planState == WakeWidgetPlanState.AVAILABLE -> WakeWidgetPrimaryAction.PREPARE_TOMORROW
    else -> WakeWidgetPrimaryAction.OPEN_HOME
}
