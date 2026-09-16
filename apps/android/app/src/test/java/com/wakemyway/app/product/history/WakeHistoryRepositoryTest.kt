package com.wakemyway.app.product.history

import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.io.File
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WakeHistoryRepositoryTest {
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `history round trips behavioral evidence and snooze lineage`() {
        val repository = repository()
        val completed = entry(
            occurrence = "wake-1",
            finishedAt = "2026-09-16T06:02:00Z",
            behavior = WakeBehaviorObservation(
                policyVersion = 1,
                timeToFirstEngagement = Duration.ofSeconds(8),
                timeToMeaningfulMovement = Duration.ofSeconds(24),
                timeToActivationCompletion = Duration.ofSeconds(41),
                maxInterventionDepth = 1,
            ),
        )
        val snoozed = entry(
            occurrence = "wake-2",
            finishedAt = "2026-09-16T07:01:00Z",
            reason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "wake-2-snooze",
            behavior = null,
        )

        repository.record(completed)
        repository.record(snoozed)

        assertEquals(listOf(snoozed, completed), repository.list())
        assertNull(repository.list().first().behavior)
    }

    @Test
    fun `first durable record for an occurrence wins idempotently`() {
        val repository = repository()
        val first = entry(
            occurrence = "same",
            finishedAt = "2026-09-16T06:01:00Z",
            reason = WakeHistoryTerminalReason.STOPPED,
        )
        val conflictingDuplicate = first.copy(
            sessionId = WakeSessionId("different-session"),
            finishedAt = Instant.parse("2026-09-16T06:03:00Z"),
        )

        repository.record(first)
        repository.record(first)
        repository.record(conflictingDuplicate)

        assertEquals(listOf(first), repository.list())
    }

    @Test
    fun `corrupt non critical history fails open and next record repairs it`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText("{not-json", Charsets.UTF_8)
        val repository = WakeHistoryRepository(context, fileName)

        assertEquals(emptyList<WakeHistoryEntry>(), repository.list())

        val recovered = entry(
            occurrence = "recovered",
            finishedAt = "2026-09-16T06:04:00Z",
        )
        repository.record(recovered)

        assertEquals(listOf(recovered), repository.list())
    }

    @Test
    fun `history is bounded and retains newest terminal occurrences`() {
        val repository = WakeHistoryRepository(
            context = context,
            fileName = uniqueFileName(),
            maxEntries = 2,
        )
        val oldest = entry("old", "2026-09-16T06:01:00Z")
        val middle = entry("middle", "2026-09-16T06:02:00Z")
        val newest = entry("new", "2026-09-16T06:03:00Z")

        repository.record(middle)
        repository.record(oldest)
        repository.record(newest)

        assertEquals(listOf(newest, middle), repository.list())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `snoozed entry requires replacement occurrence`() {
        entry(
            occurrence = "wake",
            finishedAt = "2026-09-16T06:01:00Z",
            reason = WakeHistoryTerminalReason.SNOOZED,
        )
    }

    private fun repository(): WakeHistoryRepository = WakeHistoryRepository(
        context = context,
        fileName = uniqueFileName(),
    )

    private fun entry(
        occurrence: String,
        finishedAt: String,
        reason: WakeHistoryTerminalReason = WakeHistoryTerminalReason.COMPLETED,
        replacement: String? = null,
        behavior: WakeBehaviorObservation? = null,
    ): WakeHistoryEntry = WakeHistoryEntry(
        sessionId = WakeSessionId("wake-$occurrence"),
        occurrenceId = WakeOccurrenceId(occurrence),
        scheduleId = WakeScheduleId("schedule"),
        occurrenceKind = if (occurrence.contains("snooze")) {
            WakeOccurrenceKind.SNOOZE
        } else {
            WakeOccurrenceKind.PRIMARY
        },
        scheduleRevision = 3,
        scheduledAt = Instant.parse("2026-09-16T06:00:00Z"),
        startedAt = Instant.parse("2026-09-16T06:00:05Z"),
        finishedAt = Instant.parse(finishedAt),
        terminalReason = reason,
        replacementOccurrenceId = replacement?.let(::WakeOccurrenceId),
        behavior = behavior,
    )

    private fun uniqueFileName(): String = "wake-history-${System.nanoTime()}.json"
}
