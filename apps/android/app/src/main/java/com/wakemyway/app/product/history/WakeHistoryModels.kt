package com.wakemyway.app.product.history

import com.wakemyway.core.learning.WakeBehaviorObservation
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Instant

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

/**
 * Private local record of facts observed during one physical wake occurrence.
 *
 * This is normal credential-protected product history, never Direct-Boot or Alarm Kernel authority.
 * Behavioral evidence is nullable because alarm-only wakes and sessions interrupted before runtime
 * observation must remain unknown rather than being mislabeled as failure or success.
 */
data class WakeHistoryEntry(
    val sessionId: WakeSessionId,
    val occurrenceId: WakeOccurrenceId,
    val scheduleId: WakeScheduleId,
    val occurrenceKind: WakeOccurrenceKind,
    val scheduleRevision: Long,
    val scheduledAt: Instant,
    val startedAt: Instant,
    val finishedAt: Instant,
    val terminalReason: WakeHistoryTerminalReason,
    val replacementOccurrenceId: WakeOccurrenceId? = null,
    val behavior: WakeBehaviorObservation? = null,
) {
    init {
        require(scheduleRevision > 0) { "Wake history schedule revision must be positive" }
        require(!finishedAt.isBefore(startedAt)) {
            "Wake history cannot finish before it started"
        }
        when (terminalReason) {
            WakeHistoryTerminalReason.SNOOZED -> require(replacementOccurrenceId != null) {
                "A Snoozed Wake history entry requires its replacement occurrence id"
            }
            else -> require(replacementOccurrenceId == null) {
                "Only a Snoozed Wake history entry may carry a replacement occurrence id"
            }
        }
    }
}
