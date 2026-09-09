package com.wakemyway.core.motion

import com.wakemyway.core.runtime.MotionEvidenceKind
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max

/**
 * Converts short-lived physical motion features into conservative Wake Runtime evidence.
 *
 * This class deliberately knows nothing about Android SensorEvent objects and retains only
 * bounded recent feature timestamps / anchors. Raw sensor streams must never be persisted.
 */
class MotionEvidenceExtractor(
    private val tuning: MotionEvidenceTuning = MotionEvidenceTuning(),
) {
    private val activeMovementSamplesNanos = ArrayDeque<Long>()
    private val lastEmissionNanos = mutableMapOf<MotionEvidenceKind, Long>()

    private var orientationAnchorDegrees: Double? = null
    private var lastHighAccelerationNanos: Long? = null
    private var pendingTiltChangeNanos: Long? = null
    private var pendingTiltDeltaDegrees: Double = 0.0

    fun onAcceleration(
        timestampNanos: Long,
        linearAccelerationMagnitude: Double,
    ): List<MotionEvidenceEmission> {
        require(timestampNanos >= 0) { "Motion timestamp must be non-negative" }
        require(linearAccelerationMagnitude.isFinite() && linearAccelerationMagnitude >= 0.0) {
            "Linear acceleration magnitude must be finite and non-negative"
        }

        pruneMovementWindow(timestampNanos)
        if (linearAccelerationMagnitude >= tuning.sustainedAccelerationThresholdMps2) {
            activeMovementSamplesNanos.addLast(timestampNanos)
        }
        if (linearAccelerationMagnitude >= tuning.pickupAccelerationThresholdMps2) {
            lastHighAccelerationNanos = timestampNanos
        }

        val emissions = mutableListOf<MotionEvidenceEmission>()
        pickupIfReady()?.let(emissions::add)
        sustainedMovementIfReady(timestampNanos)?.let(emissions::add)
        return emissions
    }

    fun onTilt(
        timestampNanos: Long,
        tiltDegrees: Double,
    ): List<MotionEvidenceEmission> {
        require(timestampNanos >= 0) { "Motion timestamp must be non-negative" }
        require(tiltDegrees.isFinite() && tiltDegrees in 0.0..180.0) {
            "Tilt must be a finite angle in the range 0..180 degrees"
        }

        val anchor = orientationAnchorDegrees
        if (anchor == null) {
            orientationAnchorDegrees = tiltDegrees
            return emptyList()
        }

        val delta = abs(tiltDegrees - anchor)
        if (delta >= tuning.pickupTiltChangeDegrees) {
            pendingTiltChangeNanos = timestampNanos
            pendingTiltDeltaDegrees = delta
        }

        val emissions = mutableListOf<MotionEvidenceEmission>()
        pickupIfReady()?.let(emissions::add)

        if (
            delta >= tuning.orientationChangeDegrees &&
            canEmit(MotionEvidenceKind.ORIENTATION_CHANGE, timestampNanos, tuning.orientationCooldown)
        ) {
            emissions += MotionEvidenceEmission(
                kind = MotionEvidenceKind.ORIENTATION_CHANGE,
                timestampNanos = timestampNanos,
                reason = "tilt_delta_degrees=${format(delta)}",
            )
            rememberEmission(MotionEvidenceKind.ORIENTATION_CHANGE, timestampNanos)
            orientationAnchorDegrees = tiltDegrees
        } else if (emissions.any { it.kind == MotionEvidenceKind.DEVICE_PICKUP }) {
            orientationAnchorDegrees = tiltDegrees
        }

        return emissions
    }

    fun reset() {
        activeMovementSamplesNanos.clear()
        lastEmissionNanos.clear()
        orientationAnchorDegrees = null
        lastHighAccelerationNanos = null
        pendingTiltChangeNanos = null
        pendingTiltDeltaDegrees = 0.0
    }

    private fun pickupIfReady(): MotionEvidenceEmission? {
        val accelerationAt = lastHighAccelerationNanos ?: return null
        val tiltAt = pendingTiltChangeNanos ?: return null
        val pairedWithin = tuning.pickupPairWindow.toNanos()
        if (abs(accelerationAt - tiltAt) > pairedWithin) return null
        if (pendingTiltDeltaDegrees < tuning.pickupTiltChangeDegrees) return null
        val evidenceAt = max(accelerationAt, tiltAt)
        if (!canEmit(MotionEvidenceKind.DEVICE_PICKUP, evidenceAt, tuning.pickupCooldown)) return null

        rememberEmission(MotionEvidenceKind.DEVICE_PICKUP, evidenceAt)
        lastHighAccelerationNanos = null
        pendingTiltChangeNanos = null
        val pairedDelta = pendingTiltDeltaDegrees
        pendingTiltDeltaDegrees = 0.0
        return MotionEvidenceEmission(
            kind = MotionEvidenceKind.DEVICE_PICKUP,
            timestampNanos = evidenceAt,
            reason = "acceleration_tilt_pair_delta_degrees=${format(pairedDelta)}",
        )
    }

    private fun sustainedMovementIfReady(nowNanos: Long): MotionEvidenceEmission? {
        pruneMovementWindow(nowNanos)
        if (activeMovementSamplesNanos.size < tuning.sustainedMinimumSamples) return null

        val span = activeMovementSamplesNanos.last() - activeMovementSamplesNanos.first()
        if (span < tuning.sustainedMinimumActiveDuration.toNanos()) return null
        if (!canEmit(MotionEvidenceKind.SUSTAINED_MOVEMENT, nowNanos, tuning.sustainedCooldown)) return null

        rememberEmission(MotionEvidenceKind.SUSTAINED_MOVEMENT, nowNanos)
        val sampleCount = activeMovementSamplesNanos.size
        activeMovementSamplesNanos.clear()
        return MotionEvidenceEmission(
            kind = MotionEvidenceKind.SUSTAINED_MOVEMENT,
            timestampNanos = nowNanos,
            reason = "movement_samples=$sampleCount span_ms=${span / NANOS_PER_MILLISECOND}",
        )
    }

    private fun pruneMovementWindow(nowNanos: Long) {
        val cutoff = nowNanos - tuning.sustainedWindow.toNanos()
        while (activeMovementSamplesNanos.isNotEmpty() && activeMovementSamplesNanos.first() < cutoff) {
            activeMovementSamplesNanos.removeFirst()
        }
    }

    private fun canEmit(
        kind: MotionEvidenceKind,
        timestampNanos: Long,
        cooldown: Duration,
    ): Boolean {
        val previous = lastEmissionNanos[kind] ?: return true
        return timestampNanos - previous >= cooldown.toNanos()
    }

    private fun rememberEmission(kind: MotionEvidenceKind, timestampNanos: Long) {
        lastEmissionNanos[kind] = timestampNanos
    }

    private fun format(value: Double): String = "%.1f".format(java.util.Locale.ROOT, value)

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

data class MotionEvidenceTuning(
    val pickupAccelerationThresholdMps2: Double = 1.6,
    val pickupTiltChangeDegrees: Double = 12.0,
    val pickupPairWindow: Duration = Duration.ofMillis(1_200),
    val pickupCooldown: Duration = Duration.ofSeconds(3),
    val orientationChangeDegrees: Double = 25.0,
    val orientationCooldown: Duration = Duration.ofSeconds(2),
    val sustainedAccelerationThresholdMps2: Double = 0.75,
    val sustainedWindow: Duration = Duration.ofMillis(1_600),
    val sustainedMinimumActiveDuration: Duration = Duration.ofMillis(700),
    val sustainedMinimumSamples: Int = 4,
    val sustainedCooldown: Duration = Duration.ofSeconds(4),
) {
    init {
        require(pickupAccelerationThresholdMps2 > 0.0)
        require(pickupTiltChangeDegrees in 0.0..180.0)
        require(orientationChangeDegrees in pickupTiltChangeDegrees..180.0)
        require(!pickupPairWindow.isNegative && !pickupPairWindow.isZero)
        require(!pickupCooldown.isNegative)
        require(!orientationCooldown.isNegative)
        require(sustainedAccelerationThresholdMps2 > 0.0)
        require(!sustainedWindow.isNegative && !sustainedWindow.isZero)
        require(!sustainedMinimumActiveDuration.isNegative && !sustainedMinimumActiveDuration.isZero)
        require(sustainedMinimumActiveDuration <= sustainedWindow)
        require(sustainedMinimumSamples >= 2)
        require(!sustainedCooldown.isNegative)
    }
}

data class MotionEvidenceEmission(
    val kind: MotionEvidenceKind,
    val timestampNanos: Long,
    val reason: String,
)
