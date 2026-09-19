package com.wakemyway.app.update

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateSafetyGateTest {
    private val now = Instant.parse("2026-09-19T08:00:00Z")
    private val gate = UpdateSafetyGate()

    @Test
    fun activeWakeAlwaysDefersInstallation() {
        assertEquals(
            UpdateDeferralReason.ACTIVE_WAKE,
            gate.evaluate(
                UpdateSafetySnapshot(
                    activeWake = true,
                    nextWakeAt = now.plus(Duration.ofHours(4)),
                ),
                now,
            ),
        )
    }

    @Test
    fun wakeInsideNinetyMinutesDefersInstallation() {
        assertEquals(
            UpdateDeferralReason.UPCOMING_WAKE,
            gate.evaluate(
                UpdateSafetySnapshot(
                    activeWake = false,
                    nextWakeAt = now.plus(Duration.ofMinutes(89)),
                ),
                now,
            ),
        )
    }

    @Test
    fun exactlyNinetyMinutesIsSafe() {
        assertNull(
            gate.evaluate(
                UpdateSafetySnapshot(
                    activeWake = false,
                    nextWakeAt = now.plus(Duration.ofMinutes(90)),
                ),
                now,
            ),
        )
    }

    @Test
    fun pastWakeDoesNotBlockInstallation() {
        assertNull(
            gate.evaluate(
                UpdateSafetySnapshot(
                    activeWake = false,
                    nextWakeAt = now.minus(Duration.ofMinutes(1)),
                ),
                now,
            ),
        )
    }

    @Test
    fun noUpcomingWakeIsSafe() {
        assertNull(
            gate.evaluate(
                UpdateSafetySnapshot(activeWake = false, nextWakeAt = null),
                now,
            ),
        )
    }
}
