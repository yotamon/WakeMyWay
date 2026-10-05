package com.wakemyway.app.voice

import com.wakemyway.core.runtime.WakePolicy

/**
 * Keeps Realtime's cost/failure guard compatible with every Wake Learning v0 policy.
 *
 * The budget includes the initial assistant turn, enough voluntary user/assistant exchanges to
 * satisfy the activation threshold through voice-only evidence when that happens naturally, the
 * orientation turn, and the explicitly bounded verbal re-engagement allowance. It remains
 * hard-capped so a broken conversation cannot become a nagging loop.
 */
internal object WakeRealtimeTurnBudget {
    private const val MIN_TURNS = 8
    private const val MAX_TURNS = 10

    fun resolve(policy: WakePolicy): Int = (
        policy.activationThreshold +
            policy.maxVerbalReengagementPrompts +
            1
        ).coerceIn(MIN_TURNS, MAX_TURNS)
}
