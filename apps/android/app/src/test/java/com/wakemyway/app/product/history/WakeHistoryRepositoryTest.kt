package com.wakemyway.app.product.history

import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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
    fun `history round trips behavioral evidence snooze lineage and local schedule`() {
        val repository = repository()
        val localTime = LocalDateTime.of(2026, 9, 16, 8, 0)
        val zone = ZoneId.of("Europe/Berlin")
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
            behaviorTimingOrigin = WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
            scheduledLocalDateTime = localTime,
            scheduledZoneId = zone,
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
        assertEquals(localTime, repository.list()[1].scheduledLocalDateTime)
        assertEquals(zone, repository.list()[1].scheduledZoneId)
    }

    @Test
    fun `schema v1 behavior is preserved with legacy unspecified timing origin`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText(
            """
            {
              "schemaVersion": 1,
              "entries": [
                {
                  "sessionId": "wake-legacy",
                  "occurrenceId": "legacy",
                  "scheduleId": "schedule",
                  "occurrenceKind": "PRIMARY",
                  "scheduleRevision": 2,
                  "scheduledAt": "2026-09-16T06:00:00Z",
                  "startedAt": "2026-09-16T06:00:05Z",
                  "finishedAt": "2026-09-16T06:01:00Z",
                  "terminalReason": "COMPLETED",
                  "behavior": {
                    "policyVersion": 1,
                    "firstEngagementMillis": 6000,
                    "activationCompletionMillis": 28000,
                    "maxInterventionDepth": 1
                  }
                }
              ]
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val entry = WakeHistoryRepository(context, fileName).list().single()

        assertEquals(
            WakeHistoryBehaviorTimingOrigin.LEGACY_UNSPECIFIED,
            entry.behaviorTimingOrigin,
        )
        assertEquals(Duration.ofSeconds(6), entry.behavior?.timeToFirstEngagement)
        assertEquals(Duration.ofSeconds(28), entry.behavior?.timeToActivationCompletion)
        assertNull(entry.scheduledLocalDateTime)
        assertNull(entry.scheduledZoneId)
    }

    @Test
    fun `schema v2 remains readable with local schedule unknown`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText(schemaV2Entry(), Charsets.UTF_8)

        val entry = WakeHistoryRepository(context, fileName).list().single()

        assertEquals(
            WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
            entry.behaviorTimingOrigin,
        )
        assertNull(entry.scheduledLocalDateTime)
        assertNull(entry.scheduledZoneId)
    }

    @Test
    fun `legacy entry survives append and schema v3 rewrite`() {
        val fileName = uniqueFileName()
        File(context.filesDir, fileName).writeText(schemaV2Entry(), Charsets.UTF_8)
        val repository = WakeHistoryRepository(context, fileName)
        val local = LocalDateTime.of(2026, 9, 17, 7, 30)
        val zone = ZoneId.of("Europe/Berlin")
        val fresh = entry(
            occurrence = "fresh",
            finishedAt = "2026-09-17T05:32:00Z",
            scheduledLocalDateTime = local,
            scheduledZoneId = zone,
        )

        assertEquals(1, repository.list().size)
        repository.record(fresh)
        val reread = WakeHistoryRepository(context, fileName).list()

        assertEquals(2, reread.size)
        val legacy = reread.single { it.occurrenceId == WakeOccurrenceId("v2") }
        val newEntry = reread.single { it.occurrenceId == WakeOccurrenceId("fresh") }
        assertNull(legacy.scheduledLocalDateTime)
        assertNull(legacy.scheduledZoneId)
        assertEquals(local, newEntry.scheduledLocalDateTime)
        assertEquals(zone, newEntry.scheduledZoneId)
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

    private fun schemaV2Entry(): String = """
        {
          "schemaVersion": 2,
          "entries": [
            {
              "sessionId": "wake-v2",
              "occurrenceId": "v2",
              "scheduleId": "schedule",
              "occurrenceKind": "PRIMARY",
              "scheduleRevision": 2,
              "scheduledAt": "2026-09-16T06:00:00Z",
              "startedAt": "2026-09-16T06:00:05Z",
              "finishedAt": "2026-09-16T06:01:00Z",
              "terminalReason": "COMPLETED",
              "behaviorTimingOrigin": "INTERACTIVE_RUNTIME_START",
              "behavior": {
                "policyVersion": 1,
                "firstEngagementMillis": 5000,
                "maxInterventionDepth": 0
              }
            }
          ]
        }
    """.trimIndent()

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
        behaviorTimingOrigin: WakeHistoryBehaviorTimingOrigin? = behavior?.let {
            WakeHistoryBehaviorTimingOrigin.LEGACY_UNSPECIFIED
        },
        scheduledLocalDateTime: LocalDateTime? = null,
        scheduledZoneId: ZoneId? = null,
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
        scheduledLocalDateTime = scheduledLocalDateTime,
        scheduledZoneId = scheduledZoneId,
        startedAt = Instant.parse("2026-09-16T06:00:05Z"),
        finishedAt = Instant.parse(finishedAt),
        terminalReason = reason,
        replacementOccurrenceId = replacement?.let(::WakeOccurrenceId),
        behavior = behavior,
        behaviorTimingOrigin = behaviorTimingOrigin,
    )

    private fun uniqueFileName(): String = "wake-history-${System.nanoTime()}.json"
}
