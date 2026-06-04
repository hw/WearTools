package app.tanh.toolsftw.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * App theme. The tool faces draw their own domain palette ([ToolsFtwColors]); the surrounding
 * chrome (help text, time text, progress indicator) uses the stock Wear Material 3 color scheme,
 * which renders correctly against the device's dark watch background.
 */
@Composable
fun ToolsFtwTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(content = content)
}
