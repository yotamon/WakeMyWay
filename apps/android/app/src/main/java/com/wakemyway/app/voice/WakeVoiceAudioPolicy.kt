package com.wakemyway.app.voice

/**
 * Critical alarm playback always owns the output path. Voice may request a
 * temporary volume lease only while real assistant PCM is present or the
 * surface is actively listening for a user response.
 *
 * Pending model prompts, mere WebRTC connectivity, and stale transcripts do
 * not justify reducing the local alarm volume.
 */
internal object WakeVoiceAudioPolicy {
    fun voiceWindowNeeded(remotePcmActive: Boolean, listeningForUser: Boolean): Boolean =
        remotePcmActive || listeningForUser
}
