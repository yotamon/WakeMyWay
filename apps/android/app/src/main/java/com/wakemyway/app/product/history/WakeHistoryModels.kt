package com.wakemyway.app.product.history

import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

enum class WakeHistoryTerminalReason {
    /** WakeRuntime reached its authored completion path and durable Stop succeeded. */
    COMPLETED,

    /** The user explicitly stopped the active wake. */
    STOPPED,

    /** The active wake was durably replaced by a Snooze occurrence. */
    SNOOZED,

    /** Reserved for a future explicit unrecoverable terminal path. */
    UNRECOVERABLE,
}

enum class WakeHistoryBehaviorTimingOrigin {
    /**
     * Compatibility marker for behavior written before the timing origin became part of history.
     * Product metrics must not mix these samples with a defined timing population.
     */
    LEGACY_UNSPECIFIED,

    /** Duration zero is the interactive WakeRuntime start immediately before AlarmFired. */
    INTERACTIVE_RUNTIME_START,
}

/**
 * Private local record of facts observed during one physical wake occurrence.
 *
 * This is normal credential-protected product history, never Direct-Boot or Alarm Kernel authority.
 * Behavioral evidence is nullable because alarm-only wakes and sessions interrupted before runtime
 * observation must remain unknown rather than being mislabeled as failure or success.
 *
 * New records also retain their scheduled local date/time and zone. They are nullable only for
 * entries migrated from older schemas so travelling later cannot silently relabel historic mornings.
 */
data class WakeHistoryEntry(
    val sessionId: WakeSessionId,
    val occurrenceId: WakeOccurrenceId,
    val scheduleId: WakeScheduleId,
    val occurrenceKind: WakeOccurrenceKind,
    val scheduleRevision: Long,
    val scheduledAt: Instant,
    val scheduledLocalDateTime: LocalDateTime? = null,
    val scheduledZoneId: ZoneId? = null,
    val startedAt: Instant,
    val finishedAt: Instant,
    val terminalReason: WakeHistoryTerminalReason,
    val replacementOccurrenceId: WakeOccurrenceId? = null,
    val behavior: WakeBehaviorObservation? = null,
    val behaviorTimingOrigin: WakeHistoryBehaviorTimingOrigin? = behavior?.let {
        WakeHistoryBehaviorTimingOrigin.LEGACY_UNSPECIFIED
    },
) {
    init {
        require(scheduleRevision > 0) { "Wake history schedule revision must be positive" }
        require(!finishedAt.isBefore(startedAt)) {
            "Wake history cannot finish before it started"
        }
        require((scheduledLocalDateTime == null) == (scheduledZoneId == null)) {
            "Wake history local schedule time and zone must be present together"
        }
        when (terminalReason) {
            WakeHistoryTerminalReason.SNOOZED -> require(replacementOccurrenceId != null) {
                "A Snoozed Wake history entry requires its replacement occurrence id"
            }
            else -> require(replacementOccurrenceId == null) {
                "Only a Snoozed Wake history entry may carry a replacement occurrence id"
            }
        }
        require((behavior == null) == (behaviorTimingOrigin == null)) {
            "Wake history behavior and its timing origin must be present together"
        }
    }
}
