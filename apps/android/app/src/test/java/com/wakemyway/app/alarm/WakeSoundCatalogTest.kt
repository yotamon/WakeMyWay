package com.wakemyway.app.alarm

import com.wakemyway.core.alarm.WakeSoundId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeSoundCatalogTest {
    @Test
    fun `catalog exposes the three approved product sound ids`() {
        assertEquals(
            listOf(
                WakeSoundId.MORNING_LIGHT,
                WakeSoundId.SOFT_START,
                WakeSoundId.MORNING_PULSE,
            ),
            WakeSoundCatalog.consumerProfiles.map(WakeSoundProfile::id),
        )
        assertEquals("morning_light", WakeSoundCatalog.profile(WakeSoundId.MORNING_LIGHT)?.rawResourceName)
        assertEquals("soft_start", WakeSoundCatalog.profile(WakeSoundId.SOFT_START)?.rawResourceName)
        assertEquals("morning_pulse", WakeSoundCatalog.profile(WakeSoundId.MORNING_PULSE)?.rawResourceName)
    }

    @Test
    fun `bundled selected sound resolves without emergency fallback`() {
        val resolved = WakeSoundCatalog.resolve(
            id = WakeSoundId.SOFT_START,
            emergencyResourceId = 99,
            resourceLookup = { rawName -> if (rawName == "soft_start") 42 else 0 },
        )

        assertEquals(WakeSoundId.SOFT_START, resolved.requestedId)
        assertEquals(WakeSoundId.SOFT_START, resolved.profile?.id)
        assertEquals(42, resolved.rawResourceId)
        assertFalse(resolved.usedEmergencyFallback)
    }

    @Test
    fun `missing selected branded sound fails safe to emergency resource`() {
        val resolved = WakeSoundCatalog.resolve(
            id = WakeSoundId.MORNING_PULSE,
            emergencyResourceId = 99,
            resourceLookup = { 0 },
        )

        assertEquals(WakeSoundId.MORNING_PULSE, resolved.requestedId)
        assertEquals(WakeSoundId.MORNING_PULSE, resolved.profile?.id)
        assertEquals(99, resolved.rawResourceId)
        assertEquals(WakeSoundCatalog.emergencyPlaybackSpec, resolved.playback)
        assertTrue(resolved.usedEmergencyFallback)
    }

    @Test
    fun `unknown persisted sound id never aliases another branded profile`() {
        val unknown = WakeSoundId("future-custom-sound")
        val resolved = WakeSoundCatalog.resolve(
            id = unknown,
            emergencyResourceId = 99,
            resourceLookup = { 42 },
        )

        assertEquals(unknown, resolved.requestedId)
        assertNull(resolved.profile)
        assertEquals(99, resolved.rawResourceId)
        assertTrue(resolved.usedEmergencyFallback)
    }
}
