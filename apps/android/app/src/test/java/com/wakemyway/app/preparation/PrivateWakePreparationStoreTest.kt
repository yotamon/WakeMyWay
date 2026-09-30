package com.wakemyway.app.preparation

import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.preparation.TomorrowContractId
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.io.DataOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateWakePreparationStoreTest {
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `voice context consent round trips in private storage v2`() {
        val store = store()
        val contract = TomorrowContract(
            id = TomorrowContractId("contract"),
            wakeOccurrenceId = WakeOccurrenceId("occurrence"),
            rawText = "Interview at ten",
            firstMove = "Take a shower",
            useInVoiceCheckIn = true,
            createdAtEpochMillis = 1L,
        )

        store.writeContract(contract)

        assertEquals(contract, store.readContract())
        assertTrue(requireNotNull(store.readContract()).useInVoiceCheckIn)
    }

    @Test
    fun `legacy v1 contract migrates with voice sharing disabled`() {
        val fileName = "legacy-${System.nanoTime()}.bin"
        val directory = File(context.noBackupFilesDir, "wake-preparation").apply { mkdirs() }
        val file = File(directory, fileName)
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(0x574D5743)
            output.writeInt(1)
            output.writeUTF("legacy-contract")
            output.writeUTF("legacy-occurrence")
            output.writeUTF("Private reason")
            output.writeBoolean(true)
            output.writeUTF("Open curtains")
            output.writeLong(3L)
            output.writeLong(10L)
            output.writeLong(20L)
        }

        val contract = PrivateWakePreparationStore(
            context = context,
            contractFileName = fileName,
            planFileName = "unused-${System.nanoTime()}.bin",
        ).readContract()

        assertEquals("Private reason", contract?.rawText)
        assertEquals("Open curtains", contract?.firstMove)
        assertFalse(requireNotNull(contract).useInVoiceCheckIn)
    }

    private fun store() = PrivateWakePreparationStore(
        context = context,
        contractFileName = "contract-${System.nanoTime()}.bin",
        planFileName = "plan-${System.nanoTime()}.bin",
    )
}
