package com.wakemyway.app.product.insights

import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToLong

enum class WakeInsightsPeriod(val label: String, internal val window: Duration?) {
    LAST_7_DAYS("7 days", Duration.ofDays(7)),
    LAST_30_DAYS("30 days", Duration.ofDays(30)),
    ALL("All", null),
}

data class WakeMorningInsight(
    val primaryOccurrenceId: WakeOccurrenceId,
    val scheduleId: WakeScheduleId,
    val scheduledAt: Instant,
    val scheduledLocalDateTime: LocalDateTime?,
    val scheduledZoneId: ZoneId?,
    val finalReason: WakeHistoryTerminalReason,
    val snoozeCount: Int,
    val physicalWakeCount: Int,
) {
    val activationCompleted: Boolean
        get() = finalReason == WakeHistoryTerminalReason.COMPLETED

    val usedSnooze: Boolean
        get() = snoozeCount > 0
}

data class WakeInsightsSummary(
    val period: WakeInsightsPeriod,
    val mornings: List<WakeMorningInsight>,
    val completedMorningCount: Int,
    val snoozedMorningCount: Int,
    val comparableBehaviorSessionCount: Int,
    val averageFirstEngagement: Duration?,
    val firstEngagementSampleCount: Int,
    val averageActivationCompletion: Duration?,
    val activationCompletionSampleCount: Int,
    val escalatedBehaviorSessionCount: Int,
) {
    val totalMorningCount: Int
        get() = mornings.size
}

/**
 * Derives consumer-facing Insights from immutable local Wake history.
 *
 * Morning outcomes and behavior timings deliberately use different populations:
 * - a morning is one Primary occurrence plus its Snooze replacement chain
 * - a behavior timing is one physical interactive WakeRuntime session whose timing origin is known
 *
 * This prevents a Snooze chain from inflating the morning count and prevents session-relative clocks
 * from being combined into a fabricated end-to-end morning duration.
 */
object WakeInsightsProjector {
    fun project(
        entries: List<WakeHistoryEntry>,
        period: WakeInsightsPeriod,
        now: Instant,
    ): WakeInsightsSummary {
        val byId = entries.associateBy { it.occurrenceId }
        val chains = entries
            .asSequence()
            .filter { it.occurrenceKind == com.wakemyway.core.schedule.WakeOccurrenceKind.PRIMARY }
            .map { primary -> buildChain(primary, byId) }
            .filter { chain -> period.includes(chain.first().scheduledAt, now) }
            .sortedByDescending { chain -> chain.first().scheduledAt }
            .toList()

        val mornings = chains.map { chain ->
            val primary = chain.first()
            WakeMorningInsight(
                primaryOccurrenceId = primary.occurrenceId,
                scheduleId = primary.scheduleId,
                scheduledAt = primary.scheduledAt,
                scheduledLocalDateTime = primary.scheduledLocalDateTime,
                scheduledZoneId = primary.scheduledZoneId,
                finalReason = chain.last().terminalReason,
                snoozeCount = chain.count { it.terminalReason == WakeHistoryTerminalReason.SNOOZED },
                physicalWakeCount = chain.size,
            )
        }

        val comparableBehavior = chains
            .asSequence()
            .flatten()
            .filter { entry ->
                entry.behavior != null &&
                    entry.behaviorTimingOrigin == WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START
            }
            .map { requireNotNull(it.behavior) }
            .toList()

        val firstEngagement = comparableBehavior.mapNotNull { it.timeToFirstEngagement }
        val activationCompletion = comparableBehavior.mapNotNull { it.timeToActivationCompletion }

        return WakeInsightsSummary(
            period = period,
            mornings = mornings,
            completedMorningCount = mornings.count { it.activationCompleted },
            snoozedMorningCount = mornings.count { it.usedSnooze },
            comparableBehaviorSessionCount = comparableBehavior.size,
            averageFirstEngagement = firstEngagement.averageDurationOrNull(),
            firstEngagementSampleCount = firstEngagement.size,
            averageActivationCompletion = activationCompletion.averageDurationOrNull(),
            activationCompletionSampleCount = activationCompletion.size,
            escalatedBehaviorSessionCount = comparableBehavior.count { it.maxInterventionDepth > 0 },
        )
    }

    private fun buildChain(
        primary: WakeHistoryEntry,
        byId: Map<WakeOccurrenceId, WakeHistoryEntry>,
    ): List<WakeHistoryEntry> = buildList {
        var current: WakeHistoryEntry? = primary
        val visited = mutableSetOf<WakeOccurrenceId>()
        while (current != null && visited.add(current.occurrenceId)) {
            add(current)
            current = current.replacementOccurrenceId?.let(byId::get)
        }
    }

    private fun WakeInsightsPeriod.includes(
        scheduledAt: Instant,
        now: Instant,
    ): Boolean {
        if (scheduledAt.isAfter(now)) return false
        val cutoff = window?.let(now::minus) ?: return true
        return !scheduledAt.isBefore(cutoff)
    }

    private fun List<Duration>.averageDurationOrNull(): Duration? {
        if (isEmpty()) return null
        val averageMillis = map(Duration::toMillis).average().roundToLong()
        return Duration.ofMillis(averageMillis.coerceAtLeast(0L))
    }
}
