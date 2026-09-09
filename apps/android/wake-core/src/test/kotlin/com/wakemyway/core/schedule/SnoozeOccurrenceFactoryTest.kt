package com.wakemyway.core.schedule

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SnoozeOccurrenceFactoryTest {
    private val schedule = WakeSchedule(
        id = WakeScheduleId("primary"),
        zoneId = ZoneId.of("Europe/Berlin"),
        timesByDay = mapOf(DayOfWeek.WEDNESDAY to LocalTime.of(7, 30)),
        revision = 4,
    )

    @Test
    fun `snooze is a new exact occurrence after now`() {
        val now = Instant.parse("2026-09-09T05:30:00Z")

        val occurrence = SnoozeOccurrenceFactory().create(
            schedule = schedule,
            now = now,
            duration = Duration.ofMinutes(7),
        )

        assertEquals(WakeOccurrenceKind.SNOOZE, occurrence.kind)
        assertEquals(Instant.parse("2026-09-09T05:37:00Z"), occurrence.scheduledAt.toInstant())
        assertEquals(LocalTimeResolution.EXACT, occurrence.localTimeResolution)
        assertEquals(4, occurrence.scheduleRevision)
    }

    @Test
    fun `snooze duration must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            SnoozeOccurrenceFactory().create(schedule, Instant.EPOCH, Duration.ZERO)
        }
    }
}
