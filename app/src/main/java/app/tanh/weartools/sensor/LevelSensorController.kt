package app.tanh.weartools.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class LevelSensorController(
    context: Context,
    private val onReading: (LevelReading) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var filtered = Vector3(0f, 0f, 0f)
    private var initialized = false

    fun start() {
        if (sensor == null) {
            onReading(LevelReading(available = false))
            return
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val current = Vector3(event.values[0], event.values[1], event.values[2])
        filtered =
            if (!initialized || event.sensor.type == Sensor.TYPE_GRAVITY) {
                current
            } else {
                Vector3(
                    x = lowPass(current.x, filtered.x),
                    y = lowPass(current.y, filtered.y),
                    z = lowPass(current.z, filtered.z),
                )
            }
        initialized = true
        onReading(SensorMath.levelAngles(filtered))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun lowPass(input: Float, previous: Float): Float =
        previous + FILTER_ALPHA * (input - previous)

    private companion object {
        const val FILTER_ALPHA = 0.18f
    }
}
