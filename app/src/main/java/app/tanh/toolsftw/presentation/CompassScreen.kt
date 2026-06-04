package app.tanh.toolsftw.presentation

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.tanh.toolsftw.R
import app.tanh.toolsftw.location.TrueNorthProvider
import app.tanh.toolsftw.location.hasLocationPermission
import app.tanh.toolsftw.presentation.theme.ToolsFtwColors
import app.tanh.toolsftw.presentation.theme.ToolsFtwTheme
import app.tanh.toolsftw.sensor.CompassReading
import app.tanh.toolsftw.sensor.CompassSensorController
import app.tanh.toolsftw.sensor.NorthMode
import app.tanh.toolsftw.sensor.SensorMath
import app.tanh.toolsftw.settings.AppPreferences
import app.tanh.toolsftw.settings.AltitudeUnit
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
    onFirstReading: () -> Unit = {},
) {
    val context = LocalContext.current
    var trueNorthEnabled by remember { mutableStateOf(preferences.trueNorthEnabled) }
    var altitudeUnit by remember { mutableStateOf(preferences.altitudeUnit) }
    var reading by remember { mutableStateOf(CompassReading()) }
    val currentTrueNorthEnabled = rememberUpdatedState(trueNorthEnabled)
    val trueNorthProvider = remember(context, permissionRefresh) { TrueNorthProvider(context) }

    ActiveSensorLifecycleEffect(
        isActive = isActive,
        keys = arrayOf(context, permissionRefresh),
        createController = { onReading: (CompassReading) -> Unit ->
            CompassSensorController(
                context = context,
                locationData = trueNorthProvider::locationData,
                trueNorthEnabled = { currentTrueNorthEnabled.value },
                onReading = onReading,
            )
        },
        onStart = { controller ->
            if (currentTrueNorthEnabled.value) trueNorthProvider.start()
            controller.start()
        },
        onStop = { controller ->
            controller.stop()
            trueNorthProvider.stop()
        },
        onReading = { nextReading -> reading = nextReading },
        onFirstReading = onFirstReading,
    )

    CompassFace(
        reading = reading,
        altitudeUnit = altitudeUnit,
        onToggleTrueNorth = {
            trueNorthEnabled = !trueNorthEnabled
            preferences.trueNorthEnabled = trueNorthEnabled
            if (trueNorthEnabled) {
                trueNorthProvider.start()
                // Only prompt when we actually lack permission; launching the request when it is
                // already granted still fires the result callback and needlessly restarts the
                // provider and sensor controller.
                if (!hasLocationPermission(context)) onRequestLocationPermission()
            } else {
                trueNorthProvider.stop()
            }
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
    val toggleTrueNorthLabel = stringResource(R.string.action_toggle_true_north)
    val toggleAltitudeUnitLabel = stringResource(R.string.action_toggle_altitude_unit)
    Box(modifier = Modifier.fillMaxSize()) {
        if (reading.available) {
            // Re-format only when a displayed quantity actually changes (the integer heading,
            // cardinal, or accuracy flag), not on every sub-degree sensor update.
            val displayHeading = SensorMath.displayHeading(reading.headingDegrees)
            val cardinal = SensorMath.cardinalDirection(reading.headingDegrees)
            val headingText =
                remember(displayHeading, cardinal, reading.lowAccuracy) {
                    String.format(
                        Locale.US,
                        "%s%03d\u00b0  %s",
                        if (reading.lowAccuracy) "\u2248 " else "",
                        displayHeading,
                        cardinal,
                    )
                }
            val coordinatesText =
                remember(reading.latitudeDegrees, reading.longitudeDegrees) {
                    val latitude = reading.latitudeDegrees
                    val longitude = reading.longitudeDegrees
                    if (latitude != null && longitude != null) {
                        String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
                    } else {
                        null
                    }
                }
            CompassDial(reading, Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = headingText,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text =
                        when {
                            reading.mode == NorthMode.TRUE -> stringResource(R.string.compass_mode_true_north)
                            reading.trueNorthUnavailable -> stringResource(R.string.compass_mode_magnetic_no_fix)
                            else -> stringResource(R.string.compass_mode_magnetic)
                        },
                    color =
                        when {
                            reading.mode == NorthMode.TRUE -> ToolsFtwColors.TruePointer
                            reading.trueNorthUnavailable -> ToolsFtwColors.NorthFallback
                            else -> ToolsFtwColors.MagneticPointer
                        },
                    style =
                        when {
                            reading.trueNorthUnavailable -> MaterialTheme.typography.bodyExtraSmall
                            else -> MaterialTheme.typography.labelSmall
                        },
                    modifier =
                        Modifier.clickable(
                            onClickLabel = toggleTrueNorthLabel,
                            onClick = onToggleTrueNorth,
                        ),
                )
                if (coordinatesText != null) {
                    Text(
                        text = coordinatesText,
                        color = ToolsFtwColors.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            reading.aslMeters?.let { aslMeters ->
                Text(
                    text =
                        when (altitudeUnit) {
                            AltitudeUnit.METERS ->
                                stringResource(R.string.altitude_meters, aslMeters.roundToInt())
                            AltitudeUnit.FEET ->
                                stringResource(R.string.altitude_feet, (aslMeters * METERS_TO_FEET).roundToInt())
                        },
                    modifier =
                        Modifier.clickable(
                            onClickLabel = toggleAltitudeUnitLabel,
                            onClick = onToggleAltitudeUnit,
                        ),
                    color = ToolsFtwColors.MutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.compass_sensor_unavailable),
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
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val dialCache =
        remember(canvasSize, density) {
            if (canvasSize.width > 0 && canvasSize.height > 0) {
                createCompassDialCache(canvasSize, density)
            } else {
                null
            }
        }
    DisposableEffect(dialCache) {
        onDispose {
            dialCache?.bitmap?.recycle()
        }
    }
    Canvas(modifier = modifier.onSizeChanged { canvasSize = it }) {
        val cache = dialCache ?: return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = cache.radius
        drawContext.canvas.nativeCanvas.withSave {
            rotate(-reading.headingDegrees, center.x, center.y)
            drawBitmap(cache.bitmap, 0f, 0f, null)
        }
        if (reading.mode == NorthMode.TRUE) {
            reading.trueHeadingDegrees?.let { trueHeading ->
                drawNorthPointer(
                    center = center,
                    radius = radius,
                    headingDegrees = trueHeading,
                    color = ToolsFtwColors.TruePointer,
                    length = cache.longPointerLength,
                    strokeWidth = cache.pointerStroke,
                )
            }
            drawNorthPointer(
                center = center,
                radius = radius,
                headingDegrees = reading.magneticHeadingDegrees,
                color = ToolsFtwColors.MagneticPointer,
                length = cache.shortPointerLength,
                strokeWidth = cache.pointerStroke,
            )
        } else {
            drawNorthPointer(
                center = center,
                radius = radius,
                headingDegrees = reading.magneticHeadingDegrees,
                color = ToolsFtwColors.MagneticPointer,
                length = cache.longPointerLength,
                strokeWidth = cache.pointerStroke,
            )
            reading.trueHeadingDegrees?.let { trueHeading ->
                drawNorthPointer(
                    center = center,
                    radius = radius,
                    headingDegrees = trueHeading,
                    color = ToolsFtwColors.TruePointer,
                    length = cache.shortPointerLength,
                    strokeWidth = cache.pointerStroke,
                )
            }
        }
    }
}

private data class CompassDialCache(
    val bitmap: Bitmap,
    val radius: Float,
    val pointerStroke: Float,
    val longPointerLength: Float,
    val shortPointerLength: Float,
)

private fun createCompassDialCache(
    size: IntSize,
    density: Density,
): CompassDialCache {
    val bitmap = createBitmap(size.width, size.height)
    val canvas = AndroidCanvas(bitmap)
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val center = Offset(centerX, centerY)
    val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
        }
    val strokeThin: Float
    val strokeThick: Float
    val pointerStroke: Float
    val outerInset: Float
    val majorTickLength: Float
    val mediumTickLength: Float
    val minorTickLength: Float
    val cardinalTextInset: Float
    val intercardinalTextInset: Float
    val longPointerLength: Float
    val shortPointerLength: Float
    with(density) {
        strokeThin = 1.dp.toPx()
        strokeThick = 2.dp.toPx()
        pointerStroke = 4.dp.toPx()
        outerInset = 8.dp.toPx()
        majorTickLength = 13.dp.toPx()
        mediumTickLength = 8.dp.toPx()
        minorTickLength = 4.dp.toPx()
        cardinalTextInset = 27.dp.toPx()
        intercardinalTextInset = 20.dp.toPx()
        longPointerLength = 20.dp.toPx()
        shortPointerLength = 10.dp.toPx()
    }
    val radius = minOf(size.width, size.height) / 2f - outerInset

    strokePaint.color = ToolsFtwColors.OuterRing.toArgb()
    strokePaint.strokeWidth = strokeThick
    canvas.drawCircle(centerX, centerY, radius, strokePaint)
    strokePaint.color = ToolsFtwColors.InnerRing.toArgb()
    strokePaint.strokeWidth = strokeThin
    canvas.drawCircle(centerX, centerY, radius * 0.61f, strokePaint)

    strokePaint.color = android.graphics.Color.WHITE
    TickVectors.forEach { tick ->
        val outerX = center.x + tick.x * radius
        val outerY = center.y + tick.y * radius
        val innerRadius =
            radius -
                when {
                    tick.index % 6 == 0 -> majorTickLength
                    tick.index % 2 == 0 -> mediumTickLength
                    else -> minorTickLength
                }
        canvas.drawLine(
            center.x + tick.x * innerRadius,
            center.y + tick.y * innerRadius,
            outerX,
            outerY,
            strokePaint,
        )
    }

    textPaint.textSize = with(density) { 16.dp.toPx() }
    CardinalLabelVectors.forEach { label ->
        val textRadius = radius - cardinalTextInset
        canvas.drawRotatedLabel(
            label.text,
            center.x + label.x * textRadius,
            center.y + label.y * textRadius,
            label.bearingDegrees,
            textPaint,
        )
    }
    textPaint.textSize = with(density) { 12.dp.toPx() }
    IntercardinalLabelVectors.forEach { label ->
        val textRadius = radius - intercardinalTextInset
        canvas.drawRotatedLabel(
            label.text,
            center.x + label.x * textRadius,
            center.y + label.y * textRadius,
            label.bearingDegrees,
            textPaint,
        )
    }

    return CompassDialCache(
        bitmap = bitmap,
        radius = radius,
        pointerStroke = pointerStroke,
        longPointerLength = longPointerLength,
        shortPointerLength = shortPointerLength,
    )
}

private fun android.graphics.Canvas.drawRotatedLabel(
    label: String,
    x: Float,
    y: Float,
    bearingDegrees: Float,
    paint: Paint,
) {
    withSave {
        rotate(bearingDegrees, x, y)
        val baseline = y - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        drawText(label, x, baseline, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNorthPointer(
    center: Offset,
    radius: Float,
    headingDegrees: Float,
    color: Color,
    length: Float,
    strokeWidth: Float,
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
    drawLine(color = color, start = outer, end = inner, strokeWidth = strokeWidth)
}

private data class DialVector(
    val index: Int,
    val x: Float,
    val y: Float,
)

private data class LabelVector(
    val text: String,
    val bearingDegrees: Float,
    val x: Float,
    val y: Float,
)

private fun dialVector(
    index: Int,
    bearingDegrees: Float,
): DialVector {
    val angle = Math.toRadians(bearingDegrees.toDouble() - 90.0)
    return DialVector(
        index = index,
        x = cos(angle).toFloat(),
        y = sin(angle).toFloat(),
    )
}

private fun labelVector(
    text: String,
    bearingDegrees: Float,
): LabelVector {
    val angle = Math.toRadians(bearingDegrees.toDouble() - 90.0)
    return LabelVector(
        text = text,
        bearingDegrees = bearingDegrees,
        x = cos(angle).toFloat(),
        y = sin(angle).toFloat(),
    )
}

private val TickVectors = List(24) { index -> dialVector(index, index * 15f) }
private val CardinalLabelVectors =
    listOf(
        labelVector("N", 0f),
        labelVector("E", 90f),
        labelVector("S", 180f),
        labelVector("W", 270f),
    )
private val IntercardinalLabelVectors =
    listOf(
        labelVector("NE", 45f),
        labelVector("SE", 135f),
        labelVector("SW", 225f),
        labelVector("NW", 315f),
    )
private const val METERS_TO_FEET = 3.28084

@WearPreviewDevices
@Composable
private fun CompassPreview() {
    ToolsFtwTheme {
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

@WearPreviewDevices
@Composable
private fun CompassFallbackPreview() {
    ToolsFtwTheme {
        CompassFace(
            reading =
                CompassReading(
                    headingDegrees = 35f,
                    magneticHeadingDegrees = 35f,
                    mode = NorthMode.MAGNETIC,
                    trueNorthUnavailable = true,
                ),
            onToggleTrueNorth = {},
        )
    }
}
