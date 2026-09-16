package com.wakemyway.app.product.insights

import com.wakemyway.app.product.history.WakeHistoryBehaviorTimingOrigin
import com.wakemyway.app.product.history.WakeHistoryEntry
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Explicit time window for an Insights projection.
 *
 * Wake chains are selected by the root PRIMARY occurrence's scheduled instant. Once selected, the
 * whole retained chain stays together even when a Snooze replacement crosses the period boundary.
 */
data class WakeInsightsPeriod(
    val startInclusive: Instant? = null,
    val endExclusive: Instant? = null,
) {
    init {
        require(startInclusive == null || endExclusive == null || startInclusive < endExclusive) {
            "Wake Insights period start must be before its end"
        }
    }

    fun contains(instant: Instant): Boolean =
        (startInclusive == null || instant >= startInclusive) &&
            (endExclusive == null || instant < endExclusive)

    companion object {
        val ALL = WakeInsightsPeriod()
    }
}

data class CountRatio(
    val numerator: Int,
    val denominator: Int,
) {
    init {
        require(numerator >= 0) { "Count ratio numerator must be non-negative" }
        require(denominator >= 0) { "Count ratio denominator must be non-negative" }
        require(numerator <= denominator) { "Count ratio numerator cannot exceed denominator" }
    }

    val fraction: Double?
        get() = denominator.takeIf { it > 0 }?.let { numerator.toDouble() / it }
}

data class SampledDurationMetric(
    val average: Duration,
    val median: Duration,
    val sampleCount: Int,
) {
    init {
        require(sampleCount > 0) { "A sampled duration metric requires observations" }
        require(!average.isNegative && !median.isNegative) {
            "Wake Insights duration metrics must be non-negative"
        }
    }
}

data class SampledIntMetric(
    val average: Double,
    val median: Double,
    val sampleCount: Int,
) {
    init {
        require(sampleCount > 0) { "A sampled integer metric requires observations" }
        require(average >= 0.0 && median >= 0.0) {
            "Wake Insights integer metrics must be non-negative"
        }
    }
}

data class WakeChainSummary(
    val rootOccurrenceId: WakeOccurrenceId,
    val scheduledAt: Instant,
    val finishedAt: Instant,
    val occurrenceCount: Int,
    val snoozeCount: Int,
    val terminalReason: WakeHistoryTerminalReason,
) {
    init {
        require(occurrenceCount > 0) { "A Wake chain requires at least one occurrence" }
        require(snoozeCount >= 0) { "Wake chain Snooze count must be non-negative" }
        require(terminalReason != WakeHistoryTerminalReason.SNOOZED) {
            "A complete Wake chain cannot terminate in Snoozed"
        }
    }
}

data class WakeInsightsEvidenceCoverage(
    /** Complete-chain sessions with compact behavior evidence. */
    val behaviorSessions: Int,
    /** Behavior sessions whose duration origin is safe for current timing aggregation. */
    val interactiveTimingSessions: Int,
    /** Preserved schema-v1 timing sessions excluded from duration aggregation. */
    val legacyTimingSessions: Int,
    /** Complete-chain sessions with no behavioral evidence at all. */
    val unknownBehaviorSessions: Int,
    /** PRIMARY-rooted chains in the selected period whose retained Snooze lineage is incomplete. */
    val incompleteRootedChains: Int,
    /** Retained Snooze occurrences with no retained parent, so no period root can be established. */
    val unanchoredSnoozeOccurrences: Int,
) {
    init {
        listOf(
            behaviorSessions,
            interactiveTimingSessions,
            legacyTimingSessions,
            unknownBehaviorSessions,
            incompleteRootedChains,
            unanchoredSnoozeOccurrences,
        ).forEach { require(it >= 0) { "Wake Insights coverage counts must be non-negative" } }
    }
}

data class WakeInsightsSnapshot(
    val period: WakeInsightsPeriod,
    val eligibleWakeChains: Int,
    val snoozedWakeChains: CountRatio,
    val snoozeActionCount: Int,
    val timeToFirstEngagement: SampledDurationMetric?,
    val timeToMeaningfulMovement: SampledDurationMetric?,
    val timeToActivationCompletion: SampledDurationMetric?,
    val interventionDepth: SampledIntMetric?,
    val recentChains: List<WakeChainSummary>,
    val evidenceCoverage: WakeInsightsEvidenceCoverage,
) {
    init {
        require(eligibleWakeChains >= 0) { "Eligible Wake chain count must be non-negative" }
        require(snoozedWakeChains.denominator == eligibleWakeChains) {
            "Snooze ratio denominator must equal eligible Wake chains"
        }
        require(snoozeActionCount >= 0) { "Snooze action count must be non-negative" }
    }
}

/**
 * Projects retained local Wake history into descriptive, sample-counted product facts.
 *
 * This class intentionally does not derive Wake Success, sleep quality, target-window streaks, or
 * any other score whose semantics are not directly evidenced by [WakeHistoryEntry].
 */
class WakeInsightsProjector(
    private val recentChainLimit: Int = DEFAULT_RECENT_CHAIN_LIMIT,
) {
    init {
        require(recentChainLimit > 0) { "Recent Wake chain limit must be positive" }
    }

    fun project(
        history: List<WakeHistoryEntry>,
        period: WakeInsightsPeriod = WakeInsightsPeriod.ALL,
    ): WakeInsightsSnapshot {
        require(history.map { it.occurrenceId }.distinct().size == history.size) {
            "Wake Insights requires unique occurrence ids"
        }

        val byId = history.associateBy { it.occurrenceId }
        val parentsByChild = buildMap<WakeOccurrenceId, MutableList<WakeOccurrenceId>> {
            history.forEach { entry ->
                entry.replacementOccurrenceId?.let { childId ->
                    getOrPut(childId) { mutableListOf() }.add(entry.occurrenceId)
                }
            }
        }
        val rootCandidates = history.filter { entry ->
            parentsByChild[entry.occurrenceId].isNullOrEmpty()
        }
        val unanchoredSnoozeOccurrences = rootCandidates.count {
            it.occurrenceKind == WakeOccurrenceKind.SNOOZE
        }

        val selectedPrimaryRoots = rootCandidates
            .filter { it.occurrenceKind == WakeOccurrenceKind.PRIMARY }
            .filter { period.contains(it.scheduledAt) }

        var incompleteRootedChains = 0
        val completeChains = buildList {
            selectedPrimaryRoots.forEach { root ->
                when (val resolved = resolveChain(root, byId, parentsByChild)) {
                    is ChainResolution.Complete -> add(resolved.entries)
                    ChainResolution.Incomplete -> incompleteRootedChains += 1
                }
            }
        }

        val chainSummaries = completeChains.map(::summarizeChain)
            .sortedByDescending(WakeChainSummary::scheduledAt)
        val snoozedChains = chainSummaries.count { it.snoozeCount > 0 }
        val snoozeActions = chainSummaries.sumOf(WakeChainSummary::snoozeCount)

        val sessions = completeChains.flatten()
        val behaviorSessions = sessions.filter { it.behavior != null }
        val interactiveTimingSessions = behaviorSessions.filter {
            it.behaviorTimingOrigin == WakeHistoryBehaviorTimingOrigin.INTERACTIVE_RUNTIME_START
        }
        val legacyTimingSessions = behaviorSessions.count {
            it.behaviorTimingOrigin == WakeHistoryBehaviorTimingOrigin.LEGACY_UNSPECIFIED
        }
        val unknownBehaviorSessions = sessions.count { it.behavior == null }

        return WakeInsightsSnapshot(
            period = period,
            eligibleWakeChains = completeChains.size,
            snoozedWakeChains = CountRatio(
                numerator = snoozedChains,
                denominator = completeChains.size,
            ),
            snoozeActionCount = snoozeActions,
            timeToFirstEngagement = durationMetric(
                interactiveTimingSessions.mapNotNull { it.behavior?.timeToFirstEngagement },
            ),
            timeToMeaningfulMovement = durationMetric(
                interactiveTimingSessions.mapNotNull { it.behavior?.timeToMeaningfulMovement },
            ),
            timeToActivationCompletion = durationMetric(
                interactiveTimingSessions.mapNotNull { it.behavior?.timeToActivationCompletion },
            ),
            interventionDepth = intMetric(
                behaviorSessions.map { requireNotNull(it.behavior).maxInterventionDepth },
            ),
            recentChains = chainSummaries.take(recentChainLimit),
            evidenceCoverage = WakeInsightsEvidenceCoverage(
                behaviorSessions = behaviorSessions.size,
                interactiveTimingSessions = interactiveTimingSessions.size,
                legacyTimingSessions = legacyTimingSessions,
                unknownBehaviorSessions = unknownBehaviorSessions,
                incompleteRootedChains = incompleteRootedChains,
                unanchoredSnoozeOccurrences = unanchoredSnoozeOccurrences,
            ),
        )
    }

    private fun resolveChain(
        root: WakeHistoryEntry,
        byId: Map<WakeOccurrenceId, WakeHistoryEntry>,
        parentsByChild: Map<WakeOccurrenceId, List<WakeOccurrenceId>>,
    ): ChainResolution {
        val entries = mutableListOf<WakeHistoryEntry>()
        val visited = mutableSetOf<WakeOccurrenceId>()
        var current = root

        while (true) {
            if (!visited.add(current.occurrenceId)) return ChainResolution.Incomplete
            entries += current

            if (current.terminalReason != WakeHistoryTerminalReason.SNOOZED) {
                return ChainResolution.Complete(entries)
            }

            val replacementId = current.replacementOccurrenceId ?: return ChainResolution.Incomplete
            val replacement = byId[replacementId] ?: return ChainResolution.Incomplete
            val retainedParents = parentsByChild[replacementId].orEmpty()
            if (retainedParents.size != 1 || retainedParents.single() != current.occurrenceId) {
                return ChainResolution.Incomplete
            }
            if (replacement.occurrenceKind != WakeOccurrenceKind.SNOOZE) {
                return ChainResolution.Incomplete
            }
            current = replacement
        }
    }

    private fun summarizeChain(entries: List<WakeHistoryEntry>): WakeChainSummary {
        val root = entries.first()
        val terminal = entries.last()
        return WakeChainSummary(
            rootOccurrenceId = root.occurrenceId,
            scheduledAt = root.scheduledAt,
            finishedAt = terminal.finishedAt,
            occurrenceCount = entries.size,
            snoozeCount = entries.count { it.terminalReason == WakeHistoryTerminalReason.SNOOZED },
            terminalReason = terminal.terminalReason,
        )
    }

    private fun durationMetric(samples: List<Duration>): SampledDurationMetric? {
        if (samples.isEmpty()) return null
        val millis = samples.map(Duration::toMillis).sorted()
        return SampledDurationMetric(
            average = Duration.ofMillis(millis.average().roundToLong()),
            median = Duration.ofMillis(medianOfSortedLongs(millis)),
            sampleCount = millis.size,
        )
    }

    private fun intMetric(samples: List<Int>): SampledIntMetric? {
        if (samples.isEmpty()) return null
        val sorted = samples.sorted()
        return SampledIntMetric(
            average = samples.average(),
            median = medianOfSortedInts(sorted),
            sampleCount = samples.size,
        )
    }

    private fun medianOfSortedLongs(values: List<Long>): Long {
        val middle = values.size / 2
        return if (values.size % 2 == 1) {
            values[middle]
        } else {
            val lower = values[middle - 1]
            val upper = values[middle]
            lower + ((upper - lower) / 2L)
        }
    }

    private fun medianOfSortedInts(values: List<Int>): Double {
        val middle = values.size / 2
        return if (values.size % 2 == 1) {
            values[middle].toDouble()
        } else {
            (values[middle - 1].toDouble() + values[middle].toDouble()) / 2.0
        }
    }

    private sealed interface ChainResolution {
        data class Complete(val entries: List<WakeHistoryEntry>) : ChainResolution
        data object Incomplete : ChainResolution
    }

    companion object {
        const val DEFAULT_RECENT_CHAIN_LIMIT = 7
    }
}
