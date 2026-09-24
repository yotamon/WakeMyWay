package com.wakemyway.app.alarm

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmReconcileReceiverPolicyTest {
    @Test
    fun lockedBootCompletedAlwaysRunsBootRecovery() {
        assertTrue(
            shouldRunBootRecovery(
                action = Intent.ACTION_LOCKED_BOOT_COMPLETED,
                hasActiveOccurrence = true,
            ),
        )
    }

    @Test
    fun bootCompletedPreservesWakeThatBecameActiveAfterLockedBoot() {
        assertFalse(
            shouldRunBootRecovery(
                action = Intent.ACTION_BOOT_COMPLETED,
                hasActiveOccurrence = true,
            ),
        )
    }
    @Test
    fun bootCompletedStillRecoversWhenNoWakeIsActive() {
        assertTrue(
            shouldRunBootRecovery(
                action = Intent.ACTION_BOOT_COMPLETED,
                hasActiveOccurrence = false,
            ),
        )
    }

    @Test
    fun timeChangeIsNotBootRecovery() {
        assertFalse(
            shouldRunBootRecovery(
                action = Intent.ACTION_TIME_CHANGED,
                hasActiveOccurrence = false,
            ),
        )
    }
}
