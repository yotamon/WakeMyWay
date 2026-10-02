package com.wakemyway.app.alarm

import com.wakemyway.core.schedule.WakeScheduleId
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmReadinessProjectionTest {
    @Test
    fun `disabled alarm is off even when platform capabilities are missing`() {
        val projection = projectAlarmReadiness(
            enabled = false,
            scheduleHealth = null,
            systemHealth = health(exact = false, notifications = false),
        )

        assertEquals(AlarmReadinessState.OFF, projection.state)
        assertEquals(AlarmRepairTarget.NONE, projection.repairTarget)
    }

    @Test
    fun `ready schedule stays ready`() {
        val projection = projectAlarmReadiness(
            enabled = true,
            scheduleHealth = AlarmScheduleHealth(
                scheduleId = WakeScheduleId("alarm"),
                enabled = true,
                ready = true,
                nextOccurrence = null,
                activeOccurrence = null,
            ),
            systemHealth = health(),
        )

        assertEquals(AlarmReadinessState.READY, projection.state)
        assertEquals(AlarmRepairTarget.NONE, projection.repairTarget)
    }

    @Test
    fun `blocked alarm uses the same platform blocker priority as wake ready`() {
        val projection = projectAlarmReadiness(
            enabled = true,
            scheduleHealth = null,
            systemHealth = health(exact = false, notifications = false),
        )

        assertEquals(AlarmReadinessState.NEEDS_ATTENTION, projection.state)
        assertEquals(AlarmRepairTarget.EXACT_ALARM, projection.repairTarget)
    }

    @Test
    fun `schedule drift still offers repair when platform capabilities are healthy`() {
        val projection = projectAlarmReadiness(
            enabled = true,
            scheduleHealth = null,
            systemHealth = health(),
        )

        assertEquals(AlarmReadinessState.NEEDS_ATTENTION, projection.state)
        assertEquals(AlarmRepairTarget.NONE, projection.repairTarget)
        assertEquals(true, projection.shouldOfferRepair())
    }

    private fun health(
        exact: Boolean = true,
        notifications: Boolean = true,
        channel: Boolean = true,
        fullScreen: Boolean = true,
    ) = AlarmHealth(
        ready = exact && notifications && channel && fullScreen,
        exactAlarmAllowed = exact,
        notificationsAllowed = notifications,
        notificationChannelHighImportance = channel,
        fullScreenIntentAllowed = fullScreen,
        nextOccurrence = null,
        activeOccurrence = null,
        detail = "",
    )
}
