package com.wakemyway.core.schedule

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@JvmInline
value class WakeScheduleId(val value: String) {
    init {
        require(value.isNotBlank()) { "WakeScheduleId must not be blank" }
    }
}

@JvmInline
value class WakeOccurrenceId(val value: String) {
    init {
        require(value.isNotBlank()) { "WakeOccurrenceId must not be blank" }
    }
}

data class WakeSchedule(
    val id: WakeScheduleId,
    val zoneId: ZoneId,
    val timesByDay: Map<DayOfWeek, LocalTime>,
    val revision: Long = 1,
) {
    init {
        require(timesByDay.isNotEmpty()) { "A WakeSchedule needs at least one active day" }
        require(revision > 0) { "WakeSchedule revision must be positive" }
    }
}

enum class WakeOccurrenceKind {
    PRIMARY,
    SNOOZE,
}

enum class LocalTimeResolution {
    EXACT,
    DST_GAP_FIRST_VALID_TIME,
    DST_OVERLAP_EARLIER_OFFSET,
}

data class WakeOccurrence(
    val id: WakeOccurrenceId,
    val wakeScheduleId: WakeScheduleId,
    val kind: WakeOccurrenceKind,
    val scheduledLocalDateTime: LocalDateTime,
    val scheduledAt: ZonedDateTime,
    val scheduleRevision: Long,
    val localTimeResolution: LocalTimeResolution,
)
