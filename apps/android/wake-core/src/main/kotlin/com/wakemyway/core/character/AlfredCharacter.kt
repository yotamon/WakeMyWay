package com.wakemyway.core.character

import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.runtime.SpeechIntent

object AlfredCharacter {
    val spec = CharacterSpec(
        id = CharacterId("alfred"),
        version = 8,
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
        SpeechIntent.ActivateUpperBody -> ACTIVATE_UPPER_BODY
        SpeechIntent.StandIfSafe -> STAND_IF_SAFE
        SpeechIntent.KeepEngaging -> KEEP_ENGAGING
        SpeechIntent.HoldEngagement -> HOLD_ENGAGEMENT
        is SpeechIntent.ReEngage -> RE_ENGAGE[intent.escalationLevel.coerceIn(0, MAX_RE_ENGAGE_LEVEL)]
        SpeechIntent.SnoozeConfirmation -> SNOOZE_CONFIRMATION
        SpeechIntent.SnoozeFailed -> SNOOZE_FAILED
        SpeechIntent.Orientation -> ORIENTATION
    }

    private fun motivationalVariantsFor(intent: SpeechIntent): List<String> = when (intent) {
        SpeechIntent.InitialWake -> MOTIVATIONAL_INITIAL_WAKE
        SpeechIntent.AskToSitUp -> MOTIVATIONAL_ASK_TO_SIT_UP
        SpeechIntent.AskToMove -> MOTIVATIONAL_ASK_TO_MOVE
        SpeechIntent.ActivateUpperBody -> MOTIVATIONAL_ACTIVATE_UPPER_BODY
        SpeechIntent.StandIfSafe -> MOTIVATIONAL_STAND_IF_SAFE
        SpeechIntent.KeepEngaging -> MOTIVATIONAL_KEEP_ENGAGING
        SpeechIntent.HoldEngagement -> MOTIVATIONAL_HOLD_ENGAGEMENT
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
        SpeechIntent.ActivateUpperBody -> MINIMAL_ACTIVATE_UPPER_BODY
        SpeechIntent.StandIfSafe -> MINIMAL_STAND_IF_SAFE
        SpeechIntent.KeepEngaging -> MINIMAL_KEEP_ENGAGING
        SpeechIntent.HoldEngagement -> MINIMAL_HOLD_ENGAGEMENT
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
        SpeechIntent.ActivateUpperBody -> "activate-upper-body"
        SpeechIntent.StandIfSafe -> "stand-if-safe"
        SpeechIntent.KeepEngaging -> "keep-engaging"
        SpeechIntent.HoldEngagement -> "hold-engagement"
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
        "Good morning. Sit up, then tell me when you're there.",
        "Morning. Let's begin with sitting up. Tell me when you're there.",
        "Good morning. Up to sitting, then give me a quick hello.",
        "Morning. First move: sit up, then tell me you're with me.",
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

    private val ACTIVATE_UPPER_BODY = listOf(
        "Roll your shoulders twice, slowly. Then answer me.",
        "Two slow shoulder rolls, then give me a quick answer.",
        "Stay seated. Two slow shoulder rolls, then tell me.",
        "Shoulders twice, slowly. Then tell me you're with me.",
    )

    private val STAND_IF_SAFE = listOf(
        "If it's safe, stand beside the bed. Otherwise sit tall. Then answer me.",
        "Stand beside the bed if that feels safe. Otherwise stay seated and answer.",
        "If standing is safe, stand now. If not, sit tall and answer.",
        "Stand only if safe. Otherwise stay seated. Then tell me you're here.",
    )

    private val KEEP_ENGAGING = listOf(
        "Switch on a reachable light, or open reachable curtains. Then answer me.",
        "One easy environment change: light or curtains within reach. Then answer.",
        "Bring in some light if it's within reach, then tell me.",
        "Reachable light or curtains next. Then give me a short answer.",
    )

    private val HOLD_ENGAGEMENT = listOf(
        "All right. Stay with me a moment and give me one short reply.",
        "I'm with you. One more clear reply, no extra gymnastics.",
        "Fair enough. Keep talking to me for one more beat.",
        "No need to repeat the movement. Just give me one clear reply.",
    )

    private val RE_ENGAGE = listOf(
        listOf(
            "Still with me? Give me one short answer.",
            "Stay with me a moment. Tell me you're here.",
            "I didn't catch words there. Give me a quick yes.",
            "Back with me, please. One short reply.",
        ),
        listOf(
            "Stay with me. Answer me out loud.",
            "A clearer reply this time, please.",
            "I need one short answer from you.",
            "Give me a clear yes when you hear me.",
        ),
        listOf(
            "I need a clear reply now. Answer me.",
            "Stay with me. One clear answer now.",
            "Answer out loud now, please.",
            "One deliberate reply now. Tell me you're here.",
        ),
        listOf(
            "Answer me now, please. One short reply.",
            "One clear answer now. Stay with me.",
            "I need your voice now. Give me one reply.",
            "Give me one clear spoken answer now.",
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
        "Morning. The bed has made its case. Sit up, then tell me you're there.",
        "Good morning. One clear first move: sit up, then answer me.",
        "Morning. Let's start strong and simple. Sit up, then tell me.",
    )

    private val MOTIVATIONAL_ASK_TO_SIT_UP = listOf(
        "Good start. Sit up, then tell me when you're there.",
        "No ceremony required. Sit up, then give me a quick answer.",
        "One strong first move: sit up, then tell me you're there.",
    )

    private val MOTIVATIONAL_ASK_TO_MOVE = listOf(
        "Nice. Feet on the floor next, then tell me they're down.",
        "Keep that start going. Feet down, then answer me.",
        "Good. Give me feet on the floor, then tell me when it's done.",
    )

    private val MOTIVATIONAL_ACTIVATE_UPPER_BODY = listOf(
        "Keep it simple. Two slow shoulder rolls, then answer me.",
        "Two slow shoulder rolls now, then give me a quick reply.",
        "Stay seated and roll your shoulders twice. Then answer.",
    )

    private val MOTIVATIONAL_STAND_IF_SAFE = listOf(
        "If standing feels safe, stand beside the bed. Otherwise sit tall and answer.",
        "Stand only if it's safe. Otherwise stay seated and give me a quick reply.",
        "If safe, stand beside the bed. If not, sit tall. Then answer.",
    )

    private val MOTIVATIONAL_KEEP_ENGAGING = listOf(
        "Bring in some reachable light, then give me a quick answer.",
        "Light or curtains within reach next. Then answer me.",
        "One easy environment change now: reachable light or curtains. Then answer.",
    )

    private val MOTIVATIONAL_HOLD_ENGAGEMENT = listOf(
        "Keep the thread. One more clear reply, no repeated movement.",
        "Good, stay with me for one more short answer.",
        "Keep talking to me. No need to redo the last action.",
    )

    private val MOTIVATIONAL_RE_ENGAGE = listOf(
        listOf(
            "Stay with me. Give me one quick answer.",
            "Come back to my voice. One short reply.",
            "Still here. Give me a clear yes.",
        ),
        listOf(
            "Keep the thread. Answer me out loud.",
            "One clear reply now. Stay with me.",
            "Give me your voice for one short answer.",
        ),
        listOf(
            "Stay with the morning. One clear answer now.",
            "I need one deliberate reply now.",
            "Answer me clearly now, please.",
        ),
        listOf(
            "One clear spoken answer now. Stay with me.",
            "Give me one reply now, please.",
            "Your voice now. One clear answer.",
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
        "There we are. Get your bearings, then choose what comes first.",
        "Good. Keep that momentum and find the next useful move.",
    )

    private val MINIMAL_INITIAL_WAKE = listOf(
        "Morning. Sit up, then answer.",
        "Good morning. Sit up. Tell me.",
        "Morning. Up to sitting. Answer.",
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

    private val MINIMAL_ACTIVATE_UPPER_BODY = listOf(
        "Two shoulder rolls. Then answer.",
        "Roll shoulders twice. Answer me.",
        "Shoulders twice, slowly. Then answer.",
    )

    private val MINIMAL_STAND_IF_SAFE = listOf(
        "If safe, stand. Otherwise sit tall. Answer.",
        "Stand if safe. Otherwise stay seated. Answer.",
        "Stand only if safe. If not, sit tall.",
    )

    private val MINIMAL_KEEP_ENGAGING = listOf(
        "Reachable light or curtains. Then answer.",
        "Light within reach. Then answer.",
        "Open reachable curtains. Then answer.",
    )

    private val MINIMAL_HOLD_ENGAGEMENT = listOf(
        "Stay with me. One reply.",
        "No repeat. Just answer.",
        "One more clear reply.",
    )

    private val MINIMAL_RE_ENGAGE = listOf(
        listOf(
            "Still here? Answer me.",
            "Come back. Then answer.",
            "One short reply, please.",
        ),
        listOf(
            "Answer me out loud.",
            "One clear reply now.",
            "Stay with me. Answer.",
        ),
        listOf(
            "Clear answer now, please.",
            "Answer me clearly now.",
            "One deliberate reply now.",
        ),
        listOf(
            "Answer now. One short reply.",
            "Your voice now. Answer me.",
            "One clear spoken answer now.",
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
