package com.wakemyway.app.product.insights

import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakeInsightsProjectionTest {
    private val projector = WakeInsightsProjector(recentChainLimit = 10)

    @Test
    fun `single completed primary is one eligible non snoozed Wake chain`() {
        val history = listOf(
            entry(
                id = "morning",
                scheduledAt = "2026-09-16T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.COMPLETED,
            ),
        )

        val snapshot = projector.project(history)

        assertEquals(1, snapshot.eligibleWakeChains)
        assertEquals(CountRatio(0, 1), snapshot.snoozedWakeChains)
        assertEquals(0, snapshot.snoozeActionCount)
        assertEquals(WakeHistoryTerminalReason.COMPLETED, snapshot.recentChains.single().terminalReason)
    }

    @Test
    fun `multiple Snooze replacements count as one snoozed morning and multiple actions`() {
        val history = listOf(
            entry(
                id = "root",
                scheduledAt = "2026-09-16T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.SNOOZED,
                replacement = "snooze-1",
            ),
            entry(
                id = "snooze-1",
                kind = WakeOccurrenceKind.SNOOZE,
                scheduledAt = "2026-09-16T06:05:00Z",
                terminalReason = WakeHistoryTerminalReason.SNOOZED,
                replacement = "snooze-2",
            ),
            entry(
                id = "snooze-2",
                kind = WakeOccurrenceKind.SNOOZE,
                scheduledAt = "2026-09-16T06:10:00Z",
                terminalReason = WakeHistoryTerminalReason.STOPPED,
            ),
        )

        val snapshot = projector.project(history)

        assertEquals(1, snapshot.eligibleWakeChains)
        assertEquals(CountRatio(1, 1), snapshot.snoozedWakeChains)
        assertEquals(2, snapshot.snoozeActionCount)
        assertEquals(3, snapshot.recentChains.single().occurrenceCount)
        assertEquals(2, snapshot.recentChains.single().snoozeCount)
        assertEquals(WakeHistoryTerminalReason.STOPPED, snapshot.recentChains.single().terminalReason)
    }

    @Test
    fun `primary with missing Snooze replacement is excluded from chain denominator`() {
        val history = listOf(
            entry(
                id = "root",
                scheduledAt = "2026-09-16T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.SNOOZED,
                replacement = "not-retained-yet",
            ),
        )

        val snapshot = projector.project(history)

        assertEquals(0, snapshot.eligibleWakeChains)
        assertEquals(CountRatio(0, 0), snapshot.snoozedWakeChains)
        assertEquals(1, snapshot.evidenceCoverage.incompleteRootedChains)
    }

    @Test
    fun `orphan retained Snooze is unanchored and never invented into a morning`() {
        val history = listOf(
            entry(
                id = "orphan-snooze",
                kind = WakeOccurrenceKind.SNOOZE,
                scheduledAt = "2026-09-16T06:05:00Z",
                terminalReason = WakeHistoryTerminalReason.STOPPED,
            ),
        )

        val snapshot = projector.project(history)

        assertEquals(0, snapshot.eligibleWakeChains)
        assertEquals(1, snapshot.evidenceCoverage.unanchoredSnoozeOccurrences)
        assertEquals(emptyList<WakeChainSummary>(), snapshot.recentChains)
    }

    @Test
    fun `period is anchored by root and keeps Snooze replacement outside boundary`() {
        val root = entry(
            id = "root",
            scheduledAt = "2026-09-15T23:58:00Z",
            terminalReason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "after-midnight",
        )
        val snooze = entry(
            id = "after-midnight",
            kind = WakeOccurrenceKind.SNOOZE,
            scheduledAt = "2026-09-16T00:03:00Z",
            terminalReason = WakeHistoryTerminalReason.COMPLETED,
        )
        val period = WakeInsightsPeriod(
            startInclusive = Instant.parse("2026-09-15T00:00:00Z"),
            endExclusive = Instant.parse("2026-09-16T00:00:00Z"),
        )

        val snapshot = projector.project(listOf(root, snooze), period)

        assertEquals(1, snapshot.eligibleWakeChains)
        assertEquals(2, snapshot.recentChains.single().occurrenceCount)
        assertEquals(CountRatio(1, 1), snapshot.snoozedWakeChains)
    }

    @Test
    fun `root outside period excludes its whole chain even when Snooze falls inside`() {
        val root = entry(
            id = "root",
            scheduledAt = "2026-09-15T23:58:00Z",
            terminalReason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "inside-period",
        )
        val snooze = entry(
            id = "inside-period",
            kind = WakeOccurrenceKind.SNOOZE,
            scheduledAt = "2026-09-16T00:03:00Z",
            terminalReason = WakeHistoryTerminalReason.COMPLETED,
        )
        val period = WakeInsightsPeriod(
            startInclusive = Instant.parse("2026-09-16T00:00:00Z"),
            endExclusive = Instant.parse("2026-09-17T00:00:00Z"),
        )

        val snapshot = projector.project(listOf(root, snooze), period)

        assertEquals(0, snapshot.eligibleWakeChains)
        assertEquals(emptyList<WakeChainSummary>(), snapshot.recentChains)
    }

    @Test
    fun `duration metrics use only explicit interactive timing origin and expose sample counts`() {
        val history = listOf(
            entry(
                id = "interactive-a",
                scheduledAt = "2026-09-14T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.COMPLETED,
                behavior = behavior(engagementSeconds = 4, activationSeconds = 20, depth = 0),
                timingOrigin = WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
            ),
            entry(
                id = "interactive-b",
                scheduledAt = "2026-09-15T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.STOPPED,
                behavior = behavior(engagementSeconds = 8, activationSeconds = 40, depth = 2),
                timingOrigin = WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
            ),
            entry(
                id = "legacy",
                scheduledAt = "2026-09-16T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.COMPLETED,
                behavior = behavior(engagementSeconds = 1, activationSeconds = 2, depth = 1),
                timingOrigin = WakeHistoryBehaviorTimingOrigin.LEGACY_UNSPECIFIED,
            ),
        )

        val snapshot = projector.project(history)

        assertEquals(
            SampledDurationMetric(
                average = Duration.ofSeconds(6),
                median = Duration.ofSeconds(6),
                sampleCount = 2,
            ),
            snapshot.timeToFirstEngagement,
        )
        assertEquals(
            SampledDurationMetric(
                average = Duration.ofSeconds(30),
                median = Duration.ofSeconds(30),
                sampleCount = 2,
            ),
            snapshot.timeToActivationCompletion,
        )
        assertEquals(3, snapshot.evidenceCoverage.behaviorSessions)
        assertEquals(2, snapshot.evidenceCoverage.interactiveTimingSessions)
        assertEquals(1, snapshot.evidenceCoverage.legacyTimingSessions)
        assertEquals(
            SampledIntMetric(average = 1.0, median = 1.0, sampleCount = 3),
            snapshot.interventionDepth,
        )
    }

    @Test
    fun `missing behavior is unknown and never converted to a zero timing`() {
        val history = listOf(
            entry(
                id = "known",
                scheduledAt = "2026-09-15T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.COMPLETED,
                behavior = behavior(engagementSeconds = 12, activationSeconds = null, depth = 1),
                timingOrigin = WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
            ),
            entry(
                id = "alarm-only",
                scheduledAt = "2026-09-16T06:00:00Z",
                terminalReason = WakeHistoryTerminalReason.STOPPED,
            ),
        )

        val snapshot = projector.project(history)

        assertEquals(2, snapshot.eligibleWakeChains)
        assertEquals(1, snapshot.evidenceCoverage.unknownBehaviorSessions)
        assertEquals(Duration.ofSeconds(12), snapshot.timeToFirstEngagement?.average)
        assertEquals(1, snapshot.timeToFirstEngagement?.sampleCount)
        assertNull(snapshot.timeToActivationCompletion)
    }

    @Test
    fun `odd and even medians are deterministic`() {
        val history = listOf(
            metricEntry("a", "2026-09-13T06:00:00Z", 1),
            metricEntry("b", "2026-09-14T06:00:00Z", 5),
            metricEntry("c", "2026-09-15T06:00:00Z", 9),
            metricEntry("d", "2026-09-16T06:00:00Z", 13),
        )

        val snapshot = projector.project(history)

        assertEquals(Duration.ofSeconds(7), snapshot.timeToFirstEngagement?.average)
        assertEquals(Duration.ofSeconds(7), snapshot.timeToFirstEngagement?.median)
        assertEquals(4, snapshot.timeToFirstEngagement?.sampleCount)
    }

    @Test
    fun `recent chains are ordered by root schedule and capped independently of input order`() {
        val projector = WakeInsightsProjector(recentChainLimit = 2)
        val history = listOf(
            entry("newest", "2026-09-16T06:00:00Z", WakeHistoryTerminalReason.STOPPED),
            entry("oldest", "2026-09-14T06:00:00Z", WakeHistoryTerminalReason.COMPLETED),
            entry("middle", "2026-09-15T06:00:00Z", WakeHistoryTerminalReason.COMPLETED),
        )

        val snapshot = projector.project(history)

        assertEquals(
            listOf(WakeOccurrenceId("newest"), WakeOccurrenceId("middle")),
            snapshot.recentChains.map(WakeChainSummary::rootOccurrenceId),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate occurrence ids are rejected rather than double counted`() {
        val entry = entry(
            id = "duplicate",
            scheduledAt = "2026-09-16T06:00:00Z",
            terminalReason = WakeHistoryTerminalReason.COMPLETED,
        )

        projector.project(listOf(entry, entry))
    }

    private fun metricEntry(id: String, scheduledAt: String, engagementSeconds: Long) = entry(
        id = id,
        scheduledAt = scheduledAt,
        terminalReason = WakeHistoryTerminalReason.COMPLETED,
        behavior = behavior(engagementSeconds, activationSeconds = null, depth = 0),
        timingOrigin = WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START,
    )

    private fun behavior(
        engagementSeconds: Long?,
        activationSeconds: Long?,
        depth: Int,
    ) = WakeBehaviorObservation(
        policyVersion = 1,
        timeToFirstEngagement = engagementSeconds?.let(Duration::ofSeconds),
        timeToMeaningfulMovement = null,
        timeToActivationCompletion = activationSeconds?.let(Duration::ofSeconds),
        maxInterventionDepth = depth,
    )

    private fun entry(
        id: String,
        scheduledAt: String,
        terminalReason: WakeHistoryTerminalReason,
        kind: WakeOccurrenceKind = WakeOccurrenceKind.PRIMARY,
        replacement: String? = null,
        behavior: WakeBehaviorObservation? = null,
        timingOrigin: WakeHistoryBehaviorTimingOrigin? = behavior?.let {
            WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START
        },
    ): WakeHistoryEntry {
        val scheduled = Instant.parse(scheduledAt)
        return WakeHistoryEntry(
            sessionId = WakeSessionId("session-$id"),
            occurrenceId = WakeOccurrenceId(id),
            scheduleId = WakeScheduleId("schedule"),
            occurrenceKind = kind,
            scheduleRevision = 1,
            scheduledAt = scheduled,
            startedAt = scheduled.plusSeconds(1),
            finishedAt = scheduled.plusSeconds(60),
            terminalReason = terminalReason,
            replacementOccurrenceId = replacement?.let(::WakeOccurrenceId),
            behavior = behavior,
            behaviorTimingOrigin = timingOrigin,
        )
    }
}
