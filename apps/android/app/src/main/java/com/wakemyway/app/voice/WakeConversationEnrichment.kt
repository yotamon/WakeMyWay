package com.wakemyway.app.voice

import android.content.Context
import com.wakemyway.core.runtime.SpeechIntent

/**
 * Optional non-authoritative conversational enrichment for an active Wake Session.
 *
 * Implementations may use network/model audio, but they never own Alarm Kernel state, wake
 * completion, Snooze/Stop, or Activation Evidence. WakeVoiceSessionController remains the adapter
 * that translates model observations into typed WakeInput for WakeRuntime.
 */
interface WakeConversationEnrichment : AutoCloseable {
    interface Listener {
        fun onConversationReady()
        fun onAssistantSpeechStarted()
        fun onAssistantSpeechFinished(interrupted: Boolean)
        fun onUserSpeechStarted()
        fun onUserTurnObserved()
        fun onConversationFailure(stage: String)
    }

    val ready: Boolean

    fun connect()

    /** Requests one bounded natural-language response for a typed Wake Runtime intent. */
    fun respond(intent: SpeechIntent): Boolean

    /** Enables/disables microphone contribution to the realtime conversation. */
    fun setInputEnabled(enabled: Boolean)

    override fun close()
}

/**
 * Release-safe factory. Debug/founder builds may provide the implementation by class name.
 * Reflection keeps WebRTC/network dependencies entirely out of release source/dependency graphs.
 */
internal object WakeConversationEnrichmentFactory {
    private const val DEBUG_IMPLEMENTATION =
        "com.wakemyway.app.voice.DebugRealtimeWakeConversation"

    fun create(
        context: Context,
        listener: WakeConversationEnrichment.Listener,
    ): WakeConversationEnrichment? = runCatching {
        val type = Class.forName(DEBUG_IMPLEMENTATION)
        val constructor = type.getConstructor(
            Context::class.java,
            WakeConversationEnrichment.Listener::class.java,
        )
        constructor.newInstance(context.applicationContext, listener) as WakeConversationEnrichment
    }.getOrNull()
}
