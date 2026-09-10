package com.wakemyway.app.alarm

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
}
