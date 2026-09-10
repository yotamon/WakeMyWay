package com.wakemyway.app.alarm

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
    fun repairTargetUsesOneStableCriticalPriority() {
        assertEquals(
            AlarmRepairTarget.EXACT_ALARM,
            health(exact = false, notifications = false, channel = false, fullScreen = false).repairTarget(),
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
        detail = "test",
    )
}
