package com.wakemyway.app.alarm

import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeOccurrence
import com.wakemyway.core.schedule.WakeOccurrenceId
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
    private val observed = mutableListOf<ObservedTerminal>()

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        observed.clear()
        kernel = AlarmKernel(
            context = context,
            clock = clock,
            criticalStateFileName = "terminal-actions-${System.nanoTime()}.json",
        )
        actions = WakeTerminalActions(
            context = context,
            kernel = kernel,
            observer = object : WakeTerminalObserver {
                override fun onTerminal(
                    occurrence: WakeOccurrence,
                    reason: WakeTerminalReason,
                    replacement: WakeOccurrence?,
                ) {
                    observed += ObservedTerminal(occurrence.id, reason, replacement?.id)
                }
            },
        )
    }

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        runCatching { kernel.cancelSchedule() }
    }

    @Test
    fun `failed snooze acknowledgement leaves current wake active and records nothing`() {
        val primary = activate("snooze-failure")
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        assertFalse(actions.snooze(primary.id))
        assertEquals(primary.id, kernel.activeOccurrence()?.id)
        assertNull(kernel.health().nextOccurrence)
        assertTrue(observed.isEmpty())
    }

    @Test
    fun `successful stop is durably acknowledged before observer is notified`() {
        val primary = activate("stop-success")

        assertTrue(actions.stop(primary.id))
        assertNull(kernel.activeOccurrence())
        assertNull(kernel.health().nextOccurrence)
        assertEquals(
            listOf(ObservedTerminal(primary.id, WakeTerminalReason.STOPPED, null)),
            observed,
        )
    }

    @Test
    fun `runtime completion is durably attributed as completed`() {
        val primary = activate("runtime-complete")

        assertTrue(actions.stop(primary.id, WakeTerminalReason.COMPLETED))
        assertNull(kernel.activeOccurrence())
        assertEquals(
            listOf(ObservedTerminal(primary.id, WakeTerminalReason.COMPLETED, null)),
            observed,
        )
    }

    @Test
    fun `successful snooze reports durable replacement lineage`() {
        val primary = activate("snooze-success")

        assertTrue(actions.snooze(primary.id))
        val replacement = requireNotNull(kernel.health().nextOccurrence)

        assertEquals(
            listOf(
                ObservedTerminal(
                    occurrenceId = primary.id,
                    reason = WakeTerminalReason.SNOOZED,
                    replacementId = replacement.id,
                ),
            ),
            observed,
        )
    }

    @Test
    fun `stale surface cannot stop newer active occurrence or emit another terminal event`() {
        val primary = activate("stale-surface")
        assertTrue(actions.snooze(primary.id))
        val snooze = requireNotNull(kernel.health().nextOccurrence)
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(snooze.id))
        val observationsAfterSnooze = observed.toList()

        assertTrue(actions.stop(primary.id))
        assertEquals(snooze.id, kernel.activeOccurrence()?.id)
        assertEquals(observationsAfterSnooze, observed)
    }

    @Test
    fun `observer failure cannot turn a durable Stop into a failed acknowledgement`() {
        val primary = activate("observer-failure")
        val throwingActions = WakeTerminalActions(
            context = context,
            kernel = kernel,
            observer = object : WakeTerminalObserver {
                override fun onTerminal(
                    occurrence: WakeOccurrence,
                    reason: WakeTerminalReason,
                    replacement: WakeOccurrence?,
                ) {
                    error("history storage unavailable")
                }
            },
        )

        assertTrue(throwingActions.stop(primary.id))
        assertNull(kernel.activeOccurrence())
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

    private data class ObservedTerminal(
        val occurrenceId: WakeOccurrenceId,
        val reason: WakeTerminalReason,
        val replacementId: WakeOccurrenceId?,
    )
}
