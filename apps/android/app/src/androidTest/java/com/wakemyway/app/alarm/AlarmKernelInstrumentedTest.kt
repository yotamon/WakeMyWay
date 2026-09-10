package com.wakemyway.app.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.core.schedule.WakeOccurrenceKind
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Duration
import java.time.ZonedDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmKernelInstrumentedTest {
    private lateinit var context: Context
    private lateinit var registrar: AlarmRegistrar
    private lateinit var kernel: AlarmKernel
    private val registeredIds = linkedSetOf<WakeOccurrenceId>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        registrar = AlarmRegistrar(context)
        kernel = AlarmKernel(
            context = context,
            criticalStateFileName = "critical-wake-kernel-test-${System.nanoTime()}.json",
        )
        assumeTrue("Exact alarm capability is required for this device test", kernel.health().exactAlarmAllowed)
    }

    @After
    fun tearDown() {
        runCatching { kernel.cancelSchedule() }
        registeredIds.forEach { occurrenceId -> runCatching { registrar.cancel(occurrenceId) } }
    }

    @Test
    fun currentScheduleTracksEnabledAlarmAuthority() {
        val schedule = oneShotSchedule("current-schedule")
        val committed = kernel.commitSchedule(schedule)
        committed.nextOccurrence?.id?.let(registeredIds::add)

        assertEquals(schedule, kernel.currentSchedule())

        kernel.cancelSchedule()

        assertNull(kernel.currentSchedule())
    }

    @Test
    fun cancellationTombstonePreventsStaleOccurrenceResurrection() {
        val committed = kernel.commitSchedule(oneShotSchedule("cancel"))
        val primary = requireNotNull(committed.nextOccurrence)
        registeredIds += primary.id

        // This test runs on a clean CI emulator where notification/full-screen special access is
        // intentionally not guaranteed. Wake Ready must therefore match the real device
        // capabilities rather than assuming that exact registration alone is sufficient.
        assertEquals(
            committed.exactAlarmAllowed &&
                committed.notificationsAllowed &&
                committed.notificationChannelHighImportance &&
                committed.fullScreenIntentAllowed,
            committed.ready,
        )

        kernel.cancelSchedule()

        val cancelled = kernel.health()
        assertFalse(cancelled.ready)
        assertNull(cancelled.nextOccurrence)
        assertNull(cancelled.activeOccurrence)
        assertEquals(BeginActiveResult.STALE, kernel.beginActive(primary.id))

        val reconciled = kernel.reconcile()
        assertFalse(reconciled.ready)
        assertNull(reconciled.nextOccurrence)
        assertEquals(BeginActiveResult.STALE, kernel.beginActive(primary.id))
    }

    @Test
    fun oneShotSnoozeChainDisablesAfterFinalStop() {
        val committed = kernel.commitSchedule(oneShotSchedule("snooze"))
        val primary = requireNotNull(committed.nextOccurrence)
        registeredIds += primary.id

        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(primary.id))
        val snooze = requireNotNull(kernel.snoozeActive(primary.id, Duration.ofMinutes(5)))
        registeredIds += snooze.id

        assertEquals(WakeOccurrenceKind.SNOOZE, snooze.kind)
        assertEquals(snooze.id, kernel.health().nextOccurrence?.id)
        assertEquals(BeginActiveResult.STARTED, kernel.beginActive(snooze.id))
        assertTrue(kernel.stopActive(snooze.id))

        val finished = kernel.health()
        assertFalse(finished.ready)
        assertNull(finished.nextOccurrence)
        assertNull(finished.activeOccurrence)
        assertEquals(BeginActiveResult.STALE, kernel.beginActive(primary.id))
        assertEquals(BeginActiveResult.STALE, kernel.beginActive(snooze.id))
    }

    private fun oneShotSchedule(suffix: String): WakeSchedule {
        val target = ZonedDateTime.now().plusMinutes(20).withNano(0)
        return WakeSchedule(
            id = WakeScheduleId("instrumented-$suffix-${System.nanoTime()}"),
            zoneId = target.zone,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = System.currentTimeMillis().coerceAtLeast(1),
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
        )
    }
}
