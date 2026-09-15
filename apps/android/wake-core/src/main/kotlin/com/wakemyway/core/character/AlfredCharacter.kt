package com.wakemyway.core.character

import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.runtime.SpeechIntent

object AlfredCharacter {
    val spec = CharacterSpec(
        id = CharacterId("alfred"),
        version = 4,
        displayName = "Alfred",
        voiceLocaleTag = "en-GB",
        speechRate = 0.92f,
        pitch = 0.94f,
    )

    fun render(
        intent: SpeechIntent,
        key: WakeLineKey,
        style: VoiceStyle = VoiceStyle.DEFAULT,
    ): RenderedWakeLine {
        val variants = variantsFor(intent, style)
        val index = stableVariantIndex(
            value = "${spec.id.value}|${spec.version}|${style.name}|${intent.catalogKey()}|${key.value}",
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

    private fun variantsFor(
        intent: SpeechIntent,
        style: VoiceStyle,
    ): List<String> = when (style) {
        VoiceStyle.DEFAULT -> defaultVariantsFor(intent)
        VoiceStyle.MOTIVATIONAL -> motivationalVariantsFor(intent)
        VoiceStyle.MINIMAL -> minimalVariantsFor(intent)
    }

    private fun defaultVariantsFor(intent: SpeechIntent): List<String> = when (intent) {
        SpeechIntent.InitialWake -> INITIAL_WAKE
        SpeechIntent.AskToSitUp -> ASK_TO_SIT_UP
        SpeechIntent.AskToMove -> ASK_TO_MOVE
        SpeechIntent.KeepEngaging -> KEEP_ENGAGING
        is SpeechIntent.ReEngage -> RE_ENGAGE[intent.escalationLevel.coerceIn(0, MAX_RE_ENGAGE_LEVEL)]
        SpeechIntent.SnoozeConfirmation -> SNOOZE_CONFIRMATION
        SpeechIntent.SnoozeFailed -> SNOOZE_FAILED
        SpeechIntent.Orientation -> ORIENTATION
    }

    private fun motivationalVariantsFor(intent: SpeechIntent): List<String> = when (intent) {
        SpeechIntent.InitialWake -> MOTIVATIONAL_INITIAL_WAKE
        SpeechIntent.AskToSitUp -> MOTIVATIONAL_ASK_TO_SIT_UP
        SpeechIntent.AskToMove -> MOTIVATIONAL_ASK_TO_MOVE
        SpeechIntent.KeepEngaging -> MOTIVATIONAL_KEEP_ENGAGING
        is SpeechIntent.ReEngage -> MOTIVATIONAL_RE_ENGAGE[
            intent.escalationLevel.coerceIn(0, MAX_RE_ENGAGE_LEVEL)
        ]
        SpeechIntent.SnoozeConfirmation -> MOTIVATIONAL_SNOOZE_CONFIRMATION
        SpeechIntent.SnoozeFailed -> MOTIVATIONAL_SNOOZE_FAILED
        SpeechIntent.Orientation -> MOTIVATIONAL_ORIENTATION
    }

    private fun minimalVariantsFor(intent: SpeechIntent): List<String> = when (intent) {
        SpeechIntent.InitialWake -> MINIMAL_INITIAL_WAKE
        SpeechIntent.AskToSitUp -> MINIMAL_ASK_TO_SIT_UP
        SpeechIntent.AskToMove -> MINIMAL_ASK_TO_MOVE
        SpeechIntent.KeepEngaging -> MINIMAL_KEEP_ENGAGING
        is SpeechIntent.ReEngage -> MINIMAL_RE_ENGAGE[
            intent.escalationLevel.coerceIn(0, MAX_RE_ENGAGE_LEVEL)
        ]
        SpeechIntent.SnoozeConfirmation -> MINIMAL_SNOOZE_CONFIRMATION
        SpeechIntent.SnoozeFailed -> MINIMAL_SNOOZE_FAILED
        SpeechIntent.Orientation -> MINIMAL_ORIENTATION
    }

    private fun SpeechIntent.catalogKey(): String = when (this) {
        SpeechIntent.InitialWake -> "initial-wake"
        SpeechIntent.AskToSitUp -> "ask-to-sit-up"
        SpeechIntent.AskToMove -> "ask-to-move"
        SpeechIntent.KeepEngaging -> "keep-engaging"
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

    private val KEEP_ENGAGING = listOf(
        "Good. Stay with me. One more clear move, then tell me.",
        "That's it. Keep moving, and tell me when you've done one more thing.",
        "Good. Keep the momentum. One more small move, then answer me.",
        "Still with you. Move once more, then tell me you're there.",
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

    private val MOTIVATIONAL_INITIAL_WAKE = listOf(
        "Morning. Let's make a clean start together.",
        "Good morning. One small win first, then the day can follow.",
        "Morning. You've got this. Let's begin with one clear move.",
    )

    private val MOTIVATIONAL_ASK_TO_SIT_UP = listOf(
        "Good start. Sit up, then tell me when you're there.",
        "You've got this. Sit up, then give me a quick answer.",
        "One strong first move: sit up, then tell me you're there.",
    )

    private val MOTIVATIONAL_ASK_TO_MOVE = listOf(
        "Nice. Feet on the floor next, then tell me they're down.",
        "Keep that start going. Feet down, then answer me.",
        "Good. Give me feet on the floor, then tell me when it's done.",
    )

    private val MOTIVATIONAL_KEEP_ENGAGING = listOf(
        "That's it. Keep the momentum. One more clear move, then answer me.",
        "Good work. One more small action, then tell me you're there.",
        "Keep it going. One more deliberate move, then answer me.",
    )

    private val MOTIVATIONAL_RE_ENGAGE = listOf(
        listOf(
            "Come back to me. One small move, then answer.",
            "We're still on track. Sit up and tell me you're here.",
            "Stay with it. Make one movement, then answer me.",
        ),
        listOf(
            "Keep going. Feet down, then tell me you're with me.",
            "You can do this. Get upright, then answer me.",
            "Let's keep the start alive. One clear move, then answer.",
        ),
        listOf(
            "Time for a decisive move. Feet down, then answer me.",
            "Stay with the morning. Sit up now, then answer.",
            "One strong move now. Get upright, then tell me.",
        ),
        listOf(
            "Let's finish the wake. Feet down now, then answer me.",
            "This is the firm nudge. Upright now, then tell me.",
            "One clear action now. Feet on the floor, then answer.",
        ),
    )

    private val MOTIVATIONAL_SNOOZE_CONFIRMATION = listOf(
        "Need the pause? Confirm snooze and I'll step aside.",
        "A short pause is available. Confirm it if that's your choice.",
        "If you want the snooze, confirm it and take the short reset.",
    )

    private val MOTIVATIONAL_SNOOZE_FAILED = listOf(
        "Snooze didn't schedule. Stay with me; we're still going.",
        "That pause didn't take. Keep moving while the alarm stays active.",
        "Snooze is unavailable. No problem; take the next small move.",
    )

    private val MOTIVATIONAL_ORIENTATION = listOf(
        "Good. You're making progress. Take a moment and find the first move.",
        "Nice work. Get your bearings, then choose what comes first.",
        "Good. Keep that momentum and find the next useful move.",
    )

    private val MINIMAL_INITIAL_WAKE = listOf(
        "Morning. Time to begin.",
        "Morning. Let's start.",
        "Good morning. Begin now.",
    )

    private val MINIMAL_ASK_TO_SIT_UP = listOf(
        "Sit up. Then answer.",
        "Sit up. Tell me when.",
        "Up to sitting. Then answer.",
    )

    private val MINIMAL_ASK_TO_MOVE = listOf(
        "Feet down. Then answer.",
        "Feet on the floor. Tell me.",
        "Move now. Feet down.",
    )

    private val MINIMAL_KEEP_ENGAGING = listOf(
        "One more move. Then answer.",
        "Keep moving. Then tell me.",
        "Another move. Stay with me.",
    )

    private val MINIMAL_RE_ENGAGE = listOf(
        listOf(
            "Still here. Sit up.",
            "Come back. Then answer.",
            "One move. Answer me.",
        ),
        listOf(
            "Sit up now. Answer.",
            "Feet down. Stay with me.",
            "Get upright. Then answer.",
        ),
        listOf(
            "Feet down now. Answer.",
            "Sit up. No drifting.",
            "Upright now. Then answer.",
        ),
        listOf(
            "Feet down. Answer now.",
            "Upright now. Tell me.",
            "Move now. Then answer.",
        ),
    )

    private val MINIMAL_SNOOZE_CONFIRMATION = listOf(
        "Confirm snooze if you want it.",
        "Want snooze? Confirm it.",
        "Confirm the snooze now.",
    )

    private val MINIMAL_SNOOZE_FAILED = listOf(
        "Snooze failed. Alarm stays on.",
        "No snooze. Keep going.",
        "Snooze unavailable. Alarm remains active.",
    )

    private val MINIMAL_ORIENTATION = listOf(
        "Good. Find your next move.",
        "Good. Get your bearings.",
        "Now choose what comes first.",
    )
}
