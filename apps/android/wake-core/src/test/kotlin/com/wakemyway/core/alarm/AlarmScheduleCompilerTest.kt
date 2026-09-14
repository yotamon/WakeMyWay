package com.wakemyway.core.alarm

import com.wakemyway.core.schedule.WakeCompletionPolicy
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmScheduleCompilerTest {
    private val compiler = AlarmScheduleCompiler()
    private val berlin = ZoneId.of("Europe/Berlin")
    private val instant = Instant.parse("2026-09-15T00:00:00Z")

    @Test
    fun `weekly alarm compiles to recurring kernel schedule`() {
        val alarm = alarm(
            schedule = AlarmSchedulePattern.Weekly(
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                time = LocalTime.of(7, 15),
            ),
        )

        val schedule = compiler.compile(alarm)

        assertEquals("work", schedule.id.value)
        assertEquals(WakeCompletionPolicy.RECURRING, schedule.completionPolicy)
        assertEquals(
            mapOf(
                DayOfWeek.MONDAY to LocalTime.of(7, 15),
                DayOfWeek.WEDNESDAY to LocalTime.of(7, 15),
                DayOfWeek.FRIDAY to LocalTime.of(7, 15),
            ),
            schedule.timesByDay,
        )
        assertNull(schedule.oneShotDate)
        assertEquals(alarm.revision, schedule.revision)
    }

    @Test
    fun `one shot alarm preserves exact date in kernel schedule`() {
        val date = LocalDate.of(2026, 10, 6)
        val alarm = alarm(
            schedule = AlarmSchedulePattern.OneShot(
                date = date,
                time = LocalTime.of(5, 50),
            ),
        )

        val schedule = compiler.compile(alarm)

        assertEquals(WakeCompletionPolicy.ONE_SHOT, schedule.completionPolicy)
        assertEquals(date, schedule.oneShotDate)
        assertEquals(mapOf(date.dayOfWeek to LocalTime.of(5, 50)), schedule.timesByDay)
    }

    private fun alarm(schedule: AlarmSchedulePattern) = AlarmDefinition(
        id = AlarmDefinitionId("work"),
        enabled = true,
        zoneId = berlin,
        schedule = schedule,
        revision = 7,
        createdAt = instant,
        updatedAt = instant,
    )
}
