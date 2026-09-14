package com.wakemyway.core.schedule

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Resolves the next PRIMARY Wake Occurrence strictly after [now].
 *
 * DST policy:
 * - normal local time: use the only valid offset;
 * - spring-forward gap: fire at the first valid local time after the gap;
 * - fall-back overlap: use the earlier occurrence so the alarm is never an hour late.
 */
class NextWakeOccurrenceResolver {
    fun resolve(schedule: WakeSchedule, now: Instant): WakeOccurrence {
        schedule.oneShotDate?.let { targetDate ->
            val targetTime = requireNotNull(schedule.timesByDay[targetDate.dayOfWeek]) {
                "Exact-date one-shot schedule is missing its target weekday"
            }
            val intended = LocalDateTime.of(targetDate, targetTime)
            val resolved = resolveLocalDateTime(schedule, intended)
            require(resolved.zonedDateTime.toInstant().isAfter(now)) {
                "Exact-date one-shot schedule no longer has a future occurrence"
            }
            return occurrence(schedule, intended, resolved)
        }

        val localToday = now.atZone(schedule.zoneId).toLocalDate()
        for (dayOffset in 0L..7L) {
            val date = localToday.plusDays(dayOffset)
            val time = schedule.timesByDay[date.dayOfWeek] ?: continue
            val intended = LocalDateTime.of(date, time)
            val resolved = resolveLocalDateTime(schedule, intended)

            if (resolved.zonedDateTime.toInstant().isAfter(now)) {
                return occurrence(schedule, intended, resolved)
            }
        }

        error("WakeSchedule did not produce a future occurrence within one full recurrence cycle")
    }

    private fun occurrence(
        schedule: WakeSchedule,
        intended: LocalDateTime,
        resolved: ResolvedLocalDateTime,
    ): WakeOccurrence = WakeOccurrence(
        id = occurrenceId(schedule, resolved.zonedDateTime),
        wakeScheduleId = schedule.id,
        kind = WakeOccurrenceKind.PRIMARY,
        scheduledLocalDateTime = intended,
        scheduledAt = resolved.zonedDateTime,
        scheduleRevision = schedule.revision,
        localTimeResolution = resolved.resolution,
    )

    private fun resolveLocalDateTime(
        schedule: WakeSchedule,
        intended: LocalDateTime,
    ): ResolvedLocalDateTime {
        val rules = schedule.zoneId.rules
        val validOffsets = rules.getValidOffsets(intended)

        return when (validOffsets.size) {
            1 -> ResolvedLocalDateTime(
                zonedDateTime = ZonedDateTime.ofLocal(intended, schedule.zoneId, validOffsets.single()),
                resolution = LocalTimeResolution.EXACT,
            )

            0 -> {
                val transition = requireNotNull(rules.getTransition(intended)) {
                    "Expected a timezone transition for invalid local time $intended"
                }
                ResolvedLocalDateTime(
                    zonedDateTime = transition.dateTimeAfter.atZone(schedule.zoneId),
                    resolution = LocalTimeResolution.DST_GAP_FIRST_VALID_TIME,
                )
            }

            2 -> ResolvedLocalDateTime(
                zonedDateTime = ZonedDateTime.ofLocal(
                    intended,
                    schedule.zoneId,
                    earlierInstantOffset(validOffsets[0], validOffsets[1]),
                ),
                resolution = LocalTimeResolution.DST_OVERLAP_EARLIER_OFFSET,
            )

            else -> error("Unexpected number of valid offsets for $intended: ${validOffsets.size}")
        }
    }

    private fun earlierInstantOffset(first: ZoneOffset, second: ZoneOffset): ZoneOffset =
        if (first.totalSeconds >= second.totalSeconds) first else second

    private fun occurrenceId(schedule: WakeSchedule, scheduledAt: ZonedDateTime): WakeOccurrenceId =
        WakeOccurrenceId("${schedule.id.value}:${schedule.revision}:${scheduledAt.toInstant().epochSecond}")

    private data class ResolvedLocalDateTime(
        val zonedDateTime: ZonedDateTime,
        val resolution: LocalTimeResolution,
    )
}
