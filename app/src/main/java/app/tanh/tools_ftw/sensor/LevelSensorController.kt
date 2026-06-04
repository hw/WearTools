package app.tanh.tools_ftw.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.abs

class LevelSensorController(
    context: Context,
    private val onReading: (LevelReading) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private var initialized = false
    private var lastPublishedReading: LevelReading? = null
    private var lastPublishedAtMillis = 0L

    fun start() {
        lastPublishedReading = null
        lastPublishedAtMillis = 0L
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
        val currentX = event.values[0]
        val currentY = event.values[1]
        val currentZ = event.values[2]
        if (!initialized || event.sensor.type == Sensor.TYPE_GRAVITY) {
            filteredX = currentX
            filteredY = currentY
            filteredZ = currentZ
        } else {
            filteredX = lowPass(currentX, filteredX)
            filteredY = lowPass(currentY, filteredY)
            filteredZ = lowPass(currentZ, filteredZ)
        }
        initialized = true
        val reading = SensorMath.levelAngles(filteredX, filteredY, filteredZ)
        val now = SystemClock.elapsedRealtime()
        val previousReading = lastPublishedReading
        if (
            previousReading == null ||
                shouldPublishLevelReading(
                    previous = previousReading,
                    next = reading,
                    elapsedMillis = now - lastPublishedAtMillis,
                )
        ) {
            onReading(reading)
            lastPublishedReading = reading
            lastPublishedAtMillis = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun lowPass(input: Float, previous: Float): Float =
        previous + FILTER_ALPHA * (input - previous)

    private fun shouldPublishLevelReading(
        previous: LevelReading,
        next: LevelReading,
        elapsedMillis: Long,
    ): Boolean {
        if (previous.available != next.available) return true
        if (!next.available) return false
        if (elapsedMillis >= LEVEL_READING_MAX_INTERVAL_MILLIS) return true
        return abs(previous.xDegrees - next.xDegrees) >= LEVEL_READING_EPSILON_DEGREES ||
            abs(previous.yDegrees - next.yDegrees) >= LEVEL_READING_EPSILON_DEGREES
    }

    private companion object {
        const val FILTER_ALPHA = 0.18f
        const val LEVEL_READING_MAX_INTERVAL_MILLIS = 80L
        const val LEVEL_READING_EPSILON_DEGREES = 0.05f
    }
}
