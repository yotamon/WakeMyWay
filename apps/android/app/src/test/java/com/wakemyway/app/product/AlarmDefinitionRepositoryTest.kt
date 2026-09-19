package com.wakemyway.app.product

import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmDefinitionRepositoryTest {
    private val fileName = "alarm-definition-repository-${System.nanoTime()}.json"
    private val context
        get() = RuntimeEnvironment.getApplication()

    private lateinit var repository: AlarmDefinitionRepository

    @Before
    fun setUp() {
        repository = AlarmDefinitionRepository(context, fileName)
        repository.clear()
    }

    @After
    fun tearDown() {
        repository.clear()
        context.filesDir.resolve(fileName).delete()
        context.filesDir.resolve("$fileName.bak").delete()
    }

    @Test
    fun `repository round trips complete alarm definitions`() {
        val alarm = recurringAlarm(
            id = "workdays",
            revision = 1,
            soundId = WakeSoundId.SOFT_START,
            voiceStyle = VoiceStyle.MINIMAL,
            snoozePolicy = SnoozePolicy(
                enabled = true,
                duration = Duration.ofMinutes(10),
                maxCount = 2,
            ),
            tomorrowContractMode = TomorrowContractMode.ALWAYS_PROMPT,
            firstMoveDefault = "Shower",
        )

        repository.upsert(alarm)

        val reloaded = AlarmDefinitionRepository(context, fileName).get(alarm.id)
        assertEquals(alarm, reloaded)
    }

    @Test
    fun `schema v1 alarm document remains readable across app upgrades`() {
        context.filesDir.resolve(fileName).writeText(
            """
            {
              "schemaVersion": 1,
              "alarms": [
                {
                  "id": "legacy-workdays",
                  "label": "Legacy Workdays",
                  "enabled": true,
                  "zoneId": "Europe/Berlin",
                  "schedule": {
                    "type": "weekly",
                    "time": "07:15",
                    "days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
                  },
                  "soundId": "soft-start",
                  "voiceCheckInEnabled": true,
                  "characterId": "alfred",
                  "voiceStyle": "MINIMAL",
                  "snooze": {
                    "enabled": true,
                    "durationSeconds": 600,
                    "maxCount": 2
                  },
                  "tomorrowContractMode": "ALWAYS_PROMPT",
                  "firstMoveDefault": "Open the curtains",
                  "revision": 4,
                  "createdAt": "2026-09-01T08:00:00Z",
                  "updatedAt": "2026-09-10T09:30:00Z"
                }
              ]
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val restored = AlarmDefinitionRepository(context, fileName)
            .get(AlarmDefinitionId("legacy-workdays"))

        assertEquals("Legacy Workdays", restored?.label)
        assertEquals(WakeSoundId.SOFT_START, restored?.soundId)
        assertEquals(VoiceStyle.MINIMAL, restored?.voiceStyle)
        assertEquals(Duration.ofMinutes(10), restored?.snoozePolicy?.duration)
        assertEquals(TomorrowContractMode.ALWAYS_PROMPT, restored?.tomorrowContractMode)
        assertEquals("Open the curtains", restored?.firstMoveDefault)
        assertEquals(4L, restored?.revision)
    }

    @Test
    fun `repository preserves insertion order across multiple alarms`() {
        val first = recurringAlarm(id = "first", revision = 1)
        val second = oneShotAlarm(id = "second", revision = 1)

        repository.upsert(first)
        repository.upsert(second)

        assertEquals(listOf(first, second), repository.list())
    }

    @Test
    fun `upsert requires a newer revision for edits`() {
        val initial = recurringAlarm(id = "edit", revision = 2)
        repository.upsert(initial)

        val older = initial.copy(revision = 1)
        val result = runCatching { repository.upsert(older) }

        assertTrue(result.isFailure)
        assertEquals(initial, repository.get(initial.id))
    }

    @Test
    fun `repeating the exact object is idempotent`() {
        val alarm = recurringAlarm(id = "idempotent", revision = 3)
        repository.upsert(alarm)
        repository.upsert(alarm)

        assertEquals(listOf(alarm), repository.list())
    }

    @Test
    fun `editing one alarm leaves unrelated alarms unchanged`() {
        val first = recurringAlarm(id = "first", revision = 1)
        val second = recurringAlarm(id = "second", revision = 1)
        repository.replaceAll(listOf(first, second))

        val editedFirst = first.copy(
            label = "Earlier workday",
            revision = 2,
            updatedAt = first.updatedAt.plusSeconds(60),
        )
        repository.upsert(editedFirst)

        assertEquals(editedFirst, repository.get(first.id))
        assertEquals(second, repository.get(second.id))
    }

    @Test
    fun `delete removes only the requested alarm`() {
        val first = recurringAlarm(id = "first", revision = 1)
        val second = recurringAlarm(id = "second", revision = 1)
        repository.replaceAll(listOf(first, second))

        assertTrue(repository.delete(first.id))
        assertFalse(repository.delete(first.id))
        assertNull(repository.get(first.id))
        assertEquals(second, repository.get(second.id))
    }

    @Test
    fun `replace all rejects duplicate ids`() {
        val alarm = recurringAlarm(id = "duplicate", revision = 1)
        val result = runCatching { repository.replaceAll(listOf(alarm, alarm)) }

        assertTrue(result.isFailure)
        assertTrue(repository.list().isEmpty())
    }

    private fun recurringAlarm(
        id: String,
        revision: Long,
        soundId: WakeSoundId = WakeSoundId.MORNING_LIGHT,
        voiceStyle: VoiceStyle = VoiceStyle.DEFAULT,
        snoozePolicy: SnoozePolicy = SnoozePolicy(),
        tomorrowContractMode: TomorrowContractMode = TomorrowContractMode.OPTIONAL,
        firstMoveDefault: String? = null,
    ): AlarmDefinition {
        val created = Instant.parse("2026-09-15T00:00:00Z")
        return AlarmDefinition(
            id = AlarmDefinitionId(id),
            label = "Workdays",
            enabled = true,
            zoneId = ZoneId.of("Europe/Berlin"),
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
            soundId = soundId,
            voiceCheckInEnabled = true,
            voiceStyle = voiceStyle,
            snoozePolicy = snoozePolicy,
            tomorrowContractMode = tomorrowContractMode,
            firstMoveDefault = firstMoveDefault,
            revision = revision,
            createdAt = created,
            updatedAt = created.plusSeconds(revision - 1),
        )
    }

    private fun oneShotAlarm(id: String, revision: Long): AlarmDefinition {
        val created = Instant.parse("2026-09-15T00:00:00Z")
        return AlarmDefinition(
            id = AlarmDefinitionId(id),
            label = "Flight",
            enabled = true,
            zoneId = ZoneId.of("Europe/Berlin"),
            schedule = AlarmSchedulePattern.OneShot(
                date = LocalDate.of(2026, 9, 22),
                time = LocalTime.of(5, 45),
            ),
            revision = revision,
            createdAt = created,
            updatedAt = created.plusSeconds(revision - 1),
        )
    }
}
