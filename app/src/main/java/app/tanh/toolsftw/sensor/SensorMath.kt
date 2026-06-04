package app.tanh.toolsftw.sensor

import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

object SensorMath {
    private const val DEGREES_PER_RADIAN = 180f / Math.PI.toFloat()
    private val CARDINAL_DIRECTIONS =
        arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    fun levelAngles(gravity: Vector3): LevelReading =
        levelAngles(gravity.x, gravity.y, gravity.z)

    fun levelAngles(
        gravityX: Float,
        gravityY: Float,
        gravityZ: Float,
    ): LevelReading {
        val xDegrees =
            atan2(gravityX, sqrt(gravityY * gravityY + gravityZ * gravityZ)) *
                DEGREES_PER_RADIAN
        val yDegrees =
            atan2(gravityY, sqrt(gravityX * gravityX + gravityZ * gravityZ)) *
                DEGREES_PER_RADIAN
        return LevelReading(xDegrees = xDegrees, yDegrees = yDegrees)
    }

    fun calibratedLevel(
        reading: LevelReading,
        zeroX: Float,
        zeroY: Float,
    ): LevelReading =
        reading.copy(
            xDegrees = reading.xDegrees - zeroX,
            yDegrees = reading.yDegrees - zeroY,
        )

    fun bubbleOffset(
        xDegrees: Float,
        yDegrees: Float,
        maximumDegrees: Float = 15f,
    ): Pair<Float, Float> {
        val normalizedX = (xDegrees / maximumDegrees).coerceIn(-1f, 1f)
        val normalizedY = (yDegrees / maximumDegrees).coerceIn(-1f, 1f)
        val magnitudeSquared = normalizedX * normalizedX + normalizedY * normalizedY
        if (magnitudeSquared <= 1f) return normalizedX to normalizedY

        val scale = 1f / sqrt(magnitudeSquared)
        return normalizedX * scale to normalizedY * scale
    }

    fun normalizeHeading(headingDegrees: Float): Float =
        ((headingDegrees % 360f) + 360f) % 360f

    fun displayHeading(headingDegrees: Float): Int =
        normalizeHeading(headingDegrees).roundToInt() % 360

    fun trueHeading(
        magneticHeadingDegrees: Float,
        declinationDegrees: Float,
    ): Float = normalizeHeading(magneticHeadingDegrees + declinationDegrees)

    fun compassReading(
        magneticHeadingDegrees: Float,
        trueNorthEnabled: Boolean,
        declinationDegrees: Float?,
        lowAccuracy: Boolean,
        aslMeters: Double? = null,
        coordinates: Pair<Double, Double>? = null,
    ): CompassReading {
        val magneticHeading = normalizeHeading(magneticHeadingDegrees)
        val trueHeading = declinationDegrees?.let { trueHeading(magneticHeading, it) }
        val usingTrueNorth = trueNorthEnabled && trueHeading != null
        return CompassReading(
            headingDegrees =
                if (usingTrueNorth) {
                    trueHeading
                } else {
                    magneticHeading
                },
            magneticHeadingDegrees = magneticHeading,
            trueHeadingDegrees = trueHeading,
            mode = if (usingTrueNorth) NorthMode.TRUE else NorthMode.MAGNETIC,
            lowAccuracy = lowAccuracy,
            trueNorthUnavailable = trueNorthEnabled && !usingTrueNorth,
            aslMeters = aslMeters,
            latitudeDegrees = coordinates?.first,
            longitudeDegrees = coordinates?.second,
        )
    }

    fun cardinalDirection(headingDegrees: Float): String {
        val index =
            ((normalizeHeading(headingDegrees) + 22.5f) / 45f).toInt() % CARDINAL_DIRECTIONS.size
        return CARDINAL_DIRECTIONS[index]
    }
}
