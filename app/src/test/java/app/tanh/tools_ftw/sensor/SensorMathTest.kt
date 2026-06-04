package app.tanh.tools_ftw.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SensorMathTest {
    @Test
    fun flatGravityProducesZeroAngles() {
        val reading = SensorMath.levelAngles(Vector3(0f, 0f, 9.81f))

        assertEquals(0f, reading.xDegrees, 0.001f)
        assertEquals(0f, reading.yDegrees, 0.001f)
        assertTrue(reading.isCentered)
    }

    @Test
    fun levelAnglesCalculateIndependentTilt() {
        val reading = SensorMath.levelAngles(Vector3(1f, 0f, 1f))

        assertEquals(45f, reading.xDegrees, 0.001f)
        assertEquals(0f, reading.yDegrees, 0.001f)
    }

    @Test
    fun calibrationSubtractsStoredBaseline() {
        val reading = SensorMath.calibratedLevel(LevelReading(12f, -7f), 10f, -4f)

        assertEquals(2f, reading.xDegrees, 0.001f)
        assertEquals(-3f, reading.yDegrees, 0.001f)
    }

    @Test
    fun bubbleOffsetIsConstrainedToCircle() {
        val (x, y) = SensorMath.bubbleOffset(15f, 15f)

        assertEquals(1f / sqrt(2f), x, 0.001f)
        assertEquals(1f / sqrt(2f), y, 0.001f)
    }

    @Test
    fun headingNormalizationWrapsBothDirections() {
        assertEquals(10f, SensorMath.normalizeHeading(370f), 0.001f)
        assertEquals(350f, SensorMath.normalizeHeading(-10f), 0.001f)
    }

    @Test
    fun displayedHeadingWrapsRoundedNorthToZero() {
        assertEquals(0, SensorMath.displayHeading(359.6f))
        assertEquals(0, SensorMath.displayHeading(360f))
        assertEquals(1, SensorMath.displayHeading(0.6f))
    }

    @Test
    fun trueHeadingAppliesDeclinationAndWraps() {
        assertEquals(3f, SensorMath.trueHeading(355f, 8f), 0.001f)
    }

    @Test
    fun cardinalDirectionHandlesBoundaries() {
        assertEquals("N", SensorMath.cardinalDirection(359f))
        assertEquals("NE", SensorMath.cardinalDirection(30f))
        assertEquals("W", SensorMath.cardinalDirection(270f))
    }

    @Test
    fun compassReadingUsesTrueNorthWhenDeclinationIsAvailable() {
        val reading =
            SensorMath.compassReading(
                magneticHeadingDegrees = 355f,
                trueNorthEnabled = true,
                declinationDegrees = 8f,
                lowAccuracy = false,
            )

        assertEquals(3f, reading.headingDegrees, 0.001f)
        assertEquals(355f, reading.magneticHeadingDegrees, 0.001f)
        assertEquals(3f, reading.trueHeadingDegrees!!, 0.001f)
        assertEquals(NorthMode.TRUE, reading.mode)
        assertEquals(false, reading.trueNorthUnavailable)
    }

    @Test
    fun compassReadingFallsBackToMagneticNorthWhenLocationIsUnavailable() {
        val reading =
            SensorMath.compassReading(
                magneticHeadingDegrees = 42f,
                trueNorthEnabled = true,
                declinationDegrees = null,
                lowAccuracy = true,
            )

        assertEquals(42f, reading.headingDegrees, 0.001f)
        assertEquals(42f, reading.magneticHeadingDegrees, 0.001f)
        assertEquals(null, reading.trueHeadingDegrees)
        assertEquals(NorthMode.MAGNETIC, reading.mode)
        assertEquals(true, reading.trueNorthUnavailable)
        assertEquals(true, reading.lowAccuracy)
    }

    @Test
    fun compassReadingRetainsTrueHeadingWhenMagneticModeIsSelected() {
        val reading =
            SensorMath.compassReading(
                magneticHeadingDegrees = 100f,
                trueNorthEnabled = false,
                declinationDegrees = -5f,
                lowAccuracy = false,
            )

        assertEquals(100f, reading.headingDegrees, 0.001f)
        assertEquals(100f, reading.magneticHeadingDegrees, 0.001f)
        assertEquals(95f, reading.trueHeadingDegrees!!, 0.001f)
        assertEquals(NorthMode.MAGNETIC, reading.mode)
    }
}
