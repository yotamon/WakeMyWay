package com.wakemyway.core.character

import com.wakemyway.core.runtime.SpeechIntent

object AlfredCharacter {
    val spec = CharacterSpec(
        id = CharacterId("alfred"),
        version = 1,
        displayName = "Alfred",
        voiceLocaleTag = "en-GB",
        speechRate = 0.92f,
        pitch = 0.94f,
    )

    fun render(
        intent: SpeechIntent,
        key: WakeLineKey,
    ): RenderedWakeLine {
        val variants = variantsFor(intent)
        val index = stableVariantIndex(
            value = "${spec.id.value}|${spec.version}|${intent.catalogKey()}|${key.value}",
            variantCount = variants.size,
        )
        return RenderedWakeLine(
            characterId = spec.id,
            characterVersion = spec.version,
            intent = intent,
            text = variants[index],
            variantIndex = index,
        )
    }

    private fun variantsFor(intent: SpeechIntent): List<String> = when (intent) {
        SpeechIntent.InitialWake -> INITIAL_WAKE
        SpeechIntent.AskToSitUp -> ASK_TO_SIT_UP
        SpeechIntent.AskToMove -> ASK_TO_MOVE
        is SpeechIntent.ReEngage -> RE_ENGAGE[intent.escalationLevel.coerceIn(0, MAX_RE_ENGAGE_LEVEL)]
        SpeechIntent.SnoozeConfirmation -> SNOOZE_CONFIRMATION
        SpeechIntent.SnoozeFailed -> SNOOZE_FAILED
        SpeechIntent.Orientation -> ORIENTATION
    }

    private fun SpeechIntent.catalogKey(): String = when (this) {
        SpeechIntent.InitialWake -> "initial-wake"
        SpeechIntent.AskToSitUp -> "ask-to-sit-up"
        SpeechIntent.AskToMove -> "ask-to-move"
        is SpeechIntent.ReEngage -> "re-engage-${escalationLevel.coerceIn(0, MAX_RE_ENGAGE_LEVEL)}"
        SpeechIntent.SnoozeConfirmation -> "snooze-confirmation"
        SpeechIntent.SnoozeFailed -> "snooze-failed"
        SpeechIntent.Orientation -> "orientation"
    }

    private fun stableVariantIndex(
        value: String,
        variantCount: Int,
    ): Int {
        require(variantCount > 0) { "Alfred intent must have at least one line" }
        var hash = FNV_OFFSET_BASIS
        value.encodeToByteArray().forEach { byte ->
            hash = (hash xor byte.toUByte().toUInt()) * FNV_PRIME
        }
        return (hash % variantCount.toUInt()).toInt()
    }

    private const val MAX_RE_ENGAGE_LEVEL = 3
    private const val FNV_OFFSET_BASIS: UInt = 2166136261u
    private const val FNV_PRIME: UInt = 16777619u

    private val INITIAL_WAKE = listOf(
        "Good morning. It is time to begin.",
        "Morning. We have arrived at the waking part.",
        "Good morning. The day is, regrettably, on schedule.",
        "Morning. Shall we begin?",
    )

    private val ASK_TO_SIT_UP = listOf(
        "Sit up first, if you please.",
        "One thing for now: sit up.",
        "Let us begin with sitting up.",
        "Up to sitting, please. Nothing more yet.",
    )

    private val ASK_TO_MOVE = listOf(
        "Feet on the floor, if you please.",
        "A little movement now. Feet down.",
        "Next step: feet on the floor.",
        "Let us introduce gravity. Feet on the floor.",
    )

    private val RE_ENGAGE = listOf(
        listOf(
            "Still with me? Sit up, please.",
            "A small reminder: we are waking now.",
            "Back with me, please. One movement.",
            "Let us continue. Sit up.",
        ),
        listOf(
            "We are not quite done. Sit up, please.",
            "A firmer nudge now. Feet on the floor.",
            "Still here. Let us get you upright.",
            "Time to continue. One clear movement, please.",
        ),
        listOf(
            "We need a proper movement now. Feet on the floor.",
            "No more drifting for the moment. Sit up, please.",
            "Let us be decisive. Upright now, please.",
            "The next move is yours. Feet down.",
        ),
        listOf(
            "We are past gentle reminders. Feet on the floor, please.",
            "This is the firm version: upright now.",
            "Feet on the floor now, please.",
            "We need action now. Make one clear movement.",
        ),
    )

    private val SNOOZE_CONFIRMATION = listOf(
        "Very well. Confirm the snooze, if you please.",
        "Snooze is available. Confirm it if you want the pause.",
        "A short reprieve? Confirm it and I will step aside.",
        "If you want the snooze, confirm it now.",
    )

    private val SNOOZE_FAILED = listOf(
        "Snooze did not schedule. The alarm stays active.",
        "That snooze did not take. We are still waking.",
        "Snooze is unavailable just now. The alarm remains active.",
        "No snooze was scheduled. We will keep going.",
    )

    private val ORIENTATION = listOf(
        "Good. Take a moment and get your bearings.",
        "Good. Let us orient the morning.",
        "Now we can work out what comes first.",
        "That will do. Take a moment and find your morning.",
    )
}
