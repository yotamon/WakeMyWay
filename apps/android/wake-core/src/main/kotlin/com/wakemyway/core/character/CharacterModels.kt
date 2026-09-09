package com.wakemyway.core.character

import com.wakemyway.core.runtime.SpeechIntent

@JvmInline
value class CharacterId(val value: String) {
    init {
        require(value.isNotBlank()) { "Character id must not be blank" }
    }
}

@JvmInline
value class WakeLineKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Wake line key must not be blank" }
    }
}

data class CharacterSpec(
    val id: CharacterId,
    val version: Int,
    val displayName: String,
    val voiceLocaleTag: String,
    val speechRate: Float,
    val pitch: Float,
) {
    init {
        require(version > 0) { "Character version must be positive" }
        require(displayName.isNotBlank()) { "Character display name must not be blank" }
        require(voiceLocaleTag.isNotBlank()) { "Character voice locale must not be blank" }
        require(speechRate in 0.5f..1.5f) { "Character speech rate is outside the supported range" }
        require(pitch in 0.5f..1.5f) { "Character pitch is outside the supported range" }
    }
}

data class RenderedWakeLine(
    val characterId: CharacterId,
    val characterVersion: Int,
    val intent: SpeechIntent,
    val text: String,
    val variantIndex: Int,
) {
    init {
        require(characterVersion > 0) { "Rendered character version must be positive" }
        require(text.isNotBlank()) { "Wake line must not be blank" }
        require(text.length <= MAX_WAKE_LINE_CHARACTERS) {
            "Wake lines must stay concise during sleep inertia"
        }
        require(variantIndex >= 0) { "Wake line variant index must be non-negative" }
    }

    companion object {
        const val MAX_WAKE_LINE_CHARACTERS = 120
    }
}

data class LocalVoiceCandidate(
    val id: String,
    val languageTag: String,
    val networkRequired: Boolean,
    val quality: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "Local voice id must not be blank" }
        require(languageTag.isNotBlank()) { "Local voice language tag must not be blank" }
    }
}

object OfflineVoiceSelector {
    fun select(
        candidates: Iterable<LocalVoiceCandidate>,
        preferredLanguageTag: String,
    ): LocalVoiceCandidate? {
        val preferred = java.util.Locale.forLanguageTag(preferredLanguageTag)
        val eligible = candidates
            .filterNot { it.networkRequired }
            .filter { candidate ->
                localeScore(java.util.Locale.forLanguageTag(candidate.languageTag), preferred) > 0
            }
        if (eligible.isEmpty()) return null

        return eligible.maxWithOrNull(
            compareBy<LocalVoiceCandidate> {
                localeScore(java.util.Locale.forLanguageTag(it.languageTag), preferred)
            }.thenBy { it.quality }
                .thenByDescending { it.id },
        )
    }

    private fun localeScore(candidate: java.util.Locale, preferred: java.util.Locale): Int = when {
        candidate.toLanguageTag().equals(preferred.toLanguageTag(), ignoreCase = true) -> 3
        candidate.language.equals(preferred.language, ignoreCase = true) &&
            candidate.country.equals(preferred.country, ignoreCase = true) -> 2
        candidate.language.equals(preferred.language, ignoreCase = true) -> 1
        else -> 0
    }
}
