package app.tanh.weartools.sensor

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float,
)

data class LevelReading(
    val xDegrees: Float = 0f,
    val yDegrees: Float = 0f,
    val available: Boolean = true,
) {
    val isCentered: Boolean
        get() = available && kotlin.math.abs(xDegrees) <= 1f && kotlin.math.abs(yDegrees) <= 1f
}

enum class NorthMode {
    TRUE,
    MAGNETIC,
}

data class CompassReading(
    val headingDegrees: Float = 0f,
    val magneticHeadingDegrees: Float = headingDegrees,
    val trueHeadingDegrees: Float? = null,
    val mode: NorthMode = NorthMode.MAGNETIC,
    val available: Boolean = true,
    val lowAccuracy: Boolean = false,
    val trueNorthUnavailable: Boolean = false,
    val aslMeters: Double? = null,
    val latitudeDegrees: Double? = null,
    val longitudeDegrees: Double? = null,
)

data class CompassLocationData(
    val declinationDegrees: Float? = null,
    val aslMeters: Double? = null,
    val coordinates: Pair<Double, Double>? = null,
)
