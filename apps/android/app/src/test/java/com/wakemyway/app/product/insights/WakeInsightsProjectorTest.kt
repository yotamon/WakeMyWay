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
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeInsightsProjectorTest {
    private val now = Instant.parse("2026-09-16T10:00:00Z")
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun `multi snooze chain remains one morning with final completion`() {
        val primary = entry(
            id = "primary",
            scheduledAt = now.minus(Duration.ofHours(3)),
            reason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "snooze-1",
            kind = WakeOccurrenceKind.PRIMARY,
        )
        val snooze1 = entry(
            id = "snooze-1",
            scheduledAt = primary.scheduledAt.plus(Duration.ofMinutes(10)),
            reason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "snooze-2",
            kind = WakeOccurrenceKind.SNOOZE,
        )
        val snooze2 = entry(
            id = "snooze-2",
            scheduledAt = primary.scheduledAt.plus(Duration.ofMinutes(20)),
            reason = WakeHistoryTerminalReason.COMPLETED,
            kind = WakeOccurrenceKind.SNOOZE,
        )

        val summary = WakeInsightsProjector.project(
            entries = listOf(snooze1, primary, snooze2),
            period = WakeInsightsPeriod.LAST_7_DAYS,
            now = now,
        )

        assertEquals(1, summary.totalMorningCount)
        assertEquals(1, summary.completedMorningCount)
        assertEquals(1, summary.snoozedMorningCount)
        assertEquals(2, summary.mornings.single().snoozeCount)
        assertEquals(3, summary.mornings.single().physicalWakeCount)
        assertTrue(summary.mornings.single().activationCompleted)
    }

    @Test
    fun `timing aggregates include only comparable interactive runtime samples`() {
        val comparable = behavior(
            firstEngagement = Duration.ofSeconds(8),
            activationCompletion = Duration.ofSeconds(40),
            interventionDepth = 1,
        )
        val anotherComparable = behavior(
            firstEngagement = Duration.ofSeconds(12),
            activationCompletion = null,
            interventionDepth = 0,
        )
        val legacy = behavior(
            firstEngagement = Duration.ofSeconds(1),
            activationCompletion = Duration.ofSeconds(3),
            interventionDepth = 4,
        )
        val primary = entry(
            id = "primary",
            scheduledAt = now.minus(Duration.ofHours(2)),
            reason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "snooze",
            kind = WakeOccurrenceKind.PRIMARY,
            behavior = comparable,
        )
        val snooze = entry(
            id = "snooze",
            scheduledAt = primary.scheduledAt.plus(Duration.ofMinutes(10)),
            reason = WakeHistoryTerminalReason.COMPLETED,
            kind = WakeOccurrenceKind.SNOOZE,
            behavior = anotherComparable,
        )
        val oldPrimary = entry(
            id = "legacy",
            scheduledAt = now.minus(Duration.ofHours(5)),
            reason = WakeHistoryTerminalReason.STOPPED,
            kind = WakeOccurrenceKind.PRIMARY,
            behavior = legacy,
            timingOrigin = WakeHistoryBehaviorTimingOrigin.LEGACY_UNSPECIFIED,
        )

        val summary = WakeInsightsProjector.project(
            entries = listOf(primary, snooze, oldPrimary),
            period = WakeInsightsPeriod.LAST_7_DAYS,
            now = now,
        )

        assertEquals(2, summary.comparableBehaviorSessionCount)
        assertEquals(2, summary.firstEngagementSampleCount)
        assertEquals(Duration.ofSeconds(10), summary.averageFirstEngagement)
        assertEquals(1, summary.activationCompletionSampleCount)
        assertEquals(Duration.ofSeconds(40), summary.averageActivationCompletion)
        assertEquals(1, summary.escalatedBehaviorSessionCount)
    }

    @Test
    fun `period is anchored to primary morning and keeps its snooze chain`() {
        val insidePrimary = entry(
            id = "inside",
            scheduledAt = now.minus(Duration.ofDays(6)),
            reason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "inside-snooze",
            kind = WakeOccurrenceKind.PRIMARY,
        )
        val insideSnooze = entry(
            id = "inside-snooze",
            scheduledAt = insidePrimary.scheduledAt.plus(Duration.ofMinutes(15)),
            reason = WakeHistoryTerminalReason.COMPLETED,
            kind = WakeOccurrenceKind.SNOOZE,
        )
        val outside = entry(
            id = "outside",
            scheduledAt = now.minus(Duration.ofDays(8)),
            reason = WakeHistoryTerminalReason.COMPLETED,
            kind = WakeOccurrenceKind.PRIMARY,
        )

        val summary = WakeInsightsProjector.project(
            entries = listOf(outside, insideSnooze, insidePrimary),
            period = WakeInsightsPeriod.LAST_7_DAYS,
            now = now,
        )

        assertEquals(listOf(WakeOccurrenceId("inside")), summary.mornings.map { it.primaryOccurrenceId })
        assertEquals(2, summary.mornings.single().physicalWakeCount)
    }

    @Test
    fun `orphan snooze never creates a fake morning`() {
        val orphan = entry(
            id = "orphan",
            scheduledAt = now.minus(Duration.ofHours(1)),
            reason = WakeHistoryTerminalReason.COMPLETED,
            kind = WakeOccurrenceKind.SNOOZE,
        )

        val summary = WakeInsightsProjector.project(
            entries = listOf(orphan),
            period = WakeInsightsPeriod.ALL,
            now = now,
        )

        assertEquals(0, summary.totalMorningCount)
        assertEquals(0, summary.comparableBehaviorSessionCount)
    }

    @Test
    fun `local schedule metadata is carried to morning insight`() {
        val local = LocalDateTime.of(2026, 9, 16, 7, 30)
        val primary = entry(
            id = "local",
            scheduledAt = now.minus(Duration.ofHours(4)),
            reason = WakeHistoryTerminalReason.STOPPED,
            kind = WakeOccurrenceKind.PRIMARY,
            scheduledLocalDateTime = local,
            zoneId = berlin,
        )

        val morning = WakeInsightsProjector.project(
            entries = listOf(primary),
            period = WakeInsightsPeriod.ALL,
            now = now,
        ).mornings.single()

        assertEquals(local, morning.scheduledLocalDateTime)
        assertEquals(berlin, morning.scheduledZoneId)
        assertFalse(morning.usedSnooze)
        assertNull(WakeInsightsProjector.project(emptyList(), WakeInsightsPeriod.ALL, now).averageFirstEngagement)
    }

    private fun entry(
        id: String,
        scheduledAt: Instant,
        reason: WakeHistoryTerminalReason,
        kind: WakeOccurrenceKind,
        replacement: String? = null,
        behavior: WakeBehaviorObservation? = null,
        timingOrigin: WakeHistoryBehaviorTimingOrigin? = behavior?.let {
            WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START
        },
        scheduledLocalDateTime: LocalDateTime? = LocalDateTime.of(2026, 9, 16, 7, 30),
        zoneId: ZoneId? = berlin,
    ) = WakeHistoryEntry(
        sessionId = WakeSessionId("session-$id"),
        occurrenceId = WakeOccurrenceId(id),
        scheduleId = WakeScheduleId("schedule"),
        occurrenceKind = kind,
        scheduleRevision = 1,
        scheduledAt = scheduledAt,
        scheduledLocalDateTime = scheduledLocalDateTime,
        scheduledZoneId = zoneId,
        startedAt = scheduledAt.plusSeconds(1),
        finishedAt = scheduledAt.plusSeconds(60),
        terminalReason = reason,
        replacementOccurrenceId = replacement?.let(::WakeOccurrenceId),
        behavior = behavior,
        behaviorTimingOrigin = timingOrigin,
    )

    private fun behavior(
        firstEngagement: Duration?,
        activationCompletion: Duration?,
        interventionDepth: Int,
    ) = WakeBehaviorObservation(
        policyVersion = 1,
        timeToFirstEngagement = firstEngagement,
        timeToMeaningfulMovement = null,
        timeToActivationCompletion = activationCompletion,
        maxInterventionDepth = interventionDepth,
    )
}
