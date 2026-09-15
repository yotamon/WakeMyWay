package com.wakemyway.app.alarm

import com.wakemyway.core.alarm.WakeSoundId
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmKernelRobolectricTest {
    private val now = Instant.parse("2026-09-12T04:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val context
        get() = RuntimeEnvironment.getApplication()

    private lateinit var kernel: AlarmKernel

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        kernel = AlarmKernel(
            context = context,
            clock = clock,
            criticalStateFileName = "critical-wake-robolectric-${System.nanoTime()}.json",
        )
    }

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        runCatching { kernel.cancelSchedule() }
    }

    @Test
    fun `failed snooze keeps the current occurrence active`() {
        val committed = kernel.commitSchedule(oneShotSchedule("snooze-failure"))
        val primary = requireNotNull(committed.nextOccurrence)
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(primary.id))

        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        assertNull(kernel.snoozeActive(primary.id, Duration.ofMinutes(5)))
        assertEquals(primary.id, kernel.activeOccurrence()?.id)
        assertNull(kernel.health().nextOccurrence)
    }

    @Test
    fun `stale terminal occurrence cannot replace current active authority`() {
        val committed = kernel.commitSchedule(oneShotSchedule("stale-terminal"))
        val primary = requireNotNull(committed.nextOccurrence)
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(primary.id))

        val snooze = requireNotNull(kernel.snoozeActive(primary.id, Duration.ofMinutes(5)))
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(snooze.id))

        assertFalse(kernel.stopActive(primary.id))
        assertEquals(snooze.id, kernel.activeOccurrence()?.id)
    }

    @Test
    fun `selected wake sound is durable for active execution and snooze chain`() {
        val schedule = oneShotSchedule("sound-chain")
        val committed = kernel.commitSchedule(schedule, WakeSoundId.SOFT_START)
        val primary = requireNotNull(committed.nextOccurrence)

        assertEquals(WakeSoundId.SOFT_START, kernel.wakeSoundId(schedule.id))
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(primary.id))
        assertEquals(WakeSoundId.SOFT_START, kernel.activeWakeSoundId(primary.id))

        val snooze = requireNotNull(kernel.snoozeActive(primary.id, Duration.ofMinutes(5)))
        assertEquals(WakeSoundId.SOFT_START, kernel.wakeSoundId(schedule.id))
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(snooze.id))
        assertEquals(WakeSoundId.SOFT_START, kernel.activeWakeSoundId(snooze.id))
    }

    @Test
    fun `default schedule commit uses morning light sound id`() {
        val schedule = oneShotSchedule("default-sound")

        kernel.commitSchedule(schedule)

        assertEquals(WakeSoundId.MORNING_LIGHT, kernel.wakeSoundId(schedule.id))
    }

    @Test
    fun `two schedules retain independent future occurrences`() {
        kernel.commitSchedule(oneShotSchedule("first", minutesFromNow = 20))
        kernel.commitSchedule(oneShotSchedule("second", minutesFromNow = 40))

        val occurrences = kernel.nextOccurrences()

        assertEquals(2, occurrences.size)
        assertEquals(2, kernel.currentSchedules().size)
        assertEquals(2, kernel.health().enabledScheduleCount)
        assertNotEquals(occurrences[0].wakeScheduleId, occurrences[1].wakeScheduleId)
    }

    @Test
    fun `editing one schedule leaves unrelated occurrence untouched`() {
        val firstV1 = oneShotSchedule("edit-first", minutesFromNow = 20, revision = 1)
        val second = oneShotSchedule("edit-second", minutesFromNow = 40, revision = 1)
        kernel.commitSchedule(firstV1)
        kernel.commitSchedule(second)
        val secondBefore = requireNotNull(kernel.health(second.id)?.nextOccurrence)

        kernel.commitSchedule(
            oneShotSchedule("edit-first", minutesFromNow = 30, revision = 2),
        )

        assertEquals(secondBefore, kernel.health(second.id)?.nextOccurrence)
        assertEquals(2, kernel.health().enabledScheduleCount)
    }

    @Test
    fun `disabling one schedule leaves the other ready`() {
        val first = oneShotSchedule("disable-first", minutesFromNow = 20)
        val second = oneShotSchedule("disable-second", minutesFromNow = 40)
        kernel.commitSchedule(first)
        kernel.commitSchedule(second)

        kernel.cancelSchedule(first.id)

        assertFalse(requireNotNull(kernel.health(first.id)).enabled)
        assertTrue(requireNotNull(kernel.health(second.id)).enabled)
        assertEquals(1, kernel.health().enabledScheduleCount)
        assertEquals(second.id, kernel.health().nextOccurrence?.wakeScheduleId)
    }

    @Test
    fun `old revision cannot become active after schedule edit`() {
        val v1 = oneShotSchedule("revision", minutesFromNow = 20, revision = 1)
        val oldOccurrence = requireNotNull(kernel.commitSchedule(v1).nextOccurrence)
        kernel.commitSchedule(oneShotSchedule("revision", minutesFromNow = 30, revision = 2))

        assertEquals(BeginActiveResult.STALE, kernel.beginActive(oldOccurrence.id))
        assertNull(kernel.activeOccurrence())
    }

    @Test
    fun `second valid occurrence cannot create competing active execution`() {
        val first = oneShotSchedule("collision-first", minutesFromNow = 20)
        val second = oneShotSchedule("collision-second", minutesFromNow = 20)
        kernel.commitSchedule(first)
        kernel.commitSchedule(second)
        val firstOccurrence = requireNotNull(kernel.health(first.id)?.nextOccurrence)
        val secondOccurrence = requireNotNull(kernel.health(second.id)?.nextOccurrence)

        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(firstOccurrence.id))
        assertEquals(BeginActiveResult.CONFLICT, kernel.beginActive(secondOccurrence.id))
        assertEquals(firstOccurrence.id, kernel.activeOccurrence()?.id)
        assertEquals(secondOccurrence.id, kernel.health(second.id)?.nextOccurrence?.id)
    }

    @Test
    fun `snoozing one alarm preserves another alarm chain`() {
        val first = oneShotSchedule("snooze-first", minutesFromNow = 20)
        val second = oneShotSchedule("snooze-second", minutesFromNow = 40)
        kernel.commitSchedule(first)
        kernel.commitSchedule(second)
        val firstOccurrence = requireNotNull(kernel.health(first.id)?.nextOccurrence)
        val secondBefore = requireNotNull(kernel.health(second.id)?.nextOccurrence)
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(firstOccurrence.id))

        val snooze = requireNotNull(kernel.snoozeActive(firstOccurrence.id, Duration.ofMinutes(5)))

        assertEquals(first.id, snooze.wakeScheduleId)
        assertEquals(secondBefore, kernel.health(second.id)?.nextOccurrence)
    }

    private fun oneShotSchedule(
        suffix: String,
        minutesFromNow: Long = 20,
        revision: Long = 1,
    ): WakeSchedule {
        val target = now.plus(Duration.ofMinutes(minutesFromNow)).atZone(ZoneOffset.UTC)
        return WakeSchedule(
            id = WakeScheduleId("robolectric-$suffix"),
            zoneId = ZoneOffset.UTC,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = revision,
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
            oneShotDate = target.toLocalDate(),
        )
    }
}
