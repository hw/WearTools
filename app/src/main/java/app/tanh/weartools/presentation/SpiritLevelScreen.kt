package app.tanh.weartools.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.tanh.weartools.presentation.theme.WearToolsTheme
import app.tanh.weartools.sensor.LevelReading
import app.tanh.weartools.sensor.LevelSensorController
import app.tanh.weartools.sensor.SensorMath
import app.tanh.weartools.settings.AppPreferences
import java.util.Locale

@Composable
fun SpiritLevelScreen(
    preferences: AppPreferences,
    isActive: Boolean,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var rawReading by remember { mutableStateOf(LevelReading()) }
    var zeroX by remember { mutableFloatStateOf(preferences.levelZeroX) }
    var zeroY by remember { mutableFloatStateOf(preferences.levelZeroY) }
    val reading = SensorMath.calibratedLevel(rawReading, zeroX, zeroY)

    if (isActive) {
        val controller = remember(context) { LevelSensorController(context) { rawReading = it } }
        SensorLifecycleEffect(controller, onStart = controller::start, onStop = controller::stop)
    }

    SpiritLevelFace(
        reading = reading,
        onSetZero = {
            zeroX = rawReading.xDegrees
            zeroY = rawReading.yDegrees
            preferences.levelZeroX = zeroX
            preferences.levelZeroY = zeroY
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onResetZero = {
            zeroX = 0f
            zeroY = 0f
            preferences.resetLevelZero()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        },
    )
}

@Composable
private fun SpiritLevelFace(
    reading: LevelReading,
    onSetZero: () -> Unit,
    onResetZero: () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onSetZero() },
                        onLongPress = { onResetZero() },
                    )
                },
    ) {
        if (reading.available) {
            Bullseye(reading, Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = String.format(Locale.US, "X %+.1f\u00b0", reading.xDegrees),
                    color = if (reading.isCentered) LevelGreen else Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = String.format(Locale.US, "Y %+.1f\u00b0", reading.yDegrees),
                    color = if (reading.isCentered) LevelGreen else Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            Text(
                text = "Level sensor unavailable",
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Bullseye(
    reading: LevelReading,
    modifier: Modifier = Modifier,
) {
    val (bubbleX, bubbleY) = SensorMath.bubbleOffset(reading.xDegrees, reading.yDegrees)
    val accent = if (reading.isCentered) LevelGreen else LevelAmber
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()
        val bubbleRadius = 15.dp.toPx()
        val bubbleCenter =
            Offset(
                x = center.x + bubbleX * (radius - bubbleRadius),
                y = center.y + bubbleY * (radius - bubbleRadius),
            )
        drawCircle(Color(0xFF57636D), radius, center, style = Stroke(width = 2.dp.toPx()))
        drawCircle(Color(0xFF35414A), radius * 0.64f, center, style = Stroke(width = 1.dp.toPx()))
        drawCircle(Color(0xFF35414A), radius * 0.31f, center, style = Stroke(width = 1.dp.toPx()))
        drawLine(Color(0xFF35414A), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y))
        drawLine(Color(0xFF35414A), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius))
        drawCircle(
            color = accent.copy(alpha = 0.24f),
            radius = bubbleRadius + 4.dp.toPx(),
            center = bubbleCenter,
        )
        drawCircle(
            color = accent,
            radius = bubbleRadius,
            center = bubbleCenter,
        )
    }
}

private val LevelGreen = Color(0xFF7CFF6B)
private val LevelAmber = Color(0xFFFFC84A)
@WearPreviewDevices
@Composable
private fun SpiritLevelPreview() {
    WearToolsTheme {
        SpiritLevelFace(
            reading = LevelReading(xDegrees = 3f, yDegrees = -2f),
            onSetZero = {},
            onResetZero = {},
        )
    }
}
