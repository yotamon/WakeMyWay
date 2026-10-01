package com.wakemyway.app.voice

/**
 * Tracks one user-facing Realtime response independently from the provider's audio-buffer events.
 *
 * Realtime response generation and physical playback do not have identical lifetimes: response.done
 * may arrive while buffered audio is still playing, while a cancelled/incomplete response may end
 * before output_audio_buffer.started is ever observed. This state machine keeps those cases
 * exactly-once and lets the transport apply a short terminal grace before declaring a silent turn
 * finished.
 */
internal class RealtimeAssistantTurnState {
    enum class Signal {
        STARTED,
        FINISHED,
        INTERRUPTED,
        FAILED,
        NEEDS_TERMINAL_GRACE,
    }

    private var inFlight = false
    private var audioStarted = false
    private var terminalStatus: String? = null

    val active: Boolean get() = inFlight

    fun begin(): Boolean {
        if (inFlight) return false
        inFlight = true
        audioStarted = false
        terminalStatus = null
        return true
    }

    fun onAudioStarted(): Signal? {
        if (!inFlight || audioStarted) return null
        audioStarted = true
        return Signal.STARTED
    }

    fun onAudioStopped(interrupted: Boolean): Signal? {
        if (!inFlight) return null
        return finish(if (interrupted) Signal.INTERRUPTED else Signal.FINISHED)
    }

    fun onResponseDone(status: String): Signal? {
        if (!inFlight) return null
        if (status == "failed") return finish(Signal.FAILED)

        terminalStatus = status.ifBlank { "completed" }
        return if (audioStarted) null else Signal.NEEDS_TERMINAL_GRACE
    }

    fun onTerminalGraceExpired(): Signal? {
        if (!inFlight || audioStarted || terminalStatus == null) return null
        return finish(
            if (terminalStatus == "cancelled") Signal.INTERRUPTED else Signal.FINISHED,
        )
    }

    fun reset() {
        inFlight = false
        audioStarted = false
        terminalStatus = null
    }

    private fun finish(signal: Signal): Signal {
        reset()
        return signal
    }
}
