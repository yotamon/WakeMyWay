package com.wakemyway.core.character

import com.wakemyway.core.runtime.SpeechIntent

object AlfredCharacter {
    val spec = CharacterSpec(
        id = CharacterId("alfred"),
        version = 2,
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
        "Sit up, then tell me when you're sitting.",
        "One thing for now: sit up, then say you're there.",
        "Let us begin with sitting up. Tell me when it's done.",
        "Up to sitting, please. Then tell me you're with me.",
    )

    private val ASK_TO_MOVE = listOf(
        "Feet on the floor, then tell me when they're down.",
        "A little movement now. Feet down, then answer me.",
        "Next step: feet on the floor. Tell me when you're there.",
        "Let us introduce gravity. Feet down, then tell me.",
    )

    private val RE_ENGAGE = listOf(
        listOf(
            "Still with me? Sit up and answer me, please.",
            "A small reminder: we are waking now. Tell me you're here.",
            "Back with me, please. Make one movement and answer.",
            "Let us continue. Sit up, then tell me you're there.",
        ),
        listOf(
            "We are not quite done. Sit up and answer me.",
            "A firmer nudge now. Feet down, then tell me.",
            "Still here. Get upright, then tell me you're with me.",
            "Time to continue. One clear movement, then answer me.",
        ),
        listOf(
            "We need a proper movement now. Feet down, then answer.",
            "No more drifting for the moment. Sit up and answer me.",
            "Let us be decisive. Upright now, then tell me.",
            "The next move is yours. Feet down, then answer me.",
        ),
        listOf(
            "We are past gentle reminders. Feet down, then answer me.",
            "This is the firm version: upright now, then tell me.",
            "Feet on the floor now, please. Then answer me.",
            "We need action now. Make one clear movement, then answer.",
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
