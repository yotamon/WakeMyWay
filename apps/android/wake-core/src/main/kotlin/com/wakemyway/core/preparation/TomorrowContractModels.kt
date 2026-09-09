package com.wakemyway.core.preparation

import com.wakemyway.core.character.CharacterId
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@JvmInline
value class TomorrowContractId(val value: String) {
    init {
        require(value.isNotBlank()) { "Tomorrow Contract id must not be blank" }
    }
}

@JvmInline
value class PreparedWakePlanId(val value: String) {
    init {
        require(value.isNotBlank()) { "Prepared Wake Plan id must not be blank" }
    }
}

data class TomorrowContract(
    val id: TomorrowContractId,
    val wakeOccurrenceId: WakeOccurrenceId,
    val wakeScheduleRevision: Long,
    val rawText: String,
    val firstMove: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(wakeScheduleRevision > 0) { "Wake schedule revision must be positive" }
        require(rawText.isNotBlank()) { "Tomorrow Contract text must not be blank" }
        require(rawText == rawText.trim()) { "Tomorrow Contract text must be trimmed" }
        require(rawText.length <= MAX_RAW_TEXT_CHARACTERS) {
            "Tomorrow Contract text exceeds $MAX_RAW_TEXT_CHARACTERS characters"
        }
        require(firstMove == null || firstMove.isNotBlank()) { "First move must be null or non-blank" }
        require(firstMove == null || firstMove == firstMove.trim()) { "First move must be trimmed" }
        require(firstMove == null || firstMove.length <= MAX_FIRST_MOVE_CHARACTERS) {
            "First move exceeds $MAX_FIRST_MOVE_CHARACTERS characters"
        }
        require(createdAtEpochMillis > 0) { "Created timestamp must be positive" }
        require(updatedAtEpochMillis >= createdAtEpochMillis) {
            "Updated timestamp cannot precede creation"
        }
    }

    companion object {
        const val MAX_RAW_TEXT_CHARACTERS = 500
        const val MAX_FIRST_MOVE_CHARACTERS = 80
    }
}

data class PreparedWakePlanBinding(
    val wakeOccurrenceId: WakeOccurrenceId,
    val wakeScheduleRevision: Long,
    val contractId: TomorrowContractId,
    val contractUpdatedAtEpochMillis: Long,
    val characterId: CharacterId,
    val characterVersion: Int,
) {
    init {
        require(wakeScheduleRevision > 0) { "Wake schedule revision must be positive" }
        require(contractUpdatedAtEpochMillis > 0) { "Contract revision timestamp must be positive" }
        require(characterVersion > 0) { "Character version must be positive" }
    }
}

data class PreparedWakePlan(
    val id: PreparedWakePlanId,
    val formatVersion: Int,
    val binding: PreparedWakePlanBinding,
    val orientationContext: String,
    val firstMove: String?,
    val preparedAtEpochMillis: Long,
    val checksumSha256: String,
) {
    init {
        require(formatVersion == CURRENT_FORMAT_VERSION) {
            "Unsupported Prepared Wake Plan format $formatVersion"
        }
        require(orientationContext.isNotBlank()) { "Prepared orientation context must not be blank" }
        require(orientationContext == orientationContext.trim()) { "Prepared orientation context must be trimmed" }
        require(orientationContext.length <= MAX_ORIENTATION_CONTEXT_CHARACTERS) {
            "Prepared orientation context exceeds $MAX_ORIENTATION_CONTEXT_CHARACTERS characters"
        }
        require(firstMove == null || firstMove.isNotBlank()) { "Prepared first move must be null or non-blank" }
        require(firstMove == null || firstMove == firstMove.trim()) { "Prepared first move must be trimmed" }
        require(firstMove == null || firstMove.length <= TomorrowContract.MAX_FIRST_MOVE_CHARACTERS) {
            "Prepared first move exceeds the contract limit"
        }
        require(preparedAtEpochMillis > 0) { "Prepared timestamp must be positive" }
        require(checksumSha256.matches(SHA_256_PATTERN)) { "Prepared plan checksum must be lowercase SHA-256" }
    }

    fun expectedChecksum(): String = PreparedWakePlanChecksum.calculate(
        id = id,
        formatVersion = formatVersion,
        binding = binding,
        orientationContext = orientationContext,
        firstMove = firstMove,
        preparedAtEpochMillis = preparedAtEpochMillis,
    )

    fun hasValidChecksum(): Boolean = checksumSha256 == expectedChecksum()

    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val MAX_ORIENTATION_CONTEXT_CHARACTERS = 220
        private val SHA_256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

sealed interface PreparedWakePlanResolution {
    data class Available(val plan: PreparedWakePlan) : PreparedWakePlanResolution
    data object Missing : PreparedWakePlanResolution
    data object InvalidIntegrity : PreparedWakePlanResolution
    data class Stale(val reason: StaleReason) : PreparedWakePlanResolution

    enum class StaleReason {
        OCCURRENCE,
        SCHEDULE_REVISION,
        CONTRACT,
        CHARACTER,
    }
}

object PreparedWakePlanResolver {
    fun resolve(
        plan: PreparedWakePlan?,
        expected: PreparedWakePlanBinding,
    ): PreparedWakePlanResolution {
        if (plan == null) return PreparedWakePlanResolution.Missing
        if (!plan.hasValidChecksum()) return PreparedWakePlanResolution.InvalidIntegrity

        val actual = plan.binding
        return when {
            actual.wakeOccurrenceId != expected.wakeOccurrenceId ->
                PreparedWakePlanResolution.Stale(PreparedWakePlanResolution.StaleReason.OCCURRENCE)

            actual.wakeScheduleRevision != expected.wakeScheduleRevision ->
                PreparedWakePlanResolution.Stale(PreparedWakePlanResolution.StaleReason.SCHEDULE_REVISION)

            actual.contractId != expected.contractId ||
                actual.contractUpdatedAtEpochMillis != expected.contractUpdatedAtEpochMillis ->
                PreparedWakePlanResolution.Stale(PreparedWakePlanResolution.StaleReason.CONTRACT)

            actual.characterId != expected.characterId || actual.characterVersion != expected.characterVersion ->
                PreparedWakePlanResolution.Stale(PreparedWakePlanResolution.StaleReason.CHARACTER)

            else -> PreparedWakePlanResolution.Available(plan)
        }
    }
}

internal object PreparedWakePlanChecksum {
    fun calculate(
        id: PreparedWakePlanId,
        formatVersion: Int,
        binding: PreparedWakePlanBinding,
        orientationContext: String,
        firstMove: String?,
        preparedAtEpochMillis: Long,
    ): String {
        val canonical = buildString {
            append("id=").append(id.value).append('\n')
            append("formatVersion=").append(formatVersion).append('\n')
            append("occurrence=").append(binding.wakeOccurrenceId.value).append('\n')
            append("scheduleRevision=").append(binding.wakeScheduleRevision).append('\n')
            append("contractId=").append(binding.contractId.value).append('\n')
            append("contractUpdatedAt=").append(binding.contractUpdatedAtEpochMillis).append('\n')
            append("characterId=").append(binding.characterId.value).append('\n')
            append("characterVersion=").append(binding.characterVersion).append('\n')
            append("orientationContext=").append(orientationContext).append('\n')
            append("firstMove=").append(firstMove.orEmpty()).append('\n')
            append("preparedAt=").append(preparedAtEpochMillis)
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
