package com.wakemyway.app.voice

import android.content.Context
import com.wakemyway.core.personalization.WakeSessionPlan
import com.wakemyway.core.runtime.SpeechIntent

data class WakeSpeechRequest(
    val intent: SpeechIntent,
    val sessionPlan: WakeSessionPlan,
    /** Optional safe calendar fact, only delivered with the Orientation speech intent. */
    val calendarContext: String? = null,
)

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
        /** Ephemeral assistant caption fragments only; never persist them or treat them as authority. */
        fun onAssistantTranscript(delta: String) = Unit
        fun onAssistantSpeechFinished(interrupted: Boolean)
        /**
         * Previous remote PCM became quiet while a new runtime-authorized cue is queued.
         * Restore critical local audio during the provider-first-PCM gap, without
         * treating the prior audio tail as the new WakeRuntime SpeechFinished.
         */
        fun onAssistantAudioGapBeforeQueuedCue() = Unit
        fun onUserSpeechStarted()
        /** Reports whether the detected audio turn contained usable, intentional spoken engagement. */
        fun onUserTurnObserved(coherent: Boolean = true)
        /** Provider-reported seconds are a local cost proxy, not billed currency. */
        fun onProviderUsageObserved(seconds: Int, final: Boolean) = Unit
        fun onConversationFailure(stage: String)
    }

    val ready: Boolean

    /** Inclusive connection admission + negotiation budget; local alarm is always audible. */
    val startupBudgetMillis: Long get() = 20_000L

    fun connect()

    /** Requests one bounded natural-language response for a typed Wake Runtime intent and style. */
    fun respond(request: WakeSpeechRequest): Boolean

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
        "com.wakemyway.app.voice.GptLivePreferredWakeConversation",
        "com.wakemyway.app.voice.DirectRealtimeWakeConversation",
        "com.wakemyway.app.voice.DebugRealtimeWakeConversation",
    )

    fun create(
        context: Context,
        listener: WakeConversationEnrichment.Listener,
        sessionPlan: WakeSessionPlan,
    ): WakeConversationEnrichment? = implementations.firstNotNullOfOrNull { implementation ->
        runCatching {
            val type = Class.forName(implementation)
            val live = implementation.endsWith("GptLivePreferredWakeConversation")
            val constructor = if (live) type.getConstructor(
                Context::class.java,
                WakeConversationEnrichment.Listener::class.java,
                WakeSessionPlan::class.java,
            ) else type.getConstructor(
                Context::class.java,
                WakeConversationEnrichment.Listener::class.java,
            )
            if (live) constructor.newInstance(context.applicationContext, listener, sessionPlan)
                as WakeConversationEnrichment
            else constructor.newInstance(context.applicationContext, listener)
                as WakeConversationEnrichment
        }.getOrNull()
    }
}
