package com.wakemyway.core.preparation

import com.wakemyway.core.schedule.WakeOccurrenceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WakePreparationTest {
    @Test
    fun sameContractAndPreparationTimeProduceIdenticalPlan() {
        val contract = contract()

        val first = PreparedWakePlanPreparer.prepare(contract, PREPARED_AT)
        val second = PreparedWakePlanPreparer.prepare(contract, PREPARED_AT)

        assertEquals(first, second)
        assertIs<PreparedPlanValidation.Valid>(PreparedWakePlanPreparer.validate(first, contract))
    }

    @Test
    fun tomorrowContractInfluencesBoundedMorningReminder() {
        val contract = contract(
            rawText = "  The demo tomorrow matters because I want to show the product clearly.  ",
            firstMove = "Make coffee and open the deck",
        )

        val plan = PreparedWakePlanPreparer.prepare(contract, PREPARED_AT)

        assertTrue(plan.reminderLine.contains("The demo tomorrow matters"))
        assertEquals("First move: Make coffee and open the deck", plan.firstMoveLine)
        assertTrue(plan.reminderLine.length <= 210)
        assertTrue(plan.fallbackLines.all { it.isNotBlank() })
    }

    @Test
    fun changingPrivateIntentChangesPreparedChecksum() {
        val first = PreparedWakePlanPreparer.prepare(contract(rawText = "Call the venue after breakfast"), PREPARED_AT)
        val second = PreparedWakePlanPreparer.prepare(contract(rawText = "Review the notes after breakfast"), PREPARED_AT)

        assertNotEquals(first.reminderLine, second.reminderLine)
        assertNotEquals(first.checksum, second.checksum)
    }

    @Test
    fun tamperedPreparedContentFailsIntegrityValidation() {
        val contract = contract()
        val valid = PreparedWakePlanPreparer.prepare(contract, PREPARED_AT)
        val tampered = valid.copy(reminderLine = "This line was modified after preparation.")

        val validation = PreparedWakePlanPreparer.validate(tampered, contract)

        assertEquals(
            PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.CHECKSUM_MISMATCH),
            validation,
        )
    }

    @Test
    fun staleContractRevisionCannotReuseOldPreparedPlan() {
        val original = contract(revision = 1)
        val plan = PreparedWakePlanPreparer.prepare(original, PREPARED_AT)
        val edited = original.copy(
            rawText = "Edited intention",
            revision = 2,
            updatedAtEpochMillis = original.updatedAtEpochMillis + 1,
        )

        val validation = PreparedWakePlanPreparer.validate(plan, edited)

        assertEquals(
            PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.STALE_SOURCE),
            validation,
        )
    }

    @Test
    fun planCannotBeReusedForAnotherOccurrence() {
        val original = contract()
        val plan = PreparedWakePlanPreparer.prepare(original, PREPARED_AT)
        val anotherOccurrence = original.copy(
            wakeOccurrenceId = WakeOccurrenceId("occurrence-2"),
        )

        val validation = PreparedWakePlanPreparer.validate(plan, anotherOccurrence)

        assertEquals(
            PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.WRONG_OCCURRENCE),
            validation,
        )
    }

    @Test
    fun unsupportedPlanVersionFailsClosed() {
        val contract = contract()
        val valid = PreparedWakePlanPreparer.prepare(contract, PREPARED_AT)
        val unsupported = valid.copy(formatVersion = 99)

        val validation = PreparedWakePlanPreparer.validate(unsupported, contract)

        assertEquals(
            PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.UNSUPPORTED_VERSION),
            validation,
        )
    }

    private fun contract(
        rawText: String = "Tomorrow matters. I want to get up and start deliberately.",
        firstMove: String? = "Put my feet on the floor",
        revision: Long = 1,
    ): TomorrowContract = TomorrowContract(
        id = TomorrowContractId("contract:occurrence-1"),
        wakeOccurrenceId = WakeOccurrenceId("occurrence-1"),
        rawText = rawText,
        firstMove = firstMove,
        revision = revision,
        createdAtEpochMillis = CREATED_AT,
        updatedAtEpochMillis = CREATED_AT + revision - 1,
    )

    companion object {
        private const val CREATED_AT = 1_800_000_000_000L
        private const val PREPARED_AT = 1_800_003_600_000L
    }
}
