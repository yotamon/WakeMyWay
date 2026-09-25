package com.wakemyway.app.voice

internal class RealtimeTurnCommitGate(
    private val minimumDurationMs: Long,
) {
    init {
        require(minimumDurationMs > 0L)
    }

    private var speechStartedAtMs: Long? = null
    private var pendingQualifiedTurn = false

    fun onSpeechStarted(audioStartMs: Long?) {
        speechStartedAtMs = audioStartMs
        pendingQualifiedTurn = false
    }

    fun onSpeechStopped(audioEndMs: Long?) {
        val start = speechStartedAtMs
        pendingQualifiedTurn =
            start != null && audioEndMs != null &&
                audioEndMs >= start &&
                audioEndMs - start >= minimumDurationMs
        speechStartedAtMs = null
    }

    fun onCommitted(): Boolean {
        val qualified = pendingQualifiedTurn
        pendingQualifiedTurn = false
        return qualified
    }

    fun reset() {
        speechStartedAtMs = null
        pendingQualifiedTurn = false
    }
}
