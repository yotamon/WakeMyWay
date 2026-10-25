package com.wakemyway.app.voice

/**
 * Distinguishes a Live cue awaiting dispatch behind currently audible PCM
 * from a cue already sent to the provider and waiting for its first PCM.
 *
 * A queued cue cannot be judged by the provider's first-audio timeout:
 * the provider has not received the cue yet. Both wait phases remain bounded.
 */
internal object GptLiveCueWaitPolicy {
    const val FIRST_AUDIO_TIMEOUT_MS = 12_000L
    const val QUEUED_CUE_DRAIN_TIMEOUT_MS = 20_000L

    fun isSentCueStillSilent(
        responsePending: Boolean,
        hasQueuedCue: Boolean,
        assistantAudioActive: Boolean,
    ): Boolean =
        responsePending && !hasQueuedCue && !assistantAudioActive

    fun isQueuedCueStalled(
        hasQueuedCue: Boolean,
        assistantAudioActive: Boolean,
    ): Boolean =
        hasQueuedCue && assistantAudioActive
}
