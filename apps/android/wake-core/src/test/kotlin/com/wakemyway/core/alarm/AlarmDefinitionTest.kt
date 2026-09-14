package com.wakemyway.core.alarm

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AlarmDefinitionTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val createdAt = Instant.parse("2026-09-15T00:00:00Z")

    @Test
    fun `consumer alarm defaults are stable and local-first friendly`() {
        val alarm = AlarmDefinition(
            id = AlarmDefinitionId("workdays"),
            zoneId = berlin,
            schedule = AlarmSchedulePattern.Weekly(
                days = setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                ),
                time = LocalTime.of(7, 0),
            ),
            createdAt = createdAt,
            updatedAt = createdAt,
        )

        assertTrue(alarm.enabled)
        assertEquals(WakeSoundId.MORNING_LIGHT, alarm.soundId)
        assertTrue(alarm.voiceCheckInEnabled)
        assertEquals(CharacterId.ALFRED, alarm.characterId)
        assertEquals(VoiceStyle.DEFAULT, alarm.voiceStyle)
        assertEquals(Duration.ofMinutes(5), alarm.snoozePolicy.duration)
        assertEquals(TomorrowContractMode.OPTIONAL, alarm.tomorrowContractMode)
    }

    @Test
    fun `weekly schedule requires at least one day`() {
        assertFailsWith<IllegalArgumentException> {
            AlarmSchedulePattern.Weekly(
                days = emptySet(),
                time = LocalTime.of(7, 0),
            )
        }
    }

    @Test
    fun `one shot schedule preserves exact local date`() {
        val date = LocalDate.of(2026, 9, 22)
        val schedule = AlarmSchedulePattern.OneShot(
            date = date,
            time = LocalTime.of(6, 45),
        )

        assertEquals(date, schedule.date)
        assertEquals(LocalTime.of(6, 45), schedule.time)
    }

    @Test
    fun `branded wake sound ids remain stable`() {
        assertEquals("morning-light", WakeSoundId.MORNING_LIGHT.value)
        assertEquals("soft-start", WakeSoundId.SOFT_START.value)
        assertEquals("morning-pulse", WakeSoundId.MORNING_PULSE.value)
    }

    @Test
    fun `snooze policy rejects unsafe configuration`() {
        assertFailsWith<IllegalArgumentException> {
            SnoozePolicy(duration = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            SnoozePolicy(duration = Duration.ofMinutes(31))
        }
        assertFailsWith<IllegalArgumentException> {
            SnoozePolicy(maxCount = 0)
        }
    }

    @Test
    fun `alarm revision and timestamps are monotonic`() {
        assertFailsWith<IllegalArgumentException> {
            alarm(revision = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            alarm(updatedAt = createdAt.minusSeconds(1))
        }
    }

    @Test
    fun `labels and first move defaults stay bounded`() {
        assertFailsWith<IllegalArgumentException> {
            alarm(label = "x".repeat(AlarmDefinition.MAX_LABEL_CHARACTERS + 1))
        }
        assertFailsWith<IllegalArgumentException> {
            alarm(firstMoveDefault = "x".repeat(AlarmDefinition.MAX_FIRST_MOVE_CHARACTERS + 1))
        }
    }

    private fun alarm(
        label: String = "",
        firstMoveDefault: String? = null,
        revision: Long = 1,
        updatedAt: Instant = createdAt,
    ) = AlarmDefinition(
        id = AlarmDefinitionId("test"),
        label = label,
        zoneId = berlin,
        schedule = AlarmSchedulePattern.OneShot(
            date = LocalDate.of(2026, 9, 16),
            time = LocalTime.of(7, 30),
        ),
        firstMoveDefault = firstMoveDefault,
        revision = revision,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
