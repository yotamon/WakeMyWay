package com.wakemyway.app.alarm

import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import com.wakemyway.core.schedule.NextWakeOccurrenceResolver
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CriticalAlarmStateTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun `schema one snapshot migrates without changing its occurrence authority`() {
        val schedule = WakeSchedule(
            id = WakeScheduleId("legacy-primary"),
            zoneId = berlin,
            timesByDay = mapOf(DayOfWeek.WEDNESDAY to LocalTime.of(7, 30)),
            revision = 9,
            completionPolicy = WakeCompletionPolicy.RECURRING,
        )
        val next = NextWakeOccurrenceResolver().resolve(
            schedule,
            Instant.parse("2026-09-15T00:00:00Z"),
        )
        val legacy = CriticalWakeSnapshot(
            schedule = schedule,
            nextOccurrence = next,
            activeOccurrence = null,
            registeredOccurrenceId = next.id,
            generation = 12,
            enabled = true,
        )

        val migrated = CriticalAlarmState.decodeOrMigrate(legacy.encode())

        assertEquals(CriticalAlarmState.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(12, migrated.generation)
        assertNull(migrated.activeOccurrence)
        val slot = migrated.slots.getValue(schedule.id)
        assertTrue(slot.enabled)
        assertEquals(schedule, slot.schedule)
        assertEquals(next, slot.nextOccurrence)
        assertEquals(next.id, slot.registeredOccurrenceId)
        assertEquals(CriticalWakePolicy.DEFAULT, slot.policy)
    }

    @Test
    fun `legacy cancellation tombstone stays disabled after migration`() {
        val schedule = WakeSchedule(
            id = WakeScheduleId("legacy-disabled"),
            zoneId = berlin,
            timesByDay = mapOf(DayOfWeek.MONDAY to LocalTime.of(7, 0)),
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
        )
        val legacy = CriticalWakeSnapshot(
            schedule = schedule,
            nextOccurrence = null,
            activeOccurrence = null,
            registeredOccurrenceId = null,
            generation = 3,
            enabled = false,
        )

        val migrated = CriticalAlarmState.decodeOrMigrate(legacy.encode())

        val slot = migrated.slots.getValue(schedule.id)
        assertFalse(slot.enabled)
        assertNull(slot.nextOccurrence)
        assertNull(slot.registeredOccurrenceId)
        assertEquals(CriticalWakePolicy.DEFAULT, slot.policy)
    }

    @Test
    fun `schema two state migrates with default execution policy`() {
        val schedule = WakeSchedule(
            id = WakeScheduleId("v2-alarm"),
            zoneId = berlin,
            timesByDay = mapOf(DayOfWeek.MONDAY to LocalTime.of(7, 0)),
        )
        val v3 = CriticalAlarmState(
            slots = mapOf(
                schedule.id to CriticalScheduleSlot(
                    schedule = schedule,
                    nextOccurrence = null,
                    registeredOccurrenceId = null,
                    enabled = false,
                ),
            ),
            activeOccurrence = null,
            generation = 4,
        )
        val v2Json = JSONObject(v3.encode()).apply {
            put("schemaVersion", 2)
            val slots = getJSONArray("slots")
            repeat(slots.length()) { index ->
                slots.getJSONObject(index).remove("policy")
            }
        }

        val migrated = CriticalAlarmState.decodeOrMigrate(v2Json.toString())

        assertEquals(CriticalAlarmState.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(CriticalWakePolicy.DEFAULT, migrated.slots.getValue(schedule.id).policy)
    }

    @Test
    fun `schema three round trips independent slots and execution policy`() {
        val exactDate = LocalDate.of(2026, 9, 22)
        val first = WakeSchedule(
            id = WakeScheduleId("first"),
            zoneId = berlin,
            timesByDay = mapOf(DayOfWeek.MONDAY to LocalTime.of(7, 0)),
        )
        val second = WakeSchedule(
            id = WakeScheduleId("second"),
            zoneId = berlin,
            timesByDay = mapOf(exactDate.dayOfWeek to LocalTime.of(5, 45)),
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
            oneShotDate = exactDate,
        )
        val customPolicy = CriticalWakePolicy(
            soundId = WakeSoundId.SOFT_START,
            voiceCheckInEnabled = false,
            characterId = CharacterId.ALFRED,
            voiceStyle = VoiceStyle.MINIMAL,
            snoozeEnabled = true,
            snoozeDuration = Duration.ofMinutes(15),
        )
        val state = CriticalAlarmState(
            slots = linkedMapOf(
                first.id to CriticalScheduleSlot(first, null, null, enabled = false),
                second.id to CriticalScheduleSlot(
                    schedule = second,
                    nextOccurrence = null,
                    registeredOccurrenceId = null,
                    enabled = false,
                    policy = customPolicy,
                ),
            ),
            activeOccurrence = null,
            generation = 8,
        )

        val restored = CriticalAlarmState.decodeOrMigrate(state.encode())

        assertEquals(state, restored)
        assertEquals(customPolicy, restored.slots.getValue(second.id).policy)
    }
}