package com.wakemyway.core.schedule

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NextWakeOccurrenceResolverTest {
    private val resolver = NextWakeOccurrenceResolver()
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun `resolves same-day wake when it is still in the future`() {
        val schedule = schedule(DayOfWeek.WEDNESDAY to LocalTime.of(8, 0))
        val now = Instant.parse("2026-09-09T05:00:00Z")
        val occurrence = resolver.resolve(schedule, now)
        assertEquals("2026-09-09T08:00+02:00[Europe/Berlin]", occurrence.scheduledAt.toString())
        assertEquals(LocalTimeResolution.EXACT, occurrence.localTimeResolution)
    }

    @Test
    fun `rolls to the next active weekday when today's time has passed`() {
        val schedule = schedule(
            DayOfWeek.WEDNESDAY to LocalTime.of(8, 0),
            DayOfWeek.FRIDAY to LocalTime.of(7, 30),
        )
        val now = Instant.parse("2026-09-09T08:00:00Z")
        val occurrence = resolver.resolve(schedule, now)
        assertEquals(DayOfWeek.FRIDAY, occurrence.scheduledAt.dayOfWeek)
        assertEquals(LocalTime.of(7, 30), occurrence.scheduledAt.toLocalTime())
    }

    @Test
    fun `spring DST gap resolves to first valid local time after the gap`() {
        val schedule = schedule(DayOfWeek.SUNDAY to LocalTime.of(2, 30))
        val now = Instant.parse("2026-03-28T12:00:00Z")
        val occurrence = resolver.resolve(schedule, now)
        assertEquals("2026-03-29T03:00+02:00[Europe/Berlin]", occurrence.scheduledAt.toString())
        assertEquals(LocalTimeResolution.DST_GAP_FIRST_VALID_TIME, occurrence.localTimeResolution)
    }

    @Test
    fun `fall DST overlap uses the earlier physical occurrence`() {
        val schedule = schedule(DayOfWeek.SUNDAY to LocalTime.of(2, 30))
        val now = Instant.parse("2026-10-24T12:00:00Z")
        val occurrence = resolver.resolve(schedule, now)
        assertEquals(ZoneOffset.ofHours(2), occurrence.scheduledAt.offset)
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), occurrence.scheduledAt.toInstant())
        assertEquals(LocalTimeResolution.DST_OVERLAP_EARLIER_OFFSET, occurrence.localTimeResolution)
    }

    @Test
    fun `next occurrence is always strictly after now`() {
        val schedule = schedule(DayOfWeek.WEDNESDAY to LocalTime.of(8, 0))
        val exactAlarmInstant = Instant.parse("2026-09-09T06:00:00Z")
        val occurrence = resolver.resolve(schedule, exactAlarmInstant)
        assertTrue(occurrence.scheduledAt.toInstant().isAfter(exactAlarmInstant))
        assertEquals("2026-09-16T08:00+02:00[Europe/Berlin]", occurrence.scheduledAt.toString())
    }

    private fun schedule(vararg times: Pair<DayOfWeek, LocalTime>) = WakeSchedule(
        id = WakeScheduleId("primary"),
        zoneId = berlin,
        timesByDay = mapOf(*times),
    )
}
