package com.wakemyway.app.voice

/**
 * Playback-proxy hysteresis for GPT-Live's full-duplex PCM stream.
 *
 * Live does not emit an authoritative per-utterance completion event. A momentary
 * quiet PCM packet is insufficient evidence to advance WakeRuntime: require
 * sustained quiet *and* no new assistant transcript activity. This remains a
 * conservative heuristic, not a guarantee that sound reached a speaker.
 *
 * All methods are called on the Android main thread.
 */
internal class GptLiveAudioActivityGate(
    private val audioQuietMillis: Long = 2_800L,
    private val transcriptQuietMillis: Long = 1_600L,
) {
    init {
        require(audioQuietMillis > 0)
        require(transcriptQuietMillis > 0)
    }

    private var active = false
    private var lastAudioAt = -1L
    private var lastTranscriptAt = -1L

    /** True exactly once when a new run of non-silent PCM begins. */
    fun onAudiblePcm(atMillis: Long): Boolean {
        val started = !active
        active = true
        lastAudioAt = atMillis
        return started
    }

    fun onOutputTranscript(atMillis: Long) {
        lastTranscriptAt = atMillis
    }

    /** Null when there is no active remote audio run. */
    fun remainingQuietMillis(atMillis: Long): Long? {
        if (!active) return null
        val pcmDeadline = lastAudioAt + audioQuietMillis
        val transcriptDeadline = if (lastTranscriptAt >= 0L) {
            lastTranscriptAt + transcriptQuietMillis
        } else {
            0L
        }
        return (maxOf(pcmDeadline, transcriptDeadline) - atMillis).coerceAtLeast(0L)
    }

    fun finishIfQuiet(atMillis: Long): Boolean {
        if (remainingQuietMillis(atMillis) != 0L) return false
        reset()
        return true
    }

    fun reset() {
        active = false
        lastAudioAt = -1L
        lastTranscriptAt = -1L
    }
}
