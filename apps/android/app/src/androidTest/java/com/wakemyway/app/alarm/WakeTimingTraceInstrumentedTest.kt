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
        assertTrue(report.contains("NORMAL_T_PLUS_2M STOPPED"))
        assertTrue(report.contains("SERVICE_RECOVERED"))
        assertTrue(report.contains("instrumented-test"))
    }
}
