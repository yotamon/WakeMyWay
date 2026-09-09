package com.wakemyway.core.preparation

import com.wakemyway.core.character.AlfredCharacter
import com.wakemyway.core.character.CharacterId
import com.wakemyway.core.character.WakeLineKey
import com.wakemyway.core.runtime.SpeechIntent
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@JvmInline
value class TomorrowContractId(val value: String) {
    init {
        require(value.isNotBlank()) { "TomorrowContractId must not be blank" }
    }
}

@JvmInline
value class PreparedWakePlanId(val value: String) {
    init {
        require(value.isNotBlank()) { "PreparedWakePlanId must not be blank" }
    }
}

data class TomorrowContract(
    val id: TomorrowContractId,
    val wakeOccurrenceId: WakeOccurrenceId,
    val rawText: String,
    val firstMove: String? = null,
    val revision: Long = 1,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
) {
    init {
        require(rawText.isNotBlank()) { "Tomorrow Contract text must not be blank" }
        require(rawText.length <= MAX_RAW_TEXT_CHARACTERS) { "Tomorrow Contract text is too long" }
        require(firstMove == null || firstMove.isNotBlank()) { "First Move must be null or non-blank" }
        require(firstMove == null || firstMove.length <= MAX_FIRST_MOVE_CHARACTERS) { "First Move is too long" }
        require(revision > 0) { "Tomorrow Contract revision must be positive" }
        require(createdAtEpochMillis > 0) { "Tomorrow Contract creation time must be positive" }
        require(updatedAtEpochMillis >= createdAtEpochMillis) { "Tomorrow Contract update time cannot precede creation" }
    }

    companion object {
        const val MAX_RAW_TEXT_CHARACTERS = 1_200
        const val MAX_FIRST_MOVE_CHARACTERS = 120
    }
}

data class PreparedWakePlan(
    val id: PreparedWakePlanId,
    val wakeOccurrenceId: WakeOccurrenceId,
    val formatVersion: Int,
    val sourceContractId: TomorrowContractId,
    val sourceContractRevision: Long,
    val characterId: CharacterId,
    val characterVersion: Int,
    val orientationLeadIn: String,
    val reminderLine: String,
    val firstMoveLine: String?,
    val fallbackLines: List<String>,
    val preparedAtEpochMillis: Long,
    val checksum: String,
) {
    init {
        require(formatVersion > 0) { "Prepared Wake Plan format version must be positive" }
        require(sourceContractRevision > 0) { "Prepared Wake Plan source revision must be positive" }
        require(characterVersion > 0) { "Prepared Wake Plan character version must be positive" }
        require(orientationLeadIn.isNotBlank()) { "Prepared Wake Plan orientation line must not be blank" }
        require(reminderLine.isNotBlank()) { "Prepared Wake Plan reminder line must not be blank" }
        require(firstMoveLine == null || firstMoveLine.isNotBlank()) { "Prepared Wake Plan First Move line must be null or non-blank" }
        require(fallbackLines.isNotEmpty()) { "Prepared Wake Plan needs a local fallback line" }
        require(fallbackLines.all { it.isNotBlank() }) { "Prepared Wake Plan fallback lines must not be blank" }
        require(preparedAtEpochMillis > 0) { "Prepared Wake Plan preparation time must be positive" }
        require(CHECKSUM_PATTERN.matches(checksum)) { "Prepared Wake Plan checksum must be SHA-256 hex" }
    }

    companion object {
        private val CHECKSUM_PATTERN = Regex("[0-9a-f]{64}")
    }
}

enum class PreparedPlanInvalidReason {
    UNSUPPORTED_VERSION,
    WRONG_OCCURRENCE,
    STALE_SOURCE,
    CHECKSUM_MISMATCH,
}

sealed interface PreparedPlanValidation {
    data class Valid(val plan: PreparedWakePlan) : PreparedPlanValidation

    data class Invalid(val reason: PreparedPlanInvalidReason) : PreparedPlanValidation
}

object PreparedWakePlanPreparer {
    const val FORMAT_VERSION = 1

    val genericFallbackLines: List<String> = listOf(
        "Good morning. Start with one small move.",
        "Sit up first. The rest can wait a moment.",
    )

    fun prepare(
        contract: TomorrowContract,
        preparedAtEpochMillis: Long,
        characterId: CharacterId = AlfredCharacter.spec.id,
        characterVersion: Int = AlfredCharacter.spec.version,
    ): PreparedWakePlan {
        require(preparedAtEpochMillis > 0) { "Preparation time must be positive" }
        require(characterVersion > 0) { "Character version must be positive" }

        val normalizedReminder = normalize(contract.rawText)
        val reminderContent = bound(normalizedReminder, MAX_PREPARED_REMINDER_CONTENT)
        val reminderLine = "You asked me to remember: $reminderContent"
        val normalizedFirstMove = contract.firstMove?.let(::normalize)?.takeIf { it.isNotBlank() }
        val firstMoveLine = normalizedFirstMove?.let { "First move: ${bound(it, MAX_PREPARED_FIRST_MOVE_CONTENT)}" }
        val orientationLeadIn = AlfredCharacter.render(
            intent = SpeechIntent.Orientation,
            key = WakeLineKey("prepared:${contract.wakeOccurrenceId.value}:${contract.revision}"),
        ).text

        val id = PreparedWakePlanId("plan:${contract.wakeOccurrenceId.value}:${contract.revision}")
        val checksum = checksumFor(
            id = id,
            wakeOccurrenceId = contract.wakeOccurrenceId,
            formatVersion = FORMAT_VERSION,
            sourceContractId = contract.id,
            sourceContractRevision = contract.revision,
            characterId = characterId,
            characterVersion = characterVersion,
            orientationLeadIn = orientationLeadIn,
            reminderLine = reminderLine,
            firstMoveLine = firstMoveLine,
            fallbackLines = genericFallbackLines,
            preparedAtEpochMillis = preparedAtEpochMillis,
        )

        return PreparedWakePlan(
            id = id,
            wakeOccurrenceId = contract.wakeOccurrenceId,
            formatVersion = FORMAT_VERSION,
            sourceContractId = contract.id,
            sourceContractRevision = contract.revision,
            characterId = characterId,
            characterVersion = characterVersion,
            orientationLeadIn = orientationLeadIn,
            reminderLine = reminderLine,
            firstMoveLine = firstMoveLine,
            fallbackLines = genericFallbackLines,
            preparedAtEpochMillis = preparedAtEpochMillis,
            checksum = checksum,
        )
    }

    fun validate(
        plan: PreparedWakePlan,
        contract: TomorrowContract,
    ): PreparedPlanValidation {
        if (plan.formatVersion != FORMAT_VERSION) {
            return PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.UNSUPPORTED_VERSION)
        }
        if (plan.wakeOccurrenceId != contract.wakeOccurrenceId) {
            return PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.WRONG_OCCURRENCE)
        }
        if (plan.sourceContractId != contract.id || plan.sourceContractRevision != contract.revision) {
            return PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.STALE_SOURCE)
        }

        val expected = checksumFor(
            id = plan.id,
            wakeOccurrenceId = plan.wakeOccurrenceId,
            formatVersion = plan.formatVersion,
            sourceContractId = plan.sourceContractId,
            sourceContractRevision = plan.sourceContractRevision,
            characterId = plan.characterId,
            characterVersion = plan.characterVersion,
            orientationLeadIn = plan.orientationLeadIn,
            reminderLine = plan.reminderLine,
            firstMoveLine = plan.firstMoveLine,
            fallbackLines = plan.fallbackLines,
            preparedAtEpochMillis = plan.preparedAtEpochMillis,
        )
        if (plan.checksum != expected) {
            return PreparedPlanValidation.Invalid(PreparedPlanInvalidReason.CHECKSUM_MISMATCH)
        }
        return PreparedPlanValidation.Valid(plan)
    }

    private fun checksumFor(
        id: PreparedWakePlanId,
        wakeOccurrenceId: WakeOccurrenceId,
        formatVersion: Int,
        sourceContractId: TomorrowContractId,
        sourceContractRevision: Long,
        characterId: CharacterId,
        characterVersion: Int,
        orientationLeadIn: String,
        reminderLine: String,
        firstMoveLine: String?,
        fallbackLines: List<String>,
        preparedAtEpochMillis: Long,
    ): String {
        val canonical = buildString {
            field("id", id.value)
            field("occurrence", wakeOccurrenceId.value)
            field("format", formatVersion.toString())
            field("contract", sourceContractId.value)
            field("contractRevision", sourceContractRevision.toString())
            field("character", characterId.value)
            field("characterVersion", characterVersion.toString())
            field("orientation", orientationLeadIn)
            field("reminder", reminderLine)
            field("firstMove", firstMoveLine.orEmpty())
            field("fallbackCount", fallbackLines.size.toString())
            fallbackLines.forEachIndexed { index, line -> field("fallback$index", line) }
            field("preparedAt", preparedAtEpochMillis.toString())
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun StringBuilder.field(name: String, value: String) {
        append(name.length).append(':').append(name)
        append(value.length).append(':').append(value)
        append('|')
    }

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    private fun bound(value: String, maxCharacters: Int): String {
        if (value.length <= maxCharacters) return value
        return value.take(maxCharacters - 1).trimEnd() + "…"
    }

    private const val MAX_PREPARED_REMINDER_CONTENT = 180
    private const val MAX_PREPARED_FIRST_MOVE_CONTENT = 100
}
