package com.wakemyway.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.personalization.ConversationAmount
import com.wakemyway.core.personalization.HumorPreference
import com.wakemyway.core.personalization.InterventionStyle
import com.wakemyway.core.personalization.WakeSessionPlan
import com.wakemyway.core.runtime.SpeechIntent

/**
 * Live-first Direct wake conversation. The legacy Realtime adapter is only used
 * when server authorization denies Live before provider admission (HTTP 403).
 * Network/provider/timeouts never trigger a second potentially paid session.
 */
class GptLivePreferredWakeConversation(
    context: Context,
    private val listener: WakeConversationEnrichment.Listener,
    private val plan: WakeSessionPlan,
) : WakeConversationEnrichment, GptLiveWebRtcSession.Observer {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var session: GptLiveWebRtcSession? = null
    private var legacy: DirectRealtimeWakeConversation? = null
    private var fallbackPending = false
    private var closed = false
    private var liveReady = false
    private var inputEnabled = false
    private var responsePending = false
    private val turnBuffer = StringBuilder()
    private val userTurnQuiet = Runnable {
        if (!closed && inputEnabled && turnBuffer.isNotBlank()) {
            val content = turnBuffer.toString().trim()
            turnBuffer.setLength(0)
            // Short intentional words are engagement, not command authority.
            listener.onUserTurnObserved(content.any(Char::isLetterOrDigit))
        }
    }
    private val noAudioDeadline = Runnable {
        if (!closed && liveReady && responsePending) listener.onConversationFailure("live-no-audio")
    }

    override val startupBudgetMillis: Long get() = 35_000L
    override val ready: Boolean get() = !closed && (legacy?.ready ?: liveReady)

    override fun connect() {
        if (closed) return
        legacy?.let { it.connect(); return }
        if (fallbackPending || session != null) return
        session = GptLiveWebRtcSession(appContext, this).also { transport ->
            transport.connect(
                character = when (plan.characterId.value) {
                    "sam" -> GptLiveWebRtcSession.Character.SAM
                    "luna" -> GptLiveWebRtcSession.Character.LUNA
                    "rocco" -> GptLiveWebRtcSession.Character.ROCCO
                    else -> GptLiveWebRtcSession.Character.ALFRED
                },
                settings = GptLiveWebRtcSession.VoiceLabSettings(
                    energy = when (plan.preferences.interventionStyle) {
                        InterventionStyle.GENTLE -> GptLiveWebRtcSession.Energy.CALM
                        InterventionStyle.FIRM -> GptLiveWebRtcSession.Energy.ENERGETIC
                        else -> GptLiveWebRtcSession.Energy.BALANCED
                    },
                    directness = when (plan.preferences.interventionStyle) {
                        InterventionStyle.GENTLE -> GptLiveWebRtcSession.Directness.GENTLE
                        InterventionStyle.FIRM -> GptLiveWebRtcSession.Directness.FIRM
                        else -> GptLiveWebRtcSession.Directness.BALANCED
                    },
                    conversationAmount = when (plan.preferences.conversationAmount) {
                        ConversationAmount.MINIMAL -> GptLiveWebRtcSession.ConversationAmount.MINIMAL
                        ConversationAmount.SOCIAL -> GptLiveWebRtcSession.ConversationAmount.CHATTY
                        else -> GptLiveWebRtcSession.ConversationAmount.NATURAL
                    },
                    humor = when (plan.preferences.humorPreference) {
                        HumorPreference.OFF -> GptLiveWebRtcSession.Humor.OFF
                        HumorPreference.WELCOME -> GptLiveWebRtcSession.Humor.PLAYFUL
                        else -> GptLiveWebRtcSession.Humor.SUBTLE
                    },
                    locale = when (java.util.Locale.getDefault().language) {
                        "he", "iw" -> GptLiveWebRtcSession.Locale.HE
                        "de" -> GptLiveWebRtcSession.Locale.DE
                        else -> GptLiveWebRtcSession.Locale.EN
                    },
                ),
                voiceStyle = plan.voiceStyle.name.lowercase(),
            )
        }
    }

    override fun respond(request: WakeSpeechRequest): Boolean {
        legacy?.let { return it.respond(request) }
        if (!ready || responsePending) return false
        val cue = GptLiveWakeCue.forIntent(request.intent)
        val accepted = session?.sendWakeCue(cue) ?: false
        if (accepted) {
            responsePending = true
            handler.removeCallbacks(noAudioDeadline)
            handler.postDelayed(noAudioDeadline, 12_000L)
        }
        return accepted
    }

    override fun setInputEnabled(enabled: Boolean) {
        inputEnabled = enabled
        if (!enabled) {
            turnBuffer.setLength(0)
            handler.removeCallbacks(userTurnQuiet)
        }
        legacy?.let { it.setInputEnabled(enabled); return }
        session?.setMicrophoneEnabled(enabled)
    }

    override fun onReady() {
        if (closed || fallbackPending) return
        liveReady = true
        listener.onConversationReady()
    }

    override fun onRemoteSpeechActivity(active: Boolean) {
        if (closed || fallbackPending) return
        if (active) {
            responsePending = false
            handler.removeCallbacks(noAudioDeadline)
            listener.onAssistantSpeechStarted()
        } else {
            listener.onAssistantSpeechFinished(interrupted = false)
        }
    }

    override fun onTranscript(fromUser: Boolean, delta: String) {
        if (closed || fallbackPending || !fromUser || !inputEnabled || delta.isBlank()) return
        if (turnBuffer.isEmpty()) listener.onUserSpeechStarted()
        turnBuffer.append(delta)
        if (turnBuffer.length > 250) turnBuffer.delete(0, turnBuffer.length - 250)
        handler.removeCallbacks(userTurnQuiet)
        handler.postDelayed(userTurnQuiet, 1_200L)
    }

    override fun onFailure(stage: String) {
        if (closed) return
        if (stage == "pilot-access-denied" && !liveReady) {
            fallbackPending = true
            return
        }
        listener.onConversationFailure("live-$stage")
    }

    override fun onUsageSeconds(seconds: Int, final: Boolean) = Unit

    override fun onSessionClosed(usageConfirmed: Boolean) {
        liveReady = false
        session = null
        if (closed) return
        if (fallbackPending) {
            fallbackPending = false
            legacy = DirectRealtimeWakeConversation(appContext, listener)
            legacy?.setInputEnabled(inputEnabled)
            legacy?.connect()
        } else {
            listener.onConversationFailure("live-session-ended")
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        handler.removeCallbacks(noAudioDeadline)
        handler.removeCallbacks(userTurnQuiet)
        turnBuffer.setLength(0)
        session?.requestClose()
        session = null
        legacy?.close()
        legacy = null
    }
}

/** Fixed semantic cues. The model never chooses or receives a mutable policy. */
internal object GptLiveWakeCue {
    fun forIntent(intent: SpeechIntent): String = when (intent) {
        SpeechIntent.InitialWake -> "Greet the person warmly in one short sentence and gently invite them to sit up. Then listen without pressuring a reply."
        SpeechIntent.AskToSitUp -> "If useful, gently suggest sitting upright. No demands or repeated instructions. Then listen."
        SpeechIntent.AskToMove -> "Offer one small, safe movement invitation such as placing feet on the floor. Only one action and then listen."
        SpeechIntent.ActivateUpperBody -> "Briefly invite a gentle stretch while seated. Never assume they moved."
        SpeechIntent.StandIfSafe -> "Only if comfortable and safe, invite standing up; do not assume compliance."
        SpeechIntent.KeepEngaging, SpeechIntent.HoldEngagement ->
            "Acknowledge their last remark naturally in a few words. Do not give another task or question unless needed."
        is SpeechIntent.ReEngage -> "A single soft check-in after silence, with no demand to answer. Then give them space."
        SpeechIntent.SnoozeConfirmation -> "Explain that snooze requires tapping the app's confirmation. Do not claim it was set."
        SpeechIntent.SnoozeFailed -> "Briefly say that snooze did not work and the local alarm is still active."
        SpeechIntent.Orientation -> "The app detected enough activity to offer a calm transition into the day. Say one short encouraging goodbye without claiming certainty about being awake."
    }
}
