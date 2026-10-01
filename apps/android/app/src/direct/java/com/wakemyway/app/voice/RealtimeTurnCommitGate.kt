package com.wakemyway.app.voice

/**
 * Correlates one semantic-VAD user turn from speech_started through committed.
 *
 * A watchdog timeout clears the active turn. Any later committed event for that old item is STALE,
 * not an additional unusable reply. This prevents a delayed provider commit from injecting a second
 * WakeRuntime input after the controller has already advanced to the next assistant turn.
 */
internal class RealtimeTurnCommitGate(
    private val minimumDurationMs: Long,
) {
    enum class CommitDecision {
        QUALIFIED,
        UNQUALIFIED,
        STALE,
    }

    init {
        require(minimumDurationMs > 0L)
    }

    private var activeItemId: String? = null
    private var speechStartedAtMs: Long? = null
    private var pendingQualifiedTurn = false

    @Synchronized
    fun onSpeechStarted(itemId: String, audioStartMs: Long?) {
        activeItemId = itemId
        speechStartedAtMs = audioStartMs
        pendingQualifiedTurn = false
    }

    @Synchronized
    fun onSpeechStopped(itemId: String, audioEndMs: Long?) {
        if (itemId != activeItemId) return
        val start = speechStartedAtMs
        pendingQualifiedTurn =
            start != null && audioEndMs != null &&
                audioEndMs >= start &&
                audioEndMs - start >= minimumDurationMs
        speechStartedAtMs = null
    }

    @Synchronized
    fun onCommitted(itemId: String): CommitDecision {
        if (itemId != activeItemId) return CommitDecision.STALE
        val qualified = pendingQualifiedTurn
        clearActive()
        return if (qualified) CommitDecision.QUALIFIED else CommitDecision.UNQUALIFIED
    }

    /**
     * Returns true only when this timeout actually consumed a still-active user turn.
     * If a commit won the race first, the timeout becomes a no-op.
     */
    @Synchronized
    fun onTimedOut(): Boolean {
        if (activeItemId == null) return false
        clearActive()
        return true
    }

    @Synchronized
    fun reset() {
        clearActive()
    }

    private fun clearActive() {
        activeItemId = null
        speechStartedAtMs = null
        pendingQualifiedTurn = false
    }
}
