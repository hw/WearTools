package app.tanh.toolsftw.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.tanh.toolsftw.R
import app.tanh.toolsftw.presentation.theme.ToolsFtwColors
import app.tanh.toolsftw.presentation.theme.ToolsFtwTheme
import app.tanh.toolsftw.sensor.LevelReading
import app.tanh.toolsftw.sensor.LevelSensorController
import app.tanh.toolsftw.sensor.SensorMath
import app.tanh.toolsftw.settings.AppPreferences
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SpiritLevelScreen(
    preferences: AppPreferences,
    isActive: Boolean,
    onFirstReading: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var rawReading by remember { mutableStateOf(LevelReading()) }
    var zeroX by remember { mutableFloatStateOf(preferences.levelZeroX) }
    var zeroY by remember { mutableFloatStateOf(preferences.levelZeroY) }
    val reading = SensorMath.calibratedLevel(rawReading, zeroX, zeroY)

    ActiveSensorLifecycleEffect(
        isActive = isActive,
        keys = arrayOf(context),
        createController = { onReading: (LevelReading) -> Unit ->
            LevelSensorController(context, onReading)
        },
        onStart = LevelSensorController::start,
        onStop = LevelSensorController::stop,
        onReading = { nextReading -> rawReading = nextReading },
        onFirstReading = onFirstReading,
    )

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
@OptIn(ExperimentalFoundationApi::class)
private fun SpiritLevelFace(
    reading: LevelReading,
    onSetZero: () -> Unit,
    onResetZero: () -> Unit,
) {
    val setZeroLabel = stringResource(R.string.action_set_level_zero)
    val resetZeroLabel = stringResource(R.string.action_reset_level_zero)
    Box(
        modifier =
            Modifier.fillMaxSize()
                .combinedClickable(
                    onClickLabel = setZeroLabel,
                    onLongClickLabel = resetZeroLabel,
                    onClick = onSetZero,
                    onLongClick = onResetZero,
                ),
    ) {
        if (reading.available) {
            // Key the formatting on the displayed tenths so we don't re-format on sub-0.1\u00b0 jitter.
            val xText = remember((reading.xDegrees * 10f).roundToInt()) {
                String.format(Locale.US, "X %+.1f\u00b0", reading.xDegrees)
            }
            val yText = remember((reading.yDegrees * 10f).roundToInt()) {
                String.format(Locale.US, "Y %+.1f\u00b0", reading.yDegrees)
            }
            val readingColor = if (reading.isCentered) ToolsFtwColors.LevelGreen else Color.White
            Bullseye(reading, Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = xText,
                    color = readingColor,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = yText,
                    color = readingColor,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.level_sensor_unavailable),
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
    val accent = if (reading.isCentered) ToolsFtwColors.LevelGreen else ToolsFtwColors.LevelAmber
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()
        val bubbleRadius = 15.dp.toPx()
        val bubbleCenter =
            Offset(
                x = center.x + bubbleX * (radius - bubbleRadius),
                y = center.y + bubbleY * (radius - bubbleRadius),
            )
        drawCircle(ToolsFtwColors.OuterRing, radius, center, style = Stroke(width = 2.dp.toPx()))
        drawCircle(ToolsFtwColors.InnerRing, radius * 0.64f, center, style = Stroke(width = 1.dp.toPx()))
        drawCircle(ToolsFtwColors.InnerRing, radius * 0.31f, center, style = Stroke(width = 1.dp.toPx()))
        drawLine(ToolsFtwColors.InnerRing, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y))
        drawLine(ToolsFtwColors.InnerRing, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius))
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

@WearPreviewDevices
@Composable
private fun SpiritLevelPreview() {
    ToolsFtwTheme {
        SpiritLevelFace(
            reading = LevelReading(xDegrees = 3f, yDegrees = -2f),
            onSetZero = {},
            onResetZero = {},
        )
    }
}
