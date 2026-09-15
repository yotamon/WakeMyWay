package com.wakemyway.core.schedule

import java.time.DayOfWeek
import java.time.LocalDate
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

enum class WakeCompletionPolicy {
    /** After a primary wake completes or is missed, resolve and register the next primary occurrence. */
    RECURRING,

    /** After the wake chain completes or is irrecoverably missed, disable the schedule. */
    ONE_SHOT,
}

data class WakeSchedule(
    val id: WakeScheduleId,
    val zoneId: ZoneId,
    val timesByDay: Map<DayOfWeek, LocalTime>,
    val revision: Long = 1,
    val completionPolicy: WakeCompletionPolicy = WakeCompletionPolicy.RECURRING,
    /**
     * Exact local date for a modern one-shot schedule.
     *
     * `null` remains supported for legacy V1 one-shot schedules that encoded only a weekday/time.
     * New product code should always supply this field for `ONE_SHOT` schedules so a missed or
     * delayed reconciliation can never accidentally roll the alarm into a later week.
     */
    val oneShotDate: LocalDate? = null,
) {
    init {
        require(timesByDay.isNotEmpty()) { "A WakeSchedule needs at least one active day" }
        require(revision > 0) { "WakeSchedule revision must be positive" }
        require(completionPolicy == WakeCompletionPolicy.ONE_SHOT || oneShotDate == null) {
            "Only ONE_SHOT schedules may carry an exact date"
        }
        if (oneShotDate != null) {
            require(timesByDay.size == 1 && timesByDay.containsKey(oneShotDate.dayOfWeek)) {
                "An exact-date one-shot schedule must contain exactly its target weekday"
            }
        }
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
