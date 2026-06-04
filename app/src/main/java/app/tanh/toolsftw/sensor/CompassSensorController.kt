package app.tanh.toolsftw.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.abs

class CompassSensorController(
    context: Context,
    private val locationData: () -> CompassLocationData,
    private val trueNorthEnabled: () -> Boolean,
    private val onReading: (CompassReading) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private var accuracy: Int? = null
    private var lastPublishedReading: CompassReading? = null
    private var lastPublishedTrueNorthEnabled = false
    private var lastPublishedAtMillis = 0L

    fun start() {
        lastPublishedReading = null
        lastPublishedAtMillis = 0L
        gravity = null
        geomagnetic = null
        accuracy = null
        when {
            rotationVector != null -> {
                if (!sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)) {
                    startFallbackSensors()
                    return
                }
                // Magnetometer is only needed here to surface accuracy (the low-accuracy
                // indicator) via onAccuracyChanged, so sample it slowly to save power.
                magneticField?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
            accelerometer != null && magneticField != null -> startFallbackSensors()
            else -> onReading(CompassReading(available = false))
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                publishHeading()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                gravity = smoothed(event.values, gravity)
                publishFallbackHeading()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // When the rotation vector drives the heading, mag events only carry accuracy
                // (handled in onAccuracyChanged); skip the unused smoothing work.
                if (rotationVector == null) {
                    geomagnetic = smoothed(event.values, geomagnetic)
                    publishFallbackHeading()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, newAccuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            accuracy = newAccuracy
        }
    }

    private fun publishFallbackHeading() {
        val currentGravity = gravity ?: return
        val currentGeomagnetic = geomagnetic ?: return
        if (SensorManager.getRotationMatrix(rotationMatrix, null, currentGravity, currentGeomagnetic)) {
            publishHeading()
        }
    }

    private fun startFallbackSensors() {
        if (accelerometer == null || magneticField == null) {
            onReading(CompassReading(available = false))
            return
        }
        val accelerometerRegistered =
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        val magneticFieldRegistered =
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI)
        if (!accelerometerRegistered || !magneticFieldRegistered) {
            sensorManager.unregisterListener(this)
            onReading(CompassReading(available = false))
        }
    }

    private fun publishHeading() {
        SensorManager.getOrientation(rotationMatrix, orientation)
        val magneticHeading = SensorMath.normalizeHeading(orientation[0] * 180f / PI.toFloat())
        val shouldUseTrueNorth = trueNorthEnabled()
        val lowAccuracy = accuracy?.let { it <= SensorManager.SENSOR_STATUS_ACCURACY_LOW } ?: false
        val now = SystemClock.elapsedRealtime()
        val previousReading = lastPublishedReading
        if (
            previousReading != null &&
                !shouldPublishCompassReading(
                    previous = previousReading,
                    previousTrueNorthEnabled = lastPublishedTrueNorthEnabled,
                    nextMagneticHeadingDegrees = magneticHeading,
                    nextTrueNorthEnabled = shouldUseTrueNorth,
                    nextLowAccuracy = lowAccuracy,
                    elapsedMillis = now - lastPublishedAtMillis,
                )
        ) {
            return
        }
        val location = locationData()
        val reading =
            SensorMath.compassReading(
                magneticHeadingDegrees = magneticHeading,
                trueNorthEnabled = shouldUseTrueNorth,
                declinationDegrees = location.declinationDegrees,
                lowAccuracy = lowAccuracy,
                aslMeters = location.aslMeters,
                coordinates = location.coordinates,
            )
        onReading(reading)
        lastPublishedReading = reading
        lastPublishedTrueNorthEnabled = shouldUseTrueNorth
        lastPublishedAtMillis = now
    }

    private fun shouldPublishCompassReading(
        previous: CompassReading,
        previousTrueNorthEnabled: Boolean,
        nextMagneticHeadingDegrees: Float,
        nextTrueNorthEnabled: Boolean,
        nextLowAccuracy: Boolean,
        elapsedMillis: Long,
    ): Boolean {
        if (
            previous.lowAccuracy != nextLowAccuracy ||
                previousTrueNorthEnabled != nextTrueNorthEnabled ||
                elapsedMillis >= SensorTuning.PUBLISH_MAX_INTERVAL_MILLIS
        ) {
            return true
        }
        return headingDelta(
            previous.magneticHeadingDegrees,
            nextMagneticHeadingDegrees,
        ) >= COMPASS_HEADING_EPSILON_DEGREES
    }

    private fun headingDelta(
        previousDegrees: Float,
        nextDegrees: Float,
    ): Float {
        val delta = abs(SensorMath.normalizeHeading(nextDegrees - previousDegrees))
        return minOf(delta, 360f - delta)
    }

    // Mutates and returns [previous] (privately owned) to avoid a per-event allocation; the
    // first sample copies the framework-owned [values] so we never retain its reused array.
    private fun smoothed(values: FloatArray, previous: FloatArray?): FloatArray {
        if (previous == null) return values.copyOf()
        for (index in previous.indices) {
            previous[index] += SensorTuning.FILTER_ALPHA * (values[index] - previous[index])
        }
        return previous
    }

    private companion object {
        const val COMPASS_HEADING_EPSILON_DEGREES = 0.5f
    }
}
