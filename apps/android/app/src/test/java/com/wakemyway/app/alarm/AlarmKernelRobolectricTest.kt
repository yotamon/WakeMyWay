package com.wakemyway.app.alarm

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
import org.junit.Assert.assertNull
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

    private fun oneShotSchedule(suffix: String): WakeSchedule {
        val target = now.plus(Duration.ofMinutes(20)).atZone(ZoneOffset.UTC)
        return WakeSchedule(
            id = WakeScheduleId("robolectric-$suffix"),
            zoneId = ZoneOffset.UTC,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = 1,
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
        )
    }
}
