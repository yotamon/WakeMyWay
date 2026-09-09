package com.wakemyway.app.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.wakemyway.core.motion.MotionEvidenceEmission
import com.wakemyway.core.motion.MotionEvidenceExtractor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Thin Android adapter around the pure MotionEvidenceExtractor.
 *
 * It observes sensors only while explicitly started, never persists raw samples, and never
 * decides Wake Runtime phase/outcome itself.
 */
class AndroidMotionObserver(
    context: Context,
    private val extractor: MotionEvidenceExtractor = MotionEvidenceExtractor(),
    private val onEvidence: (MotionEvidenceEmission) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.applicationContext
        .getSystemService(SensorManager::class.java)

    private val accelerationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val orientationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private var observing = false
    private var lastAvailability = availability(registered = false)

    fun start(): MotionSensorAvailability {
        if (observing) return lastAvailability

        extractor.reset()
        val accelerationRegistered = accelerationSensor?.let(::register) ?: false
        val orientationRegistered = orientationSensor?.let(::register) ?: false
        observing = accelerationRegistered || orientationRegistered
        lastAvailability = MotionSensorAvailability(
            observing = observing,
            accelerationSource = accelerationSensor?.sourceName(),
            orientationSource = orientationSensor?.sourceName(),
            accelerationRegistered = accelerationRegistered,
            orientationRegistered = orientationRegistered,
        )
        return lastAvailability
    }

    fun stop() {
        if (!observing) return
        sensorManager.unregisterListener(this)
        observing = false
        extractor.reset()
        lastAvailability = availability(registered = false)
    }

    fun availability(): MotionSensorAvailability = lastAvailability

    override fun onSensorChanged(event: SensorEvent) {
        val emissions = when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> extractor.onAcceleration(
                timestampNanos = event.timestamp,
                linearAccelerationMagnitude = vectorMagnitude(event.values),
            )

            Sensor.TYPE_ACCELEROMETER -> extractor.onAcceleration(
                timestampNanos = event.timestamp,
                linearAccelerationMagnitude = abs(
                    vectorMagnitude(event.values) - SensorManager.GRAVITY_EARTH.toDouble(),
                ),
            )

            Sensor.TYPE_ROTATION_VECTOR -> extractor.onTilt(
                timestampNanos = event.timestamp,
                tiltDegrees = tiltFromRotationVector(event.values),
            )

            Sensor.TYPE_GRAVITY -> tiltFromGravity(event.values)?.let { tilt ->
                extractor.onTilt(event.timestamp, tilt)
            } ?: emptyList()

            else -> emptyList()
        }

        emissions.forEach(onEvidence)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun register(sensor: Sensor): Boolean = sensorManager.registerListener(
        this,
        sensor,
        SensorManager.SENSOR_DELAY_UI,
    )

    private fun availability(registered: Boolean): MotionSensorAvailability = MotionSensorAvailability(
        observing = registered,
        accelerationSource = accelerationSensor?.sourceName(),
        orientationSource = orientationSensor?.sourceName(),
        accelerationRegistered = registered && accelerationSensor != null,
        orientationRegistered = registered && orientationSensor != null,
    )

    private fun Sensor.sourceName(): MotionSensorSource = when (type) {
        Sensor.TYPE_LINEAR_ACCELERATION -> MotionSensorSource.LINEAR_ACCELERATION
        Sensor.TYPE_ACCELEROMETER -> MotionSensorSource.ACCELEROMETER_FALLBACK
        Sensor.TYPE_ROTATION_VECTOR -> MotionSensorSource.ROTATION_VECTOR
        Sensor.TYPE_GRAVITY -> MotionSensorSource.GRAVITY_FALLBACK
        else -> MotionSensorSource.UNKNOWN
    }

    private fun vectorMagnitude(values: FloatArray): Double {
        if (values.size < 3) return 0.0
        val x = values[0].toDouble()
        val y = values[1].toDouble()
        val z = values[2].toDouble()
        return sqrt(x * x + y * y + z * z)
    }

    private fun tiltFromRotationVector(values: FloatArray): Double {
        val rotation = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotation, values)
        val worldVerticalComponentOfDeviceZ = rotation[8].toDouble().coerceIn(-1.0, 1.0)
        return acos(worldVerticalComponentOfDeviceZ) * 180.0 / PI
    }

    private fun tiltFromGravity(values: FloatArray): Double? {
        if (values.size < 3) return null
        val magnitude = vectorMagnitude(values)
        if (magnitude < MIN_GRAVITY_MAGNITUDE) return null
        val normalizedZ = (values[2].toDouble() / magnitude).coerceIn(-1.0, 1.0)
        return acos(normalizedZ) * 180.0 / PI
    }

    private companion object {
        const val MIN_GRAVITY_MAGNITUDE = 1.0
    }
}

data class MotionSensorAvailability(
    val observing: Boolean,
    val accelerationSource: MotionSensorSource?,
    val orientationSource: MotionSensorSource?,
    val accelerationRegistered: Boolean,
    val orientationRegistered: Boolean,
) {
    val motionAvailable: Boolean = accelerationRegistered || orientationRegistered
}

enum class MotionSensorSource {
    LINEAR_ACCELERATION,
    ACCELEROMETER_FALLBACK,
    ROTATION_VECTOR,
    GRAVITY_FALLBACK,
    UNKNOWN,
}
