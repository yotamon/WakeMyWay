package com.wakemyway.app.voice

import android.content.Context
import com.wakemyway.core.alarm.VoiceStyle
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

    /** Requests one bounded natural-language response for a typed Wake Runtime intent and style. */
    fun respond(
        intent: SpeechIntent,
        style: VoiceStyle,
    ): Boolean

    /** Enables/disables microphone contribution to the realtime conversation. */
    fun setInputEnabled(enabled: Boolean)

    override fun close()
}

/**
 * Release-safe factory for distribution-scoped conversational adapters.
 * Direct and debug variants may provide Realtime implementations by class name; Play provides none,
 * so the shared wake path keeps an optional boundary without linking provider code into Play.
 */
internal object WakeConversationEnrichmentFactory {
    private val implementations = listOf(
        "com.wakemyway.app.voice.DirectRealtimeWakeConversation",
        "com.wakemyway.app.voice.DebugRealtimeWakeConversation",
    )

    fun create(
        context: Context,
        listener: WakeConversationEnrichment.Listener,
    ): WakeConversationEnrichment? = implementations.firstNotNullOfOrNull { implementation ->
        runCatching {
            val type = Class.forName(implementation)
            val constructor = type.getConstructor(
                Context::class.java,
                WakeConversationEnrichment.Listener::class.java,
            )
            constructor.newInstance(
                context.applicationContext,
                listener,
            ) as WakeConversationEnrichment
        }.getOrNull()
    }
}
