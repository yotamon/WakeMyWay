package com.wakemyway.app.voice

/**
 * A response-level interruption is not a transport/session failure.
 *
 * Realtime can legitimately finish a response as incomplete (for example after hitting a token
 * ceiling or a safety filter) or cancelled (for example after an interruption). Those outcomes
 * should end only the current assistant turn. Only an explicit failed response tears down the
 * Realtime session; provider-level error events are handled separately by the transport adapter.
 */
internal object RealtimeResponseTerminalPolicy {
    fun shouldFailSession(status: String): Boolean = status == "failed"
}
