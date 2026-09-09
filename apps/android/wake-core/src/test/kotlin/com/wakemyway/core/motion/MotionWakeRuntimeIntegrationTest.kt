package com.wakemyway.core.motion

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeInputId
import com.wakemyway.core.runtime.WakePhase
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionWakeRuntimeIntegrationTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()

    @Test
    fun `same policy stays activating without motion but reaches orienting with distinct physical evidence`() {
        val base = activeNoResponseSession("base")
        assertEquals(WakePhase.ACTIVATING, base.phase)

        val extractor = MotionEvidenceExtractor()
        extractor.onTilt(ms(0), 0.0)

        val pickup = buildList {
            addAll(extractor.onAcceleration(ms(100), 2.0))
            addAll(extractor.onTilt(ms(300), 30.0))
        }
        assertEquals(listOf(MotionEvidenceKind.DEVICE_PICKUP), pickup.map { it.kind })

        val orientation = extractor.onTilt(ms(650), 70.0)
        assertEquals(listOf(MotionEvidenceKind.ORIENTATION_CHANGE), orientation.map { it.kind })

        val sustained = buildList {
            addAll(extractor.onAcceleration(ms(1_000), 0.9))
            addAll(extractor.onAcceleration(ms(1_250), 0.9))
            addAll(extractor.onAcceleration(ms(1_500), 0.9))
            addAll(extractor.onAcceleration(ms(1_800), 0.9))
        }
        assertTrue(sustained.any { it.kind == MotionEvidenceKind.SUSTAINED_MOVEMENT })

        var withMotion = base
        (pickup + orientation + sustained).forEachIndexed { index, evidence ->
            withMotion = runtime.reduce(
                withMotion,
                WakeInput.MotionObserved(
                    id = WakeInputId("motion-$index-${evidence.kind.name}"),
                    kind = evidence.kind,
                ),
                policy,
            ).snapshot
        }

        assertEquals(WakePhase.ORIENTING, withMotion.phase)
        assertEquals(policy.activationThreshold, runtime.diagnostics(withMotion, policy).activationScore)
    }

    private fun activeNoResponseSession(id: String) = runtime
        .initial(WakeSessionId(id), policy)
        .let { runtime.reduce(it, WakeInput.AlarmFired(WakeInputId("$id-alarm")), policy).snapshot }
        .let {
            runtime.reduce(
                it,
                WakeInput.SilenceElapsed(WakeInputId("$id-silence-1"), Duration.ofSeconds(15)),
                policy,
            ).snapshot
        }
        .let {
            runtime.reduce(
                it,
                WakeInput.SilenceElapsed(WakeInputId("$id-silence-2"), Duration.ofSeconds(15)),
                policy,
            ).snapshot
        }

    private fun ms(value: Long): Long = value * 1_000_000L
}
