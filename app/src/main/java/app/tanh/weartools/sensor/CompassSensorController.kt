package app.tanh.weartools.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI

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

    fun start() {
        when {
            rotationVector != null -> {
                sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
                magneticField?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
            }
            accelerometer != null && magneticField != null -> {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI)
            }
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
                geomagnetic = smoothed(event.values, geomagnetic)
                if (rotationVector == null) publishFallbackHeading()
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

    private fun publishHeading() {
        SensorManager.getOrientation(rotationMatrix, orientation)
        val magneticHeading = SensorMath.normalizeHeading(orientation[0] * 180f / PI.toFloat())
        val shouldUseTrueNorth = trueNorthEnabled()
        val location = locationData()
        onReading(
            SensorMath.compassReading(
                magneticHeadingDegrees = magneticHeading,
                trueNorthEnabled = shouldUseTrueNorth,
                declinationDegrees = location.declinationDegrees,
                lowAccuracy = accuracy?.let { it <= SensorManager.SENSOR_STATUS_ACCURACY_LOW } ?: false,
                aslMeters = location.aslMeters,
                coordinates = location.coordinates,
            ),
        )
    }

    private fun smoothed(values: FloatArray, previous: FloatArray?): FloatArray {
        if (previous == null) return values.copyOf()
        return FloatArray(3) { index ->
            previous[index] + FILTER_ALPHA * (values[index] - previous[index])
        }
    }

    private companion object {
        const val FILTER_ALPHA = 0.18f
    }
}
