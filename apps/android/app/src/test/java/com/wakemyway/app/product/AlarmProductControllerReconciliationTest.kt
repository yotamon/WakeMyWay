package com.wakemyway.app.product

import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmScheduleCompiler
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.assertEquals
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
class AlarmProductControllerReconciliationTest {
    private val now = Instant.parse("2026-09-26T04:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val productFileName = "alarm-product-reconcile-${System.nanoTime()}.json"
    private val criticalFileName = "critical-product-reconcile-${System.nanoTime()}.json"
    private val context
        get() = RuntimeEnvironment.getApplication()

    private lateinit var repository: AlarmDefinitionRepository
    private lateinit var kernel: AlarmKernel

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        repository = AlarmDefinitionRepository(context, productFileName)
        repository.clear()
        kernel = AlarmKernel(
            context = context,
            clock = clock,
            criticalStateFileName = criticalFileName,
        )
    }

    @After
    fun tearDown() {
        runCatching { kernel.cancelSchedule() }
        repository.clear()
        context.filesDir.resolve(productFileName).delete()
        context.filesDir.resolve("$productFileName.bak").delete()
    }

    @Test
    fun `startup reconcile restores enabled product alarm after critical slot was disabled`() {
        val alarm = recurringAlarm()
        repository.upsert(alarm)
        val controller = AlarmProductController(
            context = context,
            repository = repository,
            kernel = kernel,
            compiler = AlarmScheduleCompiler(),
            clock = clock,
        )

        val initial = controller.reconcile()
        val expectedOccurrence = requireNotNull(initial.nextOccurrence)
        assertEquals(alarm.id.value, expectedOccurrence.wakeScheduleId.value)

        kernel.cancelSchedule(WakeScheduleId(alarm.id.value))

        assertTrue(requireNotNull(repository.get(alarm.id)).enabled)
        assertNull(kernel.health(WakeScheduleId(alarm.id.value))?.nextOccurrence)

        val repaired = controller.reconcile()

        assertEquals(alarm.id.value, repaired.nextOccurrence?.wakeScheduleId?.value)
        assertTrue(requireNotNull(kernel.health(WakeScheduleId(alarm.id.value))).enabled)
        assertEquals(
            expectedOccurrence.scheduledAt,
            kernel.health(WakeScheduleId(alarm.id.value))?.nextOccurrence?.scheduledAt,
        )
    }

    private fun recurringAlarm(): AlarmDefinition {
        val createdAt = Instant.parse("2026-09-20T09:00:00Z")
        return AlarmDefinition(
            id = AlarmDefinitionId("persisted-after-update"),
            label = "Workday",
            enabled = true,
            zoneId = ZoneOffset.UTC,
            schedule = AlarmSchedulePattern.Weekly(
                days = setOf(DayOfWeek.MONDAY),
                time = LocalTime.of(7, 0),
            ),
            revision = 4,
            createdAt = createdAt,
            updatedAt = createdAt.plusSeconds(120),
        )
    }
}
