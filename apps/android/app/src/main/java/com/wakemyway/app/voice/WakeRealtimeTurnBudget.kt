package com.wakemyway.app.voice

import com.wakemyway.core.runtime.WakePolicy

/**
 * Keeps Realtime's cost/failure guard compatible with every Wake Learning v0 policy.
 *
 * The budget must include the initial assistant turn, enough user/assistant exchanges to satisfy
 * the activation threshold through voice-only evidence, the orientation turn, and the bounded
 * escalation retries. It remains hard-capped so a broken conversation cannot run indefinitely.
 */
internal object WakeRealtimeTurnBudget {
    private const val MIN_TURNS = 8
    private const val MAX_TURNS = 12

    fun resolve(policy: WakePolicy): Int = (
        policy.activationThreshold +
            policy.maxEscalationLevel +
            1
        ).coerceIn(MIN_TURNS, MAX_TURNS)
}
