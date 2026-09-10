package com.wakemyway.app.voice

/**
 * In-memory M8 timing state for one direct realtime smoke-measurement attempt.
 *
 * This class deliberately stores only monotonic timestamps/derived durations. It has
 * no fields for prompts, transcripts, audio, provider payloads, or user context.
 */
internal class VoiceSpikeMeasurementSession {
    data class Snapshot(
        val protocolId: String,
        val firstSpeechObservationMethod: String,
        val coldConnectionMs: Long?,
        val firstSpeechMs: Long?,
        val responseProbePending: Boolean,
    )

    private var coldConnectionMs: Long? = null
    private var responseRequestedAtMs: Long? = null
    private var firstAudibleObservedAtMs: Long? = null

    fun reset() {
        coldConnectionMs = null
        responseRequestedAtMs = null
        firstAudibleObservedAtMs = null
    }

    fun recordColdConnection(elapsedMs: Long) {
        require(elapsedMs >= 0) { "Cold connection latency must be non-negative" }
        coldConnectionMs = elapsedMs
    }

    fun recordSyntheticResponseRequested(monotonicAtMs: Long) {
        require(monotonicAtMs >= 0) { "Response request timestamp must be non-negative" }
        responseRequestedAtMs = monotonicAtMs
        firstAudibleObservedAtMs = null
    }

    fun recordFirstAudibleObserved(monotonicAtMs: Long): Long {
        val requestedAt = checkNotNull(responseRequestedAtMs) {
            "A synthetic response must be requested before first audible speech is marked"
        }
        check(firstAudibleObservedAtMs == null) {
            "First audible speech has already been marked for this response"
        }
        require(monotonicAtMs >= requestedAt) {
            "First audible observation cannot precede the response request"
        }

        firstAudibleObservedAtMs = monotonicAtMs
        return monotonicAtMs - requestedAt
    }

    fun snapshot(): Snapshot {
        val requestedAt = responseRequestedAtMs
        val audibleAt = firstAudibleObservedAtMs
        return Snapshot(
            protocolId = PROTOCOL_ID,
            firstSpeechObservationMethod = FIRST_SPEECH_OBSERVATION_METHOD,
            coldConnectionMs = coldConnectionMs,
            firstSpeechMs = if (requestedAt != null && audibleAt != null) audibleAt - requestedAt else null,
            responseProbePending = requestedAt != null && audibleAt == null,
        )
    }

    companion object {
        const val PROTOCOL_ID = "m8-direct-openai-manual-audible-v1"
        const val FIRST_SPEECH_OBSERVATION_METHOD = "operator-tap-upper-bound-v1"
    }
}
