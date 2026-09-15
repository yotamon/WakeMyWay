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
class WakeTerminalActionsRobolectricTest {
    private val now = Instant.parse("2026-09-15T05:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val context
        get() = RuntimeEnvironment.getApplication()

    private lateinit var kernel: AlarmKernel
    private lateinit var actions: WakeTerminalActions

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        kernel = AlarmKernel(
            context = context,
            clock = clock,
            criticalStateFileName = "terminal-actions-${System.nanoTime()}.json",
        )
        actions = WakeTerminalActions(context, kernel)
    }

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        runCatching { kernel.cancelSchedule() }
    }

    @Test
    fun `failed snooze acknowledgement leaves current wake active`() {
        val primary = activate("snooze-failure")
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        assertFalse(actions.snooze(primary.id))
        assertEquals(primary.id, kernel.activeOccurrence()?.id)
        assertNull(kernel.health().nextOccurrence)
    }

    @Test
    fun `successful stop is durably acknowledged before UI may close`() {
        val primary = activate("stop-success")

        assertTrue(actions.stop(primary.id))
        assertNull(kernel.activeOccurrence())
        assertNull(kernel.health().nextOccurrence)
    }

    @Test
    fun `stale surface cannot stop newer active occurrence`() {
        val primary = activate("stale-surface")
        assertTrue(actions.snooze(primary.id))
        val snooze = requireNotNull(kernel.health().nextOccurrence)
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(snooze.id))

        assertTrue(actions.stop(primary.id))
        assertEquals(snooze.id, kernel.activeOccurrence()?.id)
    }

    private fun activate(suffix: String) = run {
        val committed = kernel.commitSchedule(oneShotSchedule(suffix))
        val occurrence = requireNotNull(committed.nextOccurrence)
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(occurrence.id))
        occurrence
    }

    private fun oneShotSchedule(suffix: String): WakeSchedule {
        val target = now.plus(Duration.ofMinutes(20)).atZone(ZoneOffset.UTC)
        return WakeSchedule(
            id = WakeScheduleId("terminal-$suffix"),
            zoneId = ZoneOffset.UTC,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = 1,
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
            oneShotDate = target.toLocalDate(),
        )
    }
}
