package com.wakemyway.app.voice

import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeSessionSnapshot
import com.wakemyway.core.runtime.WakeTransition
import java.time.Duration

/**
 * Passive observation boundary around WakeRuntime reduction.
 *
 * The runtime remains the behavioral authority. This bridge only reports already-applied
 * transitions with a monotonic duration measured from the interactive Wake Runtime start.
 * Observer failures are isolated and can never change a WakeRuntime transition.
 */
fun interface WakeRuntimeTransitionObserver {
    fun onAppliedTransition(
        before: WakeSessionSnapshot,
        input: WakeInput,
        transition: WakeTransition,
        elapsedSinceRuntimeStart: Duration,
    )

    companion object {
        val NONE = WakeRuntimeTransitionObserver { _, _, _, _ -> Unit }
    }
}

internal class WakeRuntimeObservationBridge(
    private val runtime: WakeRuntime,
    private val policy: WakePolicy,
    private val elapsedRealtimeMillis: () -> Long,
    private val observer: WakeRuntimeTransitionObserver = WakeRuntimeTransitionObserver.NONE,
) {
    private var startedAtElapsedMillis: Long? = null

    fun begin() {
        if (startedAtElapsedMillis == null) {
            startedAtElapsedMillis = elapsedRealtimeMillis()
        }
    }

    fun reduce(
        before: WakeSessionSnapshot,
        input: WakeInput,
    ): WakeTransition {
        val transition = runtime.reduce(before, input, policy)
        if (!transition.inputApplied) return transition

        val origin = startedAtElapsedMillis ?: elapsedRealtimeMillis().also {
            startedAtElapsedMillis = it
        }
        val elapsed = Duration.ofMillis(
            (elapsedRealtimeMillis() - origin).coerceAtLeast(0L),
        )
        runCatching {
            observer.onAppliedTransition(
                before = before,
                input = input,
                transition = transition,
                elapsedSinceRuntimeStart = elapsed,
            )
        }
        return transition
    }
}
