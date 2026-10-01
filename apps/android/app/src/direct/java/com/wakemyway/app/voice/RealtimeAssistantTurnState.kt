package com.wakemyway.app.voice

/**
 * Tracks one user-facing Realtime response independently from provider event ordering.
 *
 * A requested response starts unbound. Only response.created may bind it to a provider response id;
 * output-buffer and terminal events must then match that id. This prevents a delayed clear/stop from
 * an interrupted older response from terminating a newer turn after the newer response.create was
 * already sent.
 */
internal class RealtimeAssistantTurnState {
    enum class Signal {
        STARTED,
        FINISHED,
        INTERRUPTED,
        SILENT,
        FAILED,
        NEEDS_TERMINAL_GRACE,
    }

    private var inFlight = false
    private var responseId: String? = null
    private var audioStarted = false
    private var terminalStatus: String? = null

    val active: Boolean
        @Synchronized get() = inFlight

    @Synchronized
    fun begin(): Boolean {
        if (inFlight) return false
        inFlight = true
        responseId = null
        audioStarted = false
        terminalStatus = null
        return true
    }

    /**
     * response.created is the authoritative correlation point. A second different response id
     * cannot take ownership of an already-bound user-facing turn.
     */
    @Synchronized
    fun onResponseCreated(id: String): Boolean {
        if (!inFlight || id.isBlank()) return false
        val current = responseId
        if (current == null) {
            responseId = id
            return true
        }
        return current == id
    }

    @Synchronized
    fun onAudioStarted(id: String): Signal? {
        if (!matches(id) || audioStarted) return null
        audioStarted = true
        return Signal.STARTED
    }

    @Synchronized
    fun onAudioStopped(id: String, interrupted: Boolean): Signal? {
        if (!matches(id)) return null
        return finish(if (interrupted) Signal.INTERRUPTED else Signal.FINISHED)
    }

    @Synchronized
    fun onResponseDone(id: String, status: String): Signal? {
        if (!matches(id)) return null
        if (status == "failed") return finish(Signal.FAILED)

        terminalStatus = status.ifBlank { "completed" }
        return Signal.NEEDS_TERMINAL_GRACE
    }

    @Synchronized
    fun onTerminalGraceExpired(): Signal? {
        if (!inFlight || responseId == null || terminalStatus == null) return null
        if (audioStarted) {
            return if (terminalStatus == "cancelled") finish(Signal.INTERRUPTED) else null
        }
        return finish(
            when (terminalStatus) {
                "completed" -> Signal.SILENT
                else -> Signal.INTERRUPTED
            },
        )
    }

    @Synchronized
    fun reset() {
        inFlight = false
        responseId = null
        audioStarted = false
        terminalStatus = null
    }

    private fun matches(id: String): Boolean =
        inFlight && id.isNotBlank() && responseId == id

    private fun finish(signal: Signal): Signal {
        reset()
        return signal
    }
}
