package com.wakemyway.core.schedule

import java.time.Duration
import java.time.Instant

/** Creates a durable replacement occurrence for an intentional snooze. */
class SnoozeOccurrenceFactory {
    fun create(
        schedule: WakeSchedule,
        now: Instant,
        duration: Duration,
    ): WakeOccurrence {
        require(!duration.isNegative && !duration.isZero) { "Snooze duration must be positive" }

        val scheduledAt = now.plus(duration).atZone(schedule.zoneId)
        return WakeOccurrence(
            id = WakeOccurrenceId(
                "${schedule.id.value}:snooze:${schedule.revision}:${scheduledAt.toInstant().epochSecond}",
            ),
            wakeScheduleId = schedule.id,
            kind = WakeOccurrenceKind.SNOOZE,
            scheduledLocalDateTime = scheduledAt.toLocalDateTime(),
            scheduledAt = scheduledAt,
            scheduleRevision = schedule.revision,
            localTimeResolution = LocalTimeResolution.EXACT,
        )
    }
}
