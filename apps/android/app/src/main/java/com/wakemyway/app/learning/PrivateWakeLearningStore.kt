package com.wakemyway.app.learning

import android.content.Context
import android.util.AtomicFile
import com.wakemyway.core.learning.LearnedWakeProfile
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeFeedbackRating
import com.wakemyway.core.learning.WakeOutcomeId
import com.wakemyway.core.learning.WakeOutcomeRecord
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeSessionId
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.time.Duration

internal data class StoredWakeLearningState(
    val outcomes: List<WakeOutcomeRecord> = emptyList(),
    val profile: LearnedWakeProfile? = null,
)

/**
 * Credential-protected persistence for derived Wake Learning state.
 *
 * Learning is never required for alarm delivery or Direct Boot. The store therefore rejects a
 * device-protected Context and keeps its single atomic state file under noBackupFilesDir.
 */
class PrivateWakeLearningStore(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
) {
    private val stateFile: AtomicFile

    init {
        require(!context.isDeviceProtectedStorage) {
            "Wake Learning must never use device-protected storage"
        }
        require(fileName.isNotBlank()) { "Wake Learning file name must not be blank" }
        val baseDir = File(context.applicationContext.noBackupFilesDir, DIRECTORY_NAME).apply {
            check(exists() || mkdirs()) { "Could not create Wake Learning directory" }
        }
        stateFile = AtomicFile(File(baseDir, fileName))
    }

    internal fun readState(): StoredWakeLearningState {
        if (!stateFile.baseFile.exists()) return StoredWakeLearningState()
        return stateFile.openRead().use { stream ->
            DataInputStream(BufferedInputStream(stream)).use { input ->
                check(input.readInt() == MAGIC) { "Unexpected Wake Learning file type" }
                check(input.readInt() == STORAGE_SCHEMA_VERSION) { "Unsupported Wake Learning storage version" }
                val outcomes = List(input.readBoundedCount(MAX_OUTCOMES)) { input.readOutcome() }
                val profile = if (input.readBoolean()) input.readProfile() else null
                StoredWakeLearningState(outcomes = outcomes, profile = profile)
            }
        }
    }

    internal fun writeState(state: StoredWakeLearningState) {
        require(state.outcomes.size <= MAX_OUTCOMES) { "Too many Wake Outcomes" }
        val stream = stateFile.startWrite()
        val output = DataOutputStream(stream)
        try {
            output.writeInt(MAGIC)
            output.writeInt(STORAGE_SCHEMA_VERSION)
            output.writeInt(state.outcomes.size)
            state.outcomes.forEach { outcome -> output.writeOutcome(outcome) }
            output.writeBoolean(state.profile != null)
            state.profile?.let { profile -> output.writeProfile(profile) }
            output.flush()
            stateFile.finishWrite(stream)
        } catch (error: Throwable) {
            stateFile.failWrite(stream)
            throw error
        }
    }

    fun clear() {
        stateFile.delete()
    }

    private fun DataOutputStream.writeOutcome(outcome: WakeOutcomeRecord) {
        writeUTF(outcome.id.value)
        writeInt(outcome.derivationVersion)
        writeUTF(outcome.sessionId.value)
        writeInt(outcome.policyVersion)
        writeLong(outcome.startedAtEpochMillis)
        writeNullableDuration(outcome.firstEngagementAfter)
        writeNullableDuration(outcome.meaningfulMovementAfter)
        writeNullableDuration(outcome.activationCompletedAfter)
        writeUTF(outcome.terminalOutcome.name)
        writeInt(outcome.snoozeCount)
        writeInt(outcome.maxInterventionDepth)
        writeNullableEnum(outcome.calibration?.name)
        writeNullableInt(outcome.annoyance?.value)
        writeNullableInt(outcome.agency?.value)
    }

    private fun DataInputStream.readOutcome(): WakeOutcomeRecord = WakeOutcomeRecord(
        id = WakeOutcomeId(readUTF()),
        derivationVersion = readInt(),
        sessionId = WakeSessionId(readUTF()),
        policyVersion = readInt(),
        startedAtEpochMillis = readLong(),
        firstEngagementAfter = readNullableDuration(),
        meaningfulMovementAfter = readNullableDuration(),
        activationCompletedAfter = readNullableDuration(),
        terminalOutcome = WakeOutcome.valueOf(readUTF()),
        snoozeCount = readInt(),
        maxInterventionDepth = readInt(),
        calibration = readNullableEnum()?.let(WakeCalibration::valueOf),
        annoyance = readNullableInt()?.let(::WakeFeedbackRating),
        agency = readNullableInt()?.let(::WakeFeedbackRating),
    )

    private fun DataOutputStream.writeProfile(profile: LearnedWakeProfile) {
        writeInt(profile.profileVersion)
        writeInt(profile.learningAlgorithmVersion)
        writePolicy(profile.activePolicy)
        require(profile.sourceOutcomeIds.size <= MAX_PROFILE_SOURCES) { "Too many profile source outcomes" }
        writeInt(profile.sourceOutcomeIds.size)
        profile.sourceOutcomeIds.forEach { writeUTF(it.value) }
        writeUTF(profile.explanation)
        writeLong(profile.updatedAtEpochMillis)
    }

    private fun DataInputStream.readProfile(): LearnedWakeProfile = LearnedWakeProfile(
        profileVersion = readInt(),
        learningAlgorithmVersion = readInt(),
        activePolicy = readPolicy(),
        sourceOutcomeIds = List(readBoundedCount(MAX_PROFILE_SOURCES)) { WakeOutcomeId(readUTF()) },
        explanation = readUTF(),
        updatedAtEpochMillis = readLong(),
    )

    private fun DataOutputStream.writePolicy(policy: WakePolicy) {
        writeInt(policy.version)
        writeInt(policy.activationThreshold)
        writeInt(policy.meaningfulInteractionWeight)
        writeInt(policy.coherentVoiceWeight)
        writeInt(policy.devicePickupWeight)
        writeInt(policy.orientationChangeWeight)
        writeInt(policy.sustainedMovementWeight)
        writeInt(policy.maxEscalationLevel)
        writeLong(policy.movementPromptDelay.toMillis())
        writeLong(policy.defaultSnoozeDuration.toMillis())
        writeInt(policy.rememberedInputLimit)
    }

    private fun DataInputStream.readPolicy(): WakePolicy = WakePolicy(
        version = readInt(),
        activationThreshold = readInt(),
        meaningfulInteractionWeight = readInt(),
        coherentVoiceWeight = readInt(),
        devicePickupWeight = readInt(),
        orientationChangeWeight = readInt(),
        sustainedMovementWeight = readInt(),
        maxEscalationLevel = readInt(),
        movementPromptDelay = Duration.ofMillis(readLong()),
        defaultSnoozeDuration = Duration.ofMillis(readLong()),
        rememberedInputLimit = readInt(),
    )

    private fun DataOutputStream.writeNullableDuration(value: Duration?) {
        writeBoolean(value != null)
        if (value != null) writeLong(value.toMillis())
    }

    private fun DataInputStream.readNullableDuration(): Duration? =
        if (readBoolean()) Duration.ofMillis(readLong()) else null

    private fun DataOutputStream.writeNullableEnum(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    private fun DataInputStream.readNullableEnum(): String? = if (readBoolean()) readUTF() else null

    private fun DataOutputStream.writeNullableInt(value: Int?) {
        writeBoolean(value != null)
        if (value != null) writeInt(value)
    }

    private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null

    private fun DataInputStream.readBoundedCount(max: Int): Int {
        val count = readInt()
        check(count in 0..max) { "Invalid bounded Wake Learning list size" }
        return count
    }

    companion object {
        const val MAX_OUTCOMES = 30
        private const val MAX_PROFILE_SOURCES = 6
        private const val DIRECTORY_NAME = "wake-learning"
        private const val DEFAULT_FILE_NAME = "wake-learning-v1.bin"
        private const val STORAGE_SCHEMA_VERSION = 1
        private const val MAGIC = 0x574D574C // WMWL
    }
}
