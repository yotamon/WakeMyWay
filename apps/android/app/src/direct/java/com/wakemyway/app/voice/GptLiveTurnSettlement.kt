package com.wakemyway.app.voice

/**
 * Keeps tentative user-turn evidence from racing the previous assistant PCM
 * tail. No transcript text is retained and only WakeRuntime can change state.
 */
internal class GptLiveTurnSettlement {
    private var deferred: Boolean? = null

    fun observe(coherent: Boolean, remoteAudioActive: Boolean): Boolean? {
        if (!remoteAudioActive) return coherent
        deferred = coherent || deferred == true
        return null
    }

    fun drain(): Boolean? = deferred.also { deferred = null }

    fun clear() {
        deferred = null
    }
}
