package com.wakemyway.core.character

import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.runtime.SpeechIntent

object AlfredCharacter {
    val spec = CharacterSpec(
        id = CharacterId("alfred"),
        version = 9,
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
        "Good morning. Easy start. Come up to sitting when you're ready.",
        "Morning. No rush. Bring yourself up to sitting.",
        "Good morning. Let's leave the pillow and find sitting.",
        "Morning. Up to sitting when you're ready.",
    )

    private val ASK_TO_SIT_UP = listOf(
        "Come up to sitting when you're ready.",
        "Easy first move. Bring yourself up to sitting.",
        "Let's find sitting, nice and easy.",
        "Up to sitting when it feels reasonable.",
    )

    private val ASK_TO_MOVE = listOf(
        "Feet toward the floor when you're ready.",
        "Let's introduce gravity. Feet down when it suits.",
        "A little movement now. Feet toward the floor.",
        "Next small thing: feet down, nice and easy.",
    )

    private val ACTIVATE_UPPER_BODY = listOf(
        "Two slow shoulder rolls, if that feels useful.",
        "A couple of slow shoulder rolls. Nothing heroic.",
        "Stay seated and loosen the shoulders twice.",
        "Two easy shoulder rolls, nice and slow.",
    )

    private val STAND_IF_SAFE = listOf(
        "If standing feels safe, come up beside the bed. Otherwise stay seated.",
        "Stand only if it feels safe. Otherwise sit tall.",
        "If safe, come to standing. If not, stay seated.",
        "Standing is optional. If it's safe, come up; otherwise stay seated.",
    )

    private val KEEP_ENGAGING = listOf(
        "A little reachable light might help now.",
        "If it's handy, bring in some light.",
        "Reachable curtains or a light, if you fancy it.",
        "A touch of light, only if it's within reach.",
    )

    private val HOLD_ENGAGEMENT = listOf(
        "Fair enough. The bed has had its say.",
        "I'm with you. We can keep this quiet.",
        "All right. One moment at a time.",
        "There we are. No extra gymnastics required.",
    )

    private val RE_ENGAGE = listOf(
        listOf(
            "Morning. I'm still here.",
            "No rush. I'm here.",
            "Still with you.",
            "Take your time. I'm here.",
        ),
        listOf(
            "Morning. Come back to my voice when you can.",
            "Still here. A quick hello when you're ready.",
            "Come back when you can. I'm here.",
            "Morning. Let me hear you when you're ready.",
        ),
        listOf(
            "Morning. Stay with the sound for a moment.",
            "Come back to me when you're ready.",
            "Still here. A quick hello would do.",
            "Morning. Find my voice again.",
        ),
        listOf(
            "Morning. Time to come back. I'm here.",
            "Let's leave the silence behind. I'm here.",
            "Come back to the morning with me.",
            "Morning. Find my voice when you can.",
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
        "Morning. Easy momentum: come up to sitting when you're ready.",
        "Good morning. Start small and come up to sitting.",
        "Morning. Let's get some daylight into the body. Start with sitting.",
    )

    private val MOTIVATIONAL_ASK_TO_SIT_UP = listOf(
        "Easy first move: come up to sitting.",
        "Start small. Bring yourself up to sitting.",
        "A little momentum. Find sitting when you're ready.",
    )

    private val MOTIVATIONAL_ASK_TO_MOVE = listOf(
        "Keep the gentle momentum. Feet toward the floor.",
        "Next small move: feet down when you're ready.",
        "A little more movement. Feet toward the floor.",
    )

    private val MOTIVATIONAL_ACTIVATE_UPPER_BODY = listOf(
        "Add two slow shoulder rolls. Keep it easy.",
        "A couple of shoulder rolls to bring some movement in.",
        "Two calm shoulder rolls. Nothing strenuous.",
    )

    private val MOTIVATIONAL_STAND_IF_SAFE = listOf(
        "If standing feels safe, come up beside the bed. Otherwise stay tall seated.",
        "Stand only if it feels safe. Seated and tall is perfectly fine.",
        "If safe, come to standing. Otherwise keep the seated version.",
    )

    private val MOTIVATIONAL_KEEP_ENGAGING = listOf(
        "A little reachable light could help the morning along.",
        "If it's handy, bring in some light.",
        "One small environment shift: reachable light, if useful.",
    )

    private val MOTIVATIONAL_HOLD_ENGAGEMENT = listOf(
        "Good, we can keep the momentum without adding another task.",
        "There we are. Keep the morning moving gently.",
        "All right. No new assignment; just stay with the moment.",
    )

    private val MOTIVATIONAL_RE_ENGAGE = listOf(
        listOf(
            "Morning. I'm here when you're ready.",
            "Come back gently. I'm still here.",
            "Still with you. No rush.",
        ),
        listOf(
            "Morning. Find my voice when you're ready.",
            "Come back to the thread when you can.",
            "Still here. A quick hello is enough.",
        ),
        listOf(
            "Morning. Let's find the thread again.",
            "Come back to my voice when you can.",
            "Still here. Rejoin me when you're ready.",
        ),
        listOf(
            "Morning. Time to rejoin the day with me.",
            "Come back to the morning now, nice and steady.",
            "Find my voice and come back when you can.",
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
        "Morning. Come up to sitting.",
        "Good morning. Find sitting.",
        "Morning. Sit up when ready.",
    )

    private val MINIMAL_ASK_TO_SIT_UP = listOf(
        "Come up to sitting.",
        "Sit up when ready.",
        "Find sitting, nice and easy.",
    )

    private val MINIMAL_ASK_TO_MOVE = listOf(
        "Feet toward the floor.",
        "Feet down when ready.",
        "A little movement. Feet down.",
    )

    private val MINIMAL_ACTIVATE_UPPER_BODY = listOf(
        "Two slow shoulder rolls.",
        "Roll shoulders twice, slowly.",
        "Shoulders twice. Keep it easy.",
    )

    private val MINIMAL_STAND_IF_SAFE = listOf(
        "Stand if safe; otherwise stay seated.",
        "If safe, stand. Otherwise sit tall.",
        "Standing optional. Stay seated if needed.",
    )

    private val MINIMAL_KEEP_ENGAGING = listOf(
        "Reachable light, if useful.",
        "A little light, if handy.",
        "Curtains or light, if nearby.",
    )

    private val MINIMAL_HOLD_ENGAGEMENT = listOf(
        "I'm with you.",
        "All right. No extra task.",
        "Fair enough. Keep it easy.",
    )

    private val MINIMAL_RE_ENGAGE = listOf(
        listOf(
            "Morning. I'm here.",
            "Still with you.",
            "No rush. I'm here.",
        ),
        listOf(
            "Morning. Come back when ready.",
            "Still here. Hello when ready.",
            "Find my voice when ready.",
        ),
        listOf(
            "Morning. Find my voice again.",
            "Come back when you can.",
            "Still here. Rejoin when ready.",
        ),
        listOf(
            "Morning. Time to come back.",
            "Come back to the morning.",
            "Find my voice when you can.",
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
