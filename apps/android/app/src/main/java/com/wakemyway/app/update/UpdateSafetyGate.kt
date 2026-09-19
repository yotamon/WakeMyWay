package com.wakemyway.app.update

import java.time.Duration
import java.time.Instant

class UpdateSafetyGate(
    private val minimumWakeDistance: Duration = Duration.ofMinutes(90),
) {
    fun evaluate(
        snapshot: UpdateSafetySnapshot,
        now: Instant = Instant.now(),
    ): UpdateDeferralReason? {
        if (snapshot.activeWake) return UpdateDeferralReason.ACTIVE_WAKE

        val nextWakeAt = snapshot.nextWakeAt ?: return null
        if (nextWakeAt.isBefore(now)) return null

        return if (Duration.between(now, nextWakeAt) < minimumWakeDistance) {
            UpdateDeferralReason.UPCOMING_WAKE
        } else {
            null
        }
    }
}
