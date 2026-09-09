package com.wakemyway.app.preparation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.wakemyway.core.preparation.PreparedPlanValidation
import com.wakemyway.core.preparation.PreparedWakePlanPreparer
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.preparation.TomorrowContractId
import com.wakemyway.core.schedule.WakeOccurrenceId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivateWakePreparationStoreInstrumentedTest {
    private lateinit var context: Context
    private lateinit var store: PrivateWakePreparationStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val suffix = System.nanoTime().toString()
        store = PrivateWakePreparationStore(
            context = context,
            contractFileName = "contract-instrumented-$suffix.bin",
            planFileName = "plan-instrumented-$suffix.bin",
        )
    }

    @After
    fun tearDown() {
        store.clearAll()
    }

    @Test
    fun credentialProtectedRoundTripPreservesContractAndValidatedPlan() {
        val contract = contract("instrumented-occurrence")
        val plan = PreparedWakePlanPreparer.prepare(
            contract = contract,
            preparedAtEpochMillis = 1_800_000_020_000L,
        )

        store.writeContract(contract)
        store.writePlan(plan)

        assertEquals(contract, store.readContract())
        assertEquals(plan, store.readPlan())
        val validation = PreparedWakePlanPreparer.validate(store.readPlan()!!, store.readContract()!!)
        assertTrue(validation is PreparedPlanValidation.Valid)
        assertEquals(plan, (validation as PreparedPlanValidation.Valid).plan)
    }

    @Test
    fun tamperedPreparedPlanFallsBackAtWakeTime() {
        val contract = contract("tamper-occurrence")
        val tampered = PreparedWakePlanPreparer
            .prepare(contract, 1_800_000_020_000L)
            .copy(reminderLine = "Modified after preparation")
        store.writeContract(contract)
        store.writePlan(tampered)

        val content = WakePreparationManager(context, store).loadForWake(contract.wakeOccurrenceId)

        assertTrue(content is WakeTimePreparedContent.GenericFallback)
        content as WakeTimePreparedContent.GenericFallback
        assertEquals(WakePreparationStatus.INVALID_OR_STALE, content.reason)
        assertEquals(PreparedWakePlanPreparer.genericFallbackLines, content.lines)
    }

    @Test
    fun clearRemovesBothPrivateFiles() {
        val contract = contract("clear-occurrence")
        store.writeContract(contract)
        store.writePlan(PreparedWakePlanPreparer.prepare(contract, 1_800_000_020_000L))

        store.clearAll()

        assertNull(store.readContract())
        assertNull(store.readPlan())
    }

    @Test
    fun deviceProtectedContextIsRejected() {
        val deviceProtectedContext = context.createDeviceProtectedStorageContext()

        try {
            PrivateWakePreparationStore(
                context = deviceProtectedContext,
                contractFileName = "forbidden-contract.bin",
                planFileName = "forbidden-plan.bin",
            )
            fail("Private wake preparation must reject device-protected storage")
        } catch (_: IllegalArgumentException) {
            // Expected: raw/private wake context must never enter the Direct-Boot storage boundary.
        }
    }

    @Test
    fun workManagerCanInitializeOnDemandFromApplicationConfiguration() {
        assertNotNull(WorkManager.getInstance(context))
    }

    private fun contract(occurrenceId: String): TomorrowContract = TomorrowContract(
        id = TomorrowContractId("contract:$occurrenceId"),
        wakeOccurrenceId = WakeOccurrenceId(occurrenceId),
        rawText = "Tomorrow I want to begin with a deliberate first step.",
        firstMove = "Open the curtains",
        revision = 3,
        createdAtEpochMillis = 1_800_000_000_000L,
        updatedAtEpochMillis = 1_800_000_010_000L,
    )
}
