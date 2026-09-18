package com.wakemyway.app.product.followup

import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeCalibrationOutcome
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeSafetyCheckTest {
    @Test
    fun `only an uncalibrated explicit Stop needs a safety check`() {
        val stopped = entry(WakeHistoryTerminalReason.STOPPED)

        assertTrue(stopped.needsMorningSafetyCheck())
        assertFalse(entry(WakeHistoryTerminalReason.COMPLETED).needsMorningSafetyCheck())
        assertFalse(entry(WakeHistoryTerminalReason.SNOOZED, replacement = "replacement").needsMorningSafetyCheck())
        assertFalse(
            stopped.copy(
                calibration = WakeCalibration(WakeCalibrationOutcome.GOT_UP),
            ).needsMorningSafetyCheck(),
        )
    }

    @Test
    fun `unique work name is stable per occurrence`() {
        val occurrenceId = WakeOccurrenceId("morning-1")

        assertTrue(
            WakeSafetyCheckScheduler.workName(occurrenceId)
                .endsWith(occurrenceId.value),
        )
    }

    private fun entry(
        reason: WakeHistoryTerminalReason,
        replacement: String? = null,
    ) = WakeHistoryEntry(
        sessionId = WakeSessionId("session"),
        occurrenceId = WakeOccurrenceId("occurrence"),
        scheduleId = WakeScheduleId("schedule"),
        occurrenceKind = WakeOccurrenceKind.PRIMARY,
        scheduleRevision = 1,
        scheduledAt = Instant.parse("2026-09-19T06:00:00Z"),
        startedAt = Instant.parse("2026-09-19T06:00:00Z"),
        finishedAt = Instant.parse("2026-09-19T06:01:00Z"),
        terminalReason = reason,
        replacementOccurrenceId = replacement?.let(::WakeOccurrenceId),
    )
}
