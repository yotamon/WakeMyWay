package com.wakemyway.app.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.io.File
import java.time.DayOfWeek
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
        fileName = "critical-wake-snapshot-instrumented-${System.nanoTime()}.json"
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
    fun roundTripPreservesOneShotPolicyAndCancellationTombstone() {
        val schedule = WakeSchedule(
            id = WakeScheduleId("instrumented-one-shot"),
            zoneId = ZoneId.of("Europe/Berlin"),
            timesByDay = mapOf(DayOfWeek.MONDAY to LocalTime.of(7, 30)),
            revision = 42,
            completionPolicy = WakeCompletionPolicy.ONE_SHOT,
        )
        val snapshot = CriticalWakeSnapshot(
            schedule = schedule,
            nextOccurrence = null,
            activeOccurrence = null,
            registeredOccurrenceId = null,
            generation = 3,
            enabled = false,
        )

        store.write(snapshot)
        val restored = store.read()
        val result = store.readResult()

        assertNotNull(restored)
        assertTrue(result is CriticalWakeReadResult.Snapshot)
        assertFalse(restored!!.enabled)
        assertEquals(3, restored.generation)
        assertEquals(WakeCompletionPolicy.ONE_SHOT, restored.schedule.completionPolicy)
        assertEquals(schedule.id, restored.schedule.id)
        assertEquals(schedule.zoneId, restored.schedule.zoneId)
        assertEquals(schedule.timesByDay, restored.schedule.timesByDay)
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
