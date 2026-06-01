package app.tanh.weartools.presentation

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.tanh.weartools.location.TrueNorthProvider
import app.tanh.weartools.presentation.theme.WearToolsTheme
import app.tanh.weartools.sensor.CompassReading
import app.tanh.weartools.sensor.CompassSensorController
import app.tanh.weartools.sensor.NorthMode
import app.tanh.weartools.sensor.SensorMath
import app.tanh.weartools.settings.AppPreferences
import app.tanh.weartools.settings.AltitudeUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CompassScreen(
    preferences: AppPreferences,
    permissionRefresh: Int,
    isActive: Boolean,
    onRequestLocationPermission: () -> Unit,
) {
    val context = LocalContext.current
    var trueNorthEnabled by remember { mutableStateOf(preferences.trueNorthEnabled) }
    var altitudeUnit by remember { mutableStateOf(preferences.altitudeUnit) }
    var reading by remember { mutableStateOf(CompassReading()) }
    val trueNorthProvider = remember(context, permissionRefresh) { TrueNorthProvider(context) }

    if (isActive) {
        DisposableEffect(context, permissionRefresh, trueNorthEnabled) {
            val controller =
                CompassSensorController(
                    context = context,
                    locationData = trueNorthProvider::locationData,
                    trueNorthEnabled = { trueNorthEnabled },
                    onReading = { reading = it },
                )
            val lifecycle = (context as LifecycleOwner).lifecycle
            val observer =
                LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> controller.start()
                        Lifecycle.Event.ON_STOP -> controller.stop()
                        else -> Unit
                    }
                }
            lifecycle.addObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.start()
            onDispose {
                lifecycle.removeObserver(observer)
                controller.stop()
            }
        }
    }

    CompassFace(
        reading = reading,
        altitudeUnit = altitudeUnit,
        onToggleTrueNorth = {
            trueNorthEnabled = !trueNorthEnabled
            preferences.trueNorthEnabled = trueNorthEnabled
            if (trueNorthEnabled) onRequestLocationPermission()
        },
        onToggleAltitudeUnit = {
            altitudeUnit =
                if (altitudeUnit == AltitudeUnit.METERS) AltitudeUnit.FEET else AltitudeUnit.METERS
            preferences.altitudeUnit = altitudeUnit
        },
    )
}

@Composable
private fun CompassFace(
    reading: CompassReading,
    altitudeUnit: AltitudeUnit = AltitudeUnit.METERS,
    onToggleTrueNorth: () -> Unit,
    onToggleAltitudeUnit: () -> Unit = {},
) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onToggleTrueNorth() })
                },
    ) {
        if (reading.available) {
            CompassDial(reading, Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        String.format(
                            Locale.US,
                            "%03d\u00b0  %s",
                            SensorMath.displayHeading(reading.headingDegrees),
                            SensorMath.cardinalDirection(reading.headingDegrees),
                        ),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = if (reading.mode == NorthMode.TRUE) "TRUE NORTH" else "MAGNETIC",
                    color = if (reading.mode == NorthMode.TRUE) TruePointer else MagneticPointer,
                    style = MaterialTheme.typography.labelSmall,
                )
                if (reading.latitudeDegrees != null && reading.longitudeDegrees != null) {
                    Text(
                        text =
                            String.format(
                                Locale.US,
                                "%.5f, %.5f",
                                reading.latitudeDegrees,
                                reading.longitudeDegrees,
                            ),
                        color = CompassMutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                reading.aslMeters?.let { aslMeters ->
                    Text(
                        text =
                            when (altitudeUnit) {
                                AltitudeUnit.METERS -> "${aslMeters.roundToInt()} m ASL"
                                AltitudeUnit.FEET -> "${(aslMeters * METERS_TO_FEET).roundToInt()} ft ASL"
                            },
                        modifier = Modifier.clickable(onClick = onToggleAltitudeUnit),
                        color = CompassMutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
/*
                Text(
                    text = if (reading.lowAccuracy) "LOW ACCURACY" else "",
                    color = CompassMutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
 */
            }
        } else {
            Text(
                text = "Compass sensor unavailable",
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompassDial(
    reading: CompassReading,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textPaint =
        remember(density) {
            Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
        }
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()
        drawCircle(Color(0xFF57636D), radius, center, style = Stroke(width = 2.dp.toPx()))
        drawCircle(Color(0xFF35414A), radius * 0.61f, center, style = Stroke(width = 1.dp.toPx()))
        drawContext.canvas.save()
        drawContext.canvas.nativeCanvas.rotate(-reading.headingDegrees, center.x, center.y)
        repeat(24) { index ->
            val angle = Math.toRadians(index * 15.0 - 90.0)
            val outer =
                Offset(
                    center.x + cos(angle).toFloat() * radius,
                    center.y + sin(angle).toFloat() * radius,
                )
            val innerRadius =
                radius -
                    when {
                        index % 6 == 0 -> 13.dp.toPx()
                        index % 2 == 0 -> 8.dp.toPx()
                        else -> 4.dp.toPx()
                    }
            val inner =
                Offset(
                    center.x + cos(angle).toFloat() * innerRadius,
                    center.y + sin(angle).toFloat() * innerRadius,
                )
            drawLine(Color.White, inner, outer, strokeWidth = 1.dp.toPx())
        }
        textPaint.textSize = 16.dp.toPx()
        CardinalLabels.forEachIndexed { index, label ->
            val bearingDegrees = index * 90f
            val angle = Math.toRadians(bearingDegrees.toDouble() - 90.0)
            val textRadius = radius - 27.dp.toPx()
            drawContext.canvas.nativeCanvas.drawRotatedLabel(
                label,
                center.x + cos(angle).toFloat() * textRadius,
                center.y + sin(angle).toFloat() * textRadius,
                bearingDegrees,
                textPaint,
            )
        }
        textPaint.textSize = 12.dp.toPx()
        IntercardinalLabels.forEachIndexed { index, label ->
            val bearingDegrees = index * 90f + 45f
            val angle = Math.toRadians(bearingDegrees.toDouble() - 90.0)
            val textRadius = radius - 20.dp.toPx()
            drawContext.canvas.nativeCanvas.drawRotatedLabel(
                label,
                center.x + cos(angle).toFloat() * textRadius,
                center.y + sin(angle).toFloat() * textRadius,
                bearingDegrees,
                textPaint,
            )
        }
        drawContext.canvas.restore()
        if (reading.mode == NorthMode.TRUE) {
            reading.trueHeadingDegrees?.let { trueHeading ->
                drawNorthPointer(
                    center = center,
                    radius = radius,
                    headingDegrees = trueHeading,
                    color = TruePointer,
                    length = 20.dp.toPx(),
                )
            }
            drawNorthPointer(
                center = center,
                radius = radius,
                headingDegrees = reading.magneticHeadingDegrees,
                color = MagneticPointer,
                length = 10.dp.toPx(),
            )
        } else {
            drawNorthPointer(
                center = center,
                radius = radius,
                headingDegrees = reading.magneticHeadingDegrees,
                color = MagneticPointer,
                length = 20.dp.toPx(),
            )
            reading.trueHeadingDegrees?.let { trueHeading ->
                drawNorthPointer(
                    center = center,
                    radius = radius,
                    headingDegrees = trueHeading,
                    color = TruePointer,
                    length = 10.dp.toPx(),
                )
            }
        }
    }
}

private fun android.graphics.Canvas.drawRotatedLabel(
    label: String,
    x: Float,
    y: Float,
    bearingDegrees: Float,
    paint: Paint,
) {
    save()
    rotate(bearingDegrees, x, y)
    val baseline = y - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
    drawText(label, x, baseline, paint)
    restore()
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNorthPointer(
    center: Offset,
    radius: Float,
    headingDegrees: Float,
    color: Color,
    length: Float,
) {
    val angle = Math.toRadians(-headingDegrees.toDouble() - 90.0)
    val outer =
        Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius,
        )
    val inner =
        Offset(
            x = center.x + cos(angle).toFloat() * (radius - length),
            y = center.y + sin(angle).toFloat() * (radius - length),
        )
    drawLine(color = color, start = outer, end = inner, strokeWidth = 4.dp.toPx())
}

private val CompassAmber = Color(0xFFFFC84A)
private val CompassMutedText = Color(0xFF9AA5AE)
private val MagneticPointer = Color.White
private val TruePointer = Color.Red
private val CardinalLabels = arrayOf("N", "E", "S", "W")
private val IntercardinalLabels = arrayOf("NE", "SE", "SW", "NW")
private const val METERS_TO_FEET = 3.28084

@WearPreviewDevices
@Composable
private fun CompassPreview() {
    WearToolsTheme {
        CompassFace(
            reading =
                CompassReading(
                    headingDegrees = 35f,
                    magneticHeadingDegrees = 28f,
                    trueHeadingDegrees = 35f,
                    mode = NorthMode.TRUE,
                ),
            onToggleTrueNorth = {},
        )
    }
}
