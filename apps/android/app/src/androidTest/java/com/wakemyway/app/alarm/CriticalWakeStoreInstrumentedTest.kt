package com.wakemyway.app.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalWakeStoreInstrumentedTest {
    private lateinit var context: Context
    private lateinit var store: CriticalWakeStore
    private lateinit var fileName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fileName = "critical-wake-state-instrumented-${System.nanoTime()}.json"
        store = CriticalWakeStore(
            context = context,
            fileName = fileName,
        )
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun roundTripPreservesIndependentSlotsAndExactOneShotDate() {
        val recurring = WakeSchedule(
            id = WakeScheduleId("workdays"),
            zoneId = ZoneId.of("Europe/Berlin"),
            timesByDay = mapOf(DayOfWeek.MONDAY to LocalTime.of(7, 30)),
            revision = 42,
            completionPolicy = WakeCompletionPolicy.RECURRING,
        )
        val flightDate = LocalDate.of(2026, 9, 22)
        val oneShot = WakeSchedule(
            id = WakeScheduleId("flight"),
            zoneId = ZoneId.of("Europe/Berlin"),
            timesByDay = mapOf(flightDate.dayOfWeek to LocalTime.of(5, 45)),
            revision = 3,
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
            oneShotDate = flightDate,
        )
        val state = CriticalAlarmState(
            slots = linkedMapOf(
                recurring.id to CriticalScheduleSlot(
                    schedule = recurring,
                    nextOccurrence = null,
                    registeredOccurrenceId = null,
                    enabled = false,
                ),
                oneShot.id to CriticalScheduleSlot(
                    schedule = oneShot,
                    nextOccurrence = null,
                    registeredOccurrenceId = null,
                    enabled = false,
                ),
            ),
            activeOccurrence = null,
            generation = 3,
        )

        store.write(state)
        val restored = store.read()
        val result = store.readResult()

        assertNotNull(restored)
        assertTrue(result is CriticalWakeReadResult.State)
        assertEquals(3, restored!!.generation)
        assertEquals(2, restored.slots.size)
        assertFalse(restored.slots.getValue(recurring.id).enabled)
        assertEquals(
            WakeCompletionPolicy.ONE_SHOT,
            restored.slots.getValue(oneShot.id).schedule.completionPolicy,
        )
        assertEquals(
            flightDate,
            restored.slots.getValue(oneShot.id).schedule.oneShotDate,
        )
    }

    @Test
    fun missingAndCorruptStateRemainFailClosedButDiagnosable() {
        assertEquals(CriticalWakeReadResult.Missing, store.readResult())
        assertNull(store.read())

        val protectedContext = context.createDeviceProtectedStorageContext()
        File(protectedContext.noBackupFilesDir, fileName).writeText("{ definitely-not-valid-json")

        val result = store.readResult()
        assertTrue(result is CriticalWakeReadResult.Corrupt)
        assertNull(store.read())
    }
}
