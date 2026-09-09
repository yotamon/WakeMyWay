package com.wakemyway.app.preparation

import android.content.Context
import android.util.AtomicFile
import com.wakemyway.core.character.CharacterId
import com.wakemyway.core.preparation.PreparedWakePlan
import com.wakemyway.core.preparation.PreparedWakePlanId
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.preparation.TomorrowContractId
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Owns the private M6 persistence boundary.
 *
 * Files live in the app's normal credential-protected no-backup directory. This store intentionally
 * refuses device-protected contexts so Tomorrow Contract/private prepared content cannot drift into
 * the Direct-Boot critical-data boundary.
 */
class PrivateWakePreparationStore(
    context: Context,
    contractFileName: String = CONTRACT_FILE_NAME,
    planFileName: String = PLAN_FILE_NAME,
) {
    private val contractFile: AtomicFile
    private val planFile: AtomicFile

    init {
        require(!context.isDeviceProtectedStorage) {
            "Private wake preparation must never use device-protected storage"
        }
        require(contractFileName.isNotBlank()) { "Contract file name must not be blank" }
        require(planFileName.isNotBlank()) { "Plan file name must not be blank" }

        val baseDir = File(context.applicationContext.noBackupFilesDir, DIRECTORY_NAME).apply {
            check(exists() || mkdirs()) { "Could not create private wake preparation directory" }
        }
        contractFile = AtomicFile(File(baseDir, contractFileName))
        planFile = AtomicFile(File(baseDir, planFileName))
    }

    fun readContract(): TomorrowContract? = readAtomic(contractFile) { input ->
        requireHeader(input, CONTRACT_MAGIC)
        TomorrowContract(
            id = TomorrowContractId(input.readUTF()),
            wakeOccurrenceId = WakeOccurrenceId(input.readUTF()),
            rawText = input.readUTF(),
            firstMove = input.readNullableUtf(),
            revision = input.readLong(),
            createdAtEpochMillis = input.readLong(),
            updatedAtEpochMillis = input.readLong(),
        )
    }

    fun writeContract(contract: TomorrowContract) {
        writeAtomic(contractFile) { output ->
            writeHeader(output, CONTRACT_MAGIC)
            output.writeUTF(contract.id.value)
            output.writeUTF(contract.wakeOccurrenceId.value)
            output.writeUTF(contract.rawText)
            output.writeNullableUtf(contract.firstMove)
            output.writeLong(contract.revision)
            output.writeLong(contract.createdAtEpochMillis)
            output.writeLong(contract.updatedAtEpochMillis)
        }
    }

    fun readPlan(): PreparedWakePlan? = readAtomic(planFile) { input ->
        requireHeader(input, PLAN_MAGIC)
        PreparedWakePlan(
            id = PreparedWakePlanId(input.readUTF()),
            wakeOccurrenceId = WakeOccurrenceId(input.readUTF()),
            formatVersion = input.readInt(),
            sourceContractId = TomorrowContractId(input.readUTF()),
            sourceContractRevision = input.readLong(),
            characterId = CharacterId(input.readUTF()),
            characterVersion = input.readInt(),
            orientationLeadIn = input.readUTF(),
            reminderLine = input.readUTF(),
            firstMoveLine = input.readNullableUtf(),
            fallbackLines = List(input.readBoundedCount(MAX_FALLBACK_LINES)) { input.readUTF() },
            preparedAtEpochMillis = input.readLong(),
            checksum = input.readUTF(),
        )
    }

    fun writePlan(plan: PreparedWakePlan) {
        require(plan.fallbackLines.size <= MAX_FALLBACK_LINES) { "Too many Prepared Wake Plan fallback lines" }
        writeAtomic(planFile) { output ->
            writeHeader(output, PLAN_MAGIC)
            output.writeUTF(plan.id.value)
            output.writeUTF(plan.wakeOccurrenceId.value)
            output.writeInt(plan.formatVersion)
            output.writeUTF(plan.sourceContractId.value)
            output.writeLong(plan.sourceContractRevision)
            output.writeUTF(plan.characterId.value)
            output.writeInt(plan.characterVersion)
            output.writeUTF(plan.orientationLeadIn)
            output.writeUTF(plan.reminderLine)
            output.writeNullableUtf(plan.firstMoveLine)
            output.writeInt(plan.fallbackLines.size)
            plan.fallbackLines.forEach(output::writeUTF)
            output.writeLong(plan.preparedAtEpochMillis)
            output.writeUTF(plan.checksum)
        }
    }

    fun clearContract() {
        contractFile.delete()
    }

    fun clearPlan() {
        planFile.delete()
    }

    fun clearAll() {
        clearContract()
        clearPlan()
    }

    private fun <T> readAtomic(file: AtomicFile, decode: (DataInputStream) -> T): T? {
        if (!file.baseFile.exists()) return null
        return file.openRead().use { stream ->
            DataInputStream(BufferedInputStream(stream)).use(decode)
        }
    }

    private fun writeAtomic(file: AtomicFile, encode: (DataOutputStream) -> Unit) {
        val stream = file.startWrite()
        val output = DataOutputStream(stream)
        try {
            encode(output)
            output.flush()
            file.finishWrite(stream)
        } catch (error: Throwable) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun requireHeader(input: DataInputStream, expectedMagic: Int) {
        check(input.readInt() == expectedMagic) { "Unexpected private wake preparation file type" }
        check(input.readInt() == STORAGE_SCHEMA_VERSION) { "Unsupported private wake preparation storage version" }
    }

    private fun writeHeader(output: DataOutputStream, magic: Int) {
        output.writeInt(magic)
        output.writeInt(STORAGE_SCHEMA_VERSION)
    }

    private fun DataInputStream.readNullableUtf(): String? = if (readBoolean()) readUTF() else null

    private fun DataOutputStream.writeNullableUtf(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    private fun DataInputStream.readBoundedCount(max: Int): Int {
        val count = readInt()
        check(count in 0..max) { "Invalid bounded list size" }
        return count
    }

    companion object {
        private const val DIRECTORY_NAME = "wake-preparation"
        private const val CONTRACT_FILE_NAME = "tomorrow-contract-v1.bin"
        private const val PLAN_FILE_NAME = "prepared-wake-plan-v1.bin"
        private const val STORAGE_SCHEMA_VERSION = 1
        private const val CONTRACT_MAGIC = 0x574D5743 // WMWC
        private const val PLAN_MAGIC = 0x574D5750 // WMWP
        private const val MAX_FALLBACK_LINES = 16
    }
}
