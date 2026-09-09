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
    val displayName: String,
    val voiceLocaleTag: String,
    val speechRate: Float,
    val pitch: Float,
) {
    init {
        require(displayName.isNotBlank())
        require(voiceLocaleTag.isNotBlank())
        require(speechRate in 0.5f..1.5f)
        require(pitch in 0.5f..1.5f)
    }
}

data class RenderedWakeLine(
    val characterId: CharacterId,
    val intent: SpeechIntent,
    val text: String,
    val variantIndex: Int,
) {
    init {
        require(text.isNotBlank())
        require(text.length <= MAX_WAKE_LINE_CHARACTERS) {
            "Wake lines must stay concise during sleep inertia"
        }
        require(variantIndex >= 0)
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
        require(id.isNotBlank())
        require(languageTag.isNotBlank())
    }
}

object OfflineVoiceSelector {
    fun select(
        candidates: Iterable<LocalVoiceCandidate>,
        preferredLanguageTag: String,
    ): LocalVoiceCandidate? {
        val local = candidates.filterNot { it.networkRequired }
        if (local.isEmpty()) return null

        val preferred = java.util.Locale.forLanguageTag(preferredLanguageTag)
        return local.maxWithOrNull(
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
