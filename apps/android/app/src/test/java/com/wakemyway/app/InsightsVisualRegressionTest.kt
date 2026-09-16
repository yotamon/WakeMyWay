package com.wakemyway.app

import com.github.takahirom.roborazzi.captureRoboImage
import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.app.product.insights.WakeInsightsPeriod
import com.wakemyway.app.product.insights.WakeInsightsProjector
import com.wakemyway.app.ui.insights.InsightsScreen
import com.wakemyway.app.ui.navigation.ConsumerTab
import com.wakemyway.app.ui.navigation.WmwConsumerScaffold
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "en-rUS-w393dp-h852dp-night-mdpi",
)
class InsightsVisualRegressionTest {
    @Test
    fun insightsPopulated() {
        val now = Instant.parse("2026-09-16T10:00:00Z")
        val summary = WakeInsightsProjector.project(
            entries = visualHistory(now),
            period = WakeInsightsPeriod.LAST_7_DAYS,
            now = now,
        )

        captureRoboImage("insights_populated.png") {
            WakeMyWayTheme {
                WmwConsumerScaffold(
                    selectedTab = ConsumerTab.INSIGHTS,
                    onTabSelected = {},
                ) { contentModifier ->
                    InsightsScreen(
                        summary = summary,
                        onPeriodSelected = {},
                        modifier = contentModifier,
                    )
                }
            }
        }
    }

    @Test
    fun insightsEmpty() {
        val summary = WakeInsightsProjector.project(
            entries = emptyList(),
            period = WakeInsightsPeriod.LAST_7_DAYS,
            now = Instant.parse("2026-09-16T10:00:00Z"),
        )

        captureRoboImage("insights_empty.png") {
            WakeMyWayTheme {
                WmwConsumerScaffold(
                    selectedTab = ConsumerTab.INSIGHTS,
                    onTabSelected = {},
                ) { contentModifier ->
                    InsightsScreen(
                        summary = summary,
                        onPeriodSelected = {},
                        modifier = contentModifier,
                    )
                }
            }
        }
    }

    private fun visualHistory(now: Instant): List<WakeHistoryEntry> {
        val zone = ZoneId.of("Europe/Berlin")
        val day1 = primary(
            id = "day-1",
            scheduledAt = now.minus(Duration.ofDays(1)),
            local = LocalDateTime.of(2026, 9, 15, 7, 30),
            zone = zone,
            reason = WakeHistoryTerminalReason.COMPLETED,
            behavior = behavior(7, 36, 0),
        )
        val day2 = primary(
            id = "day-2",
            scheduledAt = now.minus(Duration.ofDays(2)),
            local = LocalDateTime.of(2026, 9, 14, 7, 30),
            zone = zone,
            reason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "day-2-snooze",
            behavior = behavior(13, null, 1),
        )
        val day2Snooze = entry(
            id = "day-2-snooze",
            kind = WakeOccurrenceKind.SNOOZE,
            scheduledAt = day2.scheduledAt.plus(Duration.ofMinutes(10)),
            local = LocalDateTime.of(2026, 9, 14, 7, 40),
            zone = zone,
            reason = WakeHistoryTerminalReason.COMPLETED,
            behavior = behavior(5, 31, 0),
        )
        val day3 = primary(
            id = "day-3",
            scheduledAt = now.minus(Duration.ofDays(3)),
            local = LocalDateTime.of(2026, 9, 13, 8, 0),
            zone = zone,
            reason = WakeHistoryTerminalReason.STOPPED,
            behavior = behavior(18, null, 2),
        )
        val day4 = primary(
            id = "day-4",
            scheduledAt = now.minus(Duration.ofDays(4)),
            local = LocalDateTime.of(2026, 9, 12, 7, 30),
            zone = zone,
            reason = WakeHistoryTerminalReason.COMPLETED,
            behavior = behavior(9, 43, 0),
        )
        val day5 = primary(
            id = "day-5",
            scheduledAt = now.minus(Duration.ofDays(5)),
            local = LocalDateTime.of(2026, 9, 11, 7, 30),
            zone = zone,
            reason = WakeHistoryTerminalReason.SNOOZED,
            replacement = "day-5-snooze",
            behavior = behavior(15, null, 1),
        )
        val day5Snooze = entry(
            id = "day-5-snooze",
            kind = WakeOccurrenceKind.SNOOZE,
            scheduledAt = day5.scheduledAt.plus(Duration.ofMinutes(15)),
            local = LocalDateTime.of(2026, 9, 11, 7, 45),
            zone = zone,
            reason = WakeHistoryTerminalReason.STOPPED,
            behavior = behavior(8, null, 1),
        )
        return listOf(day1, day2, day2Snooze, day3, day4, day5, day5Snooze)
    }

    private fun primary(
        id: String,
        scheduledAt: Instant,
        local: LocalDateTime,
        zone: ZoneId,
        reason: WakeHistoryTerminalReason,
        replacement: String? = null,
        behavior: WakeBehaviorObservation? = null,
    ) = entry(
        id = id,
        kind = WakeOccurrenceKind.PRIMARY,
        scheduledAt = scheduledAt,
        local = local,
        zone = zone,
        reason = reason,
        replacement = replacement,
        behavior = behavior,
    )

    private fun entry(
        id: String,
        kind: WakeOccurrenceKind,
        scheduledAt: Instant,
        local: LocalDateTime,
        zone: ZoneId,
        reason: WakeHistoryTerminalReason,
        replacement: String? = null,
        behavior: WakeBehaviorObservation? = null,
    ) = WakeHistoryEntry(
        sessionId = WakeSessionId("visual-$id"),
        occurrenceId = WakeOccurrenceId(id),
        scheduleId = WakeScheduleId("visual-morning"),
        occurrenceKind = kind,
        scheduleRevision = 1,
        scheduledAt = scheduledAt,
        scheduledLocalDateTime = local,
        scheduledZoneId = zone,
        startedAt = scheduledAt.plusSeconds(2),
        finishedAt = scheduledAt.plusSeconds(90),
        terminalReason = reason,
        replacementOccurrenceId = replacement?.let(::WakeOccurrenceId),
        behavior = behavior,
        behaviorTimingOrigin = behavior?.let {
            WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START
        },
    )

    private fun behavior(
        firstEngagementSeconds: Long,
        activationSeconds: Long?,
        interventionDepth: Int,
    ) = WakeBehaviorObservation(
        policyVersion = 1,
        timeToFirstEngagement = Duration.ofSeconds(firstEngagementSeconds),
        timeToMeaningfulMovement = activationSeconds?.let { Duration.ofSeconds(it / 2) },
        timeToActivationCompletion = activationSeconds?.let { Duration.ofSeconds(it) },
        maxInterventionDepth = interventionDepth,
    )
}
