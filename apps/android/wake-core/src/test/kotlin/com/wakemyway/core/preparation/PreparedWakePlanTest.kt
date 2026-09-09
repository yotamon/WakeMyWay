package com.wakemyway.core.preparation

import com.wakemyway.core.character.CharacterId
import com.wakemyway.core.schedule.WakeOccurrenceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PreparedWakePlanTest {
    private val factory = PreparedWakePlanFactory()

    @Test
    fun `contract requires bounded trimmed private text`() {
        assertFailsWith<IllegalArgumentException> {
            contract(rawText = "  interview tomorrow  ")
        }
        assertFailsWith<IllegalArgumentException> {
            contract(rawText = "x".repeat(TomorrowContract.MAX_RAW_TEXT_CHARACTERS + 1))
        }
        assertFailsWith<IllegalArgumentException> {
            contract(firstMove = "  shower  ")
        }
    }

    @Test
    fun `preparation is deterministic for the same inputs and timestamp`() {
        val contract = contract(
            rawText = "I have an interview at ten and want enough time to shower and eat.",
            firstMove = "Shower",
        )

        val first = factory.prepare(contract, ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000)
        val replayed = factory.prepare(contract, ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000)

        assertEquals(first, replayed)
        assertTrue(first.hasValidChecksum())
        assertEquals(contract.wakeOccurrenceId, first.binding.wakeOccurrenceId)
        assertEquals(contract.wakeScheduleRevision, first.binding.wakeScheduleRevision)
        assertEquals(contract.id, first.binding.contractId)
        assertEquals(contract.updatedAtEpochMillis, first.binding.contractUpdatedAtEpochMillis)
        assertEquals("Shower", first.firstMove)
    }

    @Test
    fun `preparation minimizes whitespace and bounds orientation context`() {
        val long = buildString {
            repeat(80) { index ->
                append("important-$index   ")
                if (index % 5 == 0) append('\n')
            }
        }.trim()
        val contract = contract(rawText = long.take(TomorrowContract.MAX_RAW_TEXT_CHARACTERS))

        val plan = factory.prepare(contract, ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000)

        assertTrue(plan.orientationContext.length <= PreparedWakePlan.MAX_ORIENTATION_CONTEXT_CHARACTERS)
        assertTrue("  " !in plan.orientationContext)
        assertTrue('\n' !in plan.orientationContext)
        assertTrue(plan.hasValidChecksum())
    }

    @Test
    fun `checksum detects any prepared payload mutation`() {
        val plan = factory.prepare(contract(), ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000)

        val corrupted = plan.copy(orientationContext = plan.orientationContext + " changed")

        assertTrue(!corrupted.hasValidChecksum())
        assertIs<PreparedWakePlanResolution.InvalidIntegrity>(
            PreparedWakePlanResolver.resolve(corrupted, plan.binding),
        )
    }

    @Test
    fun `resolver rejects plan for another occurrence`() {
        val expectedContract = contract(occurrenceId = "wake-a")
        val otherContract = contract(occurrenceId = "wake-b")
        val expectedBinding = factory.prepare(
            expectedContract,
            ALFRED,
            characterVersion = 1,
            preparedAtEpochMillis = 2_000,
        ).binding
        val otherPlan = factory.prepare(
            otherContract,
            ALFRED,
            characterVersion = 1,
            preparedAtEpochMillis = 2_000,
        )

        assertEquals(
            PreparedWakePlanResolution.Stale(PreparedWakePlanResolution.StaleReason.OCCURRENCE),
            PreparedWakePlanResolver.resolve(otherPlan, expectedBinding),
        )
    }

    @Test
    fun `resolver rejects stale contract revision`() {
        val oldContract = contract(updatedAt = 1_000)
        val newContract = oldContract.copy(updatedAtEpochMillis = 1_500)
        val oldPlan = factory.prepare(oldContract, ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000)
        val expectedBinding = factory.prepare(
            newContract,
            ALFRED,
            characterVersion = 1,
            preparedAtEpochMillis = 2_500,
        ).binding

        assertEquals(
            PreparedWakePlanResolution.Stale(PreparedWakePlanResolution.StaleReason.CONTRACT),
            PreparedWakePlanResolver.resolve(oldPlan, expectedBinding),
        )
    }

    @Test
    fun `resolver rejects stale character version`() {
        val contract = contract()
        val oldPlan = factory.prepare(contract, ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000)
        val expectedBinding = oldPlan.binding.copy(characterVersion = 2)

        assertEquals(
            PreparedWakePlanResolution.Stale(PreparedWakePlanResolution.StaleReason.CHARACTER),
            PreparedWakePlanResolver.resolve(oldPlan, expectedBinding),
        )
    }

    @Test
    fun `resolver returns available only for an exact valid binding`() {
        val plan = factory.prepare(contract(), ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000)

        assertEquals(
            PreparedWakePlanResolution.Available(plan),
            PreparedWakePlanResolver.resolve(plan, plan.binding),
        )
    }

    @Test
    fun `missing plan resolves to explicit generic fallback state`() {
        val binding = factory.prepare(contract(), ALFRED, characterVersion = 1, preparedAtEpochMillis = 2_000).binding

        assertIs<PreparedWakePlanResolution.Missing>(PreparedWakePlanResolver.resolve(null, binding))
    }

    @Test
    fun `plan cannot be prepared before latest contract revision`() {
        assertFailsWith<IllegalArgumentException> {
            factory.prepare(
                contract(updatedAt = 1_500),
                ALFRED,
                characterVersion = 1,
                preparedAtEpochMillis = 1_499,
            )
        }
    }

    private fun contract(
        occurrenceId: String = "wake-1",
        rawText: String = "Interview at ten. I want time to shower and eat.",
        firstMove: String? = "Shower",
        updatedAt: Long = 1_000,
    ): TomorrowContract = TomorrowContract(
        id = TomorrowContractId("contract-1"),
        wakeOccurrenceId = WakeOccurrenceId(occurrenceId),
        wakeScheduleRevision = 7,
        rawText = rawText,
        firstMove = firstMove,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = updatedAt,
    )

    private companion object {
        val ALFRED = CharacterId("alfred")
    }
}
