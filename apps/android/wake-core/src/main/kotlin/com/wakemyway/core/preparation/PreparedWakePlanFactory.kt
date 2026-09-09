package com.wakemyway.core.preparation

import com.wakemyway.core.character.CharacterId

class PreparedWakePlanFactory {
    fun prepare(
        contract: TomorrowContract,
        characterId: CharacterId,
        characterVersion: Int,
        preparedAtEpochMillis: Long,
    ): PreparedWakePlan {
        require(characterVersion > 0) { "Character version must be positive" }
        require(preparedAtEpochMillis >= contract.updatedAtEpochMillis) {
            "Preparation cannot predate the contract revision"
        }

        val binding = PreparedWakePlanBinding(
            wakeOccurrenceId = contract.wakeOccurrenceId,
            wakeScheduleRevision = contract.wakeScheduleRevision,
            contractId = contract.id,
            contractUpdatedAtEpochMillis = contract.updatedAtEpochMillis,
            characterId = characterId,
            characterVersion = characterVersion,
        )
        val orientationContext = minimizeContext(contract.rawText)
        val id = PreparedWakePlanId(
            "${contract.wakeOccurrenceId.value}:${contract.id.value}:${characterId.value}:v$characterVersion",
        )
        val checksum = PreparedWakePlanChecksum.calculate(
            id = id,
            formatVersion = PreparedWakePlan.CURRENT_FORMAT_VERSION,
            binding = binding,
            orientationContext = orientationContext,
            firstMove = contract.firstMove,
            preparedAtEpochMillis = preparedAtEpochMillis,
        )

        return PreparedWakePlan(
            id = id,
            formatVersion = PreparedWakePlan.CURRENT_FORMAT_VERSION,
            binding = binding,
            orientationContext = orientationContext,
            firstMove = contract.firstMove,
            preparedAtEpochMillis = preparedAtEpochMillis,
            checksumSha256 = checksum,
        )
    }

    private fun minimizeContext(rawText: String): String {
        val normalized = rawText
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.length <= PreparedWakePlan.MAX_ORIENTATION_CONTEXT_CHARACTERS) return normalized

        val contentLimit = PreparedWakePlan.MAX_ORIENTATION_CONTEXT_CHARACTERS - ELLIPSIS.length
        val candidate = normalized.take(contentLimit)
        val lastWhitespace = candidate.indexOfLast { it.isWhitespace() }
        val cut = if (lastWhitespace >= MIN_WORD_BOUNDARY_INDEX) {
            candidate.take(lastWhitespace)
        } else {
            candidate
        }
        return cut.trimEnd(' ', ',', ';', ':', '-', '.') + ELLIPSIS
    }

    private companion object {
        const val MIN_WORD_BOUNDARY_INDEX = 80
        const val ELLIPSIS = "…"
    }
}
