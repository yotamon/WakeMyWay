package com.wakemyway.app.product.history

import com.wakemyway.app.alarm.WakeTerminalReason
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeInputId
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.LocalTimeResolution
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WakeHistorySessionRecorderTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `alarm only terminal keeps behavior unknown`() {
        val repository = repository()
        val occurrence = occurrence("primary")
        val recorder = WakeHistorySessionRecorder(
            occurrence,
            repository,
            Clock.fixed(Instant.parse("2026-09-16T06:01:00Z"), ZoneOffset.UTC),
        )

        recorder.onTerminal(occurrence, WakeTerminalReason.STOPPED)

        val entry = repository.list().single()
        assertEquals(WakeHistoryTerminalReason.STOPPED, entry.terminalReason)
        assertNull(entry.behavior)
    }

    @Test
    fun `snooze terminal stores replacement occurrence`() {
        val repository = repository()
        val occurrence = occurrence("primary")
        val replacement = occurrence("snooze", WakeOccurrenceKind.SNOOZE)
        val recorder = WakeHistorySessionRecorder(
            occurrence,
            repository,
            Clock.fixed(Instant.parse("2026-09-16T06:01:00Z"), ZoneOffset.UTC),
        )

        recorder.onTerminal(occurrence, WakeTerminalReason.SNOOZED, replacement)

        val entry = repository.list().single()
        assertEquals(WakeHistoryTerminalReason.SNOOZED, entry.terminalReason)
        assertEquals(replacement.id, entry.replacementOccurrenceId)
    }

    @Test
    fun `runtime observations are reduced to compact evidence`() {
        val repository = repository()
        val occurrence = occurrence("voice")
        val recorder = WakeHistorySessionRecorder(
            occurrence,
            repository,
            Clock.fixed(Instant.parse("2026-09-16T06:01:00Z"), ZoneOffset.UTC),
        )
        val runtime = WakeRuntime()
        val policy = WakePolicy()
        var snapshot = runtime.initial(WakeSessionId("test"), policy)
        val input = WakeInput.UserInteracted(WakeInputId("tap"))
        val transition = runtime.reduce(snapshot, input, policy)
        recorder.observeRuntimeTransition(snapshot, input, transition, Duration.ofSeconds(3))

        recorder.onTerminal(occurrence, WakeTerminalReason.COMPLETED)

        val behavior = requireNotNull(repository.list().single().behavior)
        assertEquals(Duration.ofSeconds(3), behavior.timeToFirstEngagement)
        assertEquals(policy.version, behavior.policyVersion)
    }

    private fun repository() = WakeHistoryRepository(
        context,
        "wake-history-recorder-${System.nanoTime()}.json",
    )

    private fun occurrence(
        suffix: String,
        kind: WakeOccurrenceKind = WakeOccurrenceKind.PRIMARY,
    ): WakeOccurrence {
        val local = LocalDateTime.of(2026, 9, 16, 8, 0)
        val zoned = ZonedDateTime.of(local, ZoneOffset.UTC)
        return WakeOccurrence(
            id = WakeOccurrenceId("occurrence-$suffix"),
            wakeScheduleId = WakeScheduleId("weekday"),
            kind = kind,
            scheduledLocalDateTime = local,
            scheduledAt = zoned,
            scheduleRevision = 2,
            localTimeResolution = LocalTimeResolution.EXACT,
        )
    }
}
