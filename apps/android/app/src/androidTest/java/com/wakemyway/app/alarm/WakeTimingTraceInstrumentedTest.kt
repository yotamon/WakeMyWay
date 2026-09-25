package com.wakemyway.app.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.core.schedule.NextWakeOccurrenceResolver
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Instant
import java.time.ZonedDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeTimingTraceInstrumentedTest {
    private lateinit var trace: WakeTimingTrace

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        trace = WakeTimingTrace(
            context = context,
            journalName = "wake-reliability-journal-test-${System.nanoTime()}",
        )
    }

    @After
    fun tearDown() {
        trace.clearHistory()
    }

    @Test
    fun physicalScenarioIdentitySurvivesJournalAndReport() {
        val target = ZonedDateTime.now().plusMinutes(15).withNano(0)
        val schedule = WakeSchedule(
            id = WakeScheduleId("physical-scenario-test"),
            zoneId = target.zone,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = System.currentTimeMillis().coerceAtLeast(1),
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
        )
        val occurrence = NextWakeOccurrenceResolver().resolve(schedule, Instant.now())

        trace.expected(
            occurrence = occurrence,
            scenario = WakeTimingTrace.SCENARIO_DIRECT_BOOT,
            expectFullScreen = true,
        )

        val snapshot = trace.history(limit = 1).single()

        assertEquals(WakeTimingTrace.SCENARIO_DIRECT_BOOT, snapshot.scenario)
        assertEquals(ReliabilityState.EXPECTED, snapshot.state(nowWallMillis = occurrence.scheduledAt.toInstant().toEpochMilli()))
        assertTrue(trace.reportText().contains("DIRECT_BOOT EXPECTED"))
    }

    @Test
    fun journalRetainsReplayableEventOrderAndHumanReadableReport() {
        val target = ZonedDateTime.now().plusMinutes(15).withNano(0)
        val schedule = WakeSchedule(
            id = WakeScheduleId("journal-test"),
            zoneId = target.zone,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = System.currentTimeMillis().coerceAtLeast(1),
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
        )
        val occurrence = NextWakeOccurrenceResolver().resolve(schedule, Instant.now())

        trace.expected(occurrence, WakeTimingTrace.SCENARIO_NORMAL_T_PLUS_2M, expectFullScreen = true)
        trace.capabilities(
            occurrenceId = occurrence.id,
            exactAlarmAllowed = true,
            notificationsAllowed = true,
            fullScreenIntentAllowed = true,
        )
        trace.receiver(occurrence)
        trace.foreground(occurrence.id)
        trace.audioStarted(occurrence.id)
        trace.uiVisible(occurrence.id)
        trace.serviceRecovered(occurrence.id)
        trace.reconciled(occurrence.id, "instrumented-test", wakeReadyAfter = true)
        trace.stopped(occurrence.id)

        val snapshot = trace.history(limit = 1).single()
        val eventTypes = snapshot.events.map { it.type }

        assertEquals(ReliabilityState.STOPPED, snapshot.state())
        assertEquals(1, snapshot.serviceRecoveryCount)
        assertEquals(
            listOf(
                "EXPECTED",
                "CAPABILITIES",
                "RECEIVER",
                "FOREGROUND",
                "AUDIO_STARTED",
                "UI_VISIBLE",
                "SERVICE_RECOVERED",
                "RECONCILED",
                "STOPPED",
            ),
            eventTypes,
        )

        val report = trace.reportText()
        assertTrue(report.contains("Wake My Way Reliability Report"))
        assertTrue(report.contains("build="))
        assertTrue(report.contains("app=com.wakemyway.app"))
        assertTrue(report.contains("NORMAL_T_PLUS_2M STOPPED"))
        assertTrue(report.contains("SERVICE_RECOVERED"))
        assertTrue(report.contains("instrumented-test"))
    }

    @Test
    fun presentationLossIsTerminalInvalidationInsteadOfMissedReceiver() {
        val target = ZonedDateTime.now().plusMinutes(15).withNano(0)
        val schedule = WakeSchedule(
            id = WakeScheduleId("invalidated-presentation-test"),
            zoneId = target.zone,
            timesByDay = mapOf(target.dayOfWeek to target.toLocalTime()),
            revision = System.currentTimeMillis().coerceAtLeast(1),
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
        )
        val occurrence = NextWakeOccurrenceResolver().resolve(schedule, Instant.now())

        trace.expected(
            occurrence = occurrence,
            scenario = WakeTimingTrace.SCENARIO_FULL_SCREEN_UNAVAILABLE,
            expectFullScreen = true,
        )
        trace.capabilities(
            occurrenceId = occurrence.id,
            exactAlarmAllowed = true,
            notificationsAllowed = true,
            fullScreenIntentAllowed = false,
        )
        trace.invalidated(occurrence.id, AlarmRepairTarget.FULL_SCREEN_INTENT)

        val snapshot = trace.history(limit = 1).single()
        val wellPastReceiverGrace =
            occurrence.scheduledAt.toInstant().toEpochMilli() + WakeTimingTrace.MISSED_RECEIVER_GRACE_MS + 1

        assertEquals(
            ReliabilityState.INVALIDATED,
            snapshot.state(nowWallMillis = wellPastReceiverGrace),
        )
        assertEquals("INVALIDATED", snapshot.terminalAction)
        assertEquals(
            listOf("EXPECTED", "CAPABILITIES", "INVALIDATED"),
            snapshot.events.map { it.type },
        )
        assertTrue(
            snapshot.events.last().detail?.contains("repairTarget=FULL_SCREEN_INTENT") == true,
        )

        val report = trace.reportText()
        assertTrue(report.contains("FULL_SCREEN_UNAVAILABLE INVALIDATED"))
        assertTrue(report.contains("terminal=INVALIDATED"))
        assertTrue(report.contains("repairTarget=FULL_SCREEN_INTENT"))
    }
}
