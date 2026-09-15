package com.wakemyway.app.alarm

import com.wakemyway.core.schedule.LocalTimeResolution
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPresentationCapabilitiesTest {
    @Test
    fun readyRequiresEveryControllabilityCapability() {
        assertTrue(
            AlarmPresentationCapabilities(
                notificationsAllowed = true,
                highImportanceChannel = true,
                fullScreenIntentAllowed = true,
            ).ready,
        )

        assertFalse(
            AlarmPresentationCapabilities(
                notificationsAllowed = false,
                highImportanceChannel = true,
                fullScreenIntentAllowed = true,
            ).ready,
        )
        assertFalse(
            AlarmPresentationCapabilities(
                notificationsAllowed = true,
                highImportanceChannel = false,
                fullScreenIntentAllowed = true,
            ).ready,
        )
        assertFalse(
            AlarmPresentationCapabilities(
                notificationsAllowed = true,
                highImportanceChannel = true,
                fullScreenIntentAllowed = false,
            ).ready,
        )
    }

    @Test
    fun futureRepairTargetUsesOneStableCriticalPriority() {
        assertEquals(
            AlarmRepairTarget.EXACT_ALARM,
            health(exact = false, notifications = false, channel = false, fullScreen = false)
                .futureSchedulingRepairTarget(),
        )
        assertEquals(
            AlarmRepairTarget.NOTIFICATIONS,
            health(notifications = false, channel = false, fullScreen = false).repairTarget(),
        )
        assertEquals(
            AlarmRepairTarget.ACTIVE_WAKE_CHANNEL,
            health(channel = false, fullScreen = false).repairTarget(),
        )
        assertEquals(
            AlarmRepairTarget.FULL_SCREEN_INTENT,
            health(fullScreen = false).repairTarget(),
        )
        assertEquals(AlarmRepairTarget.NONE, health().repairTarget())
    }

    @Test
    fun activeRepairTargetIgnoresFutureExactAlarmCapability() {
        assertEquals(
            AlarmRepairTarget.NONE,
            health(exact = false, active = true).repairTarget(),
        )
        assertEquals(
            AlarmRepairTarget.NOTIFICATIONS,
            health(exact = false, notifications = false, active = true).repairTarget(),
        )
        assertEquals(
            AlarmRepairTarget.ACTIVE_WAKE_CHANNEL,
            health(exact = false, channel = false, active = true).repairTarget(),
        )
        assertEquals(
            AlarmRepairTarget.FULL_SCREEN_INTENT,
            health(exact = false, fullScreen = false, active = true).repairTarget(),
        )
    }

    private fun health(
        exact: Boolean = true,
        notifications: Boolean = true,
        channel: Boolean = true,
        fullScreen: Boolean = true,
        active: Boolean = false,
    ) = AlarmHealth(
        ready = exact && notifications && channel && fullScreen,
        exactAlarmAllowed = exact,
        notificationsAllowed = notifications,
        notificationChannelHighImportance = channel,
        fullScreenIntentAllowed = fullScreen,
        nextOccurrence = null,
        activeOccurrence = if (active) activeOccurrence() else null,
        detail = "test",
    )

    private fun activeOccurrence(): WakeOccurrence {
        val local = LocalDateTime.of(2026, 9, 15, 7, 30)
        return WakeOccurrence(
            id = WakeOccurrenceId("active-test"),
            wakeScheduleId = WakeScheduleId("schedule-test"),
            kind = WakeOccurrenceKind.PRIMARY,
            scheduledLocalDateTime = local,
            scheduledAt = local.atZone(ZoneOffset.UTC),
            scheduleRevision = 1,
            localTimeResolution = LocalTimeResolution.EXACT,
        )
    }
}
