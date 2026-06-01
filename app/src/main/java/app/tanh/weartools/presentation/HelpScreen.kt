package app.tanh.weartools.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.tanh.weartools.BuildConfig
import app.tanh.weartools.presentation.theme.WearToolsTheme

@Composable
fun HelpScreen() {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = listState,
        timeText = { TimeText() },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
        ) {
            item {
                Text(
                    text = "HELP",
                    modifier = HelpTextModifier,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            helpSection(
                title = "Navigate",
                body = "Swipe left or right to switch between level, compass, and help.",
            )
            helpSection(
                title = "Spirit level",
                body = "Tap the screen to set the current angle as zero. Long press to reset zero.",
            )
            helpSection(
                title = "Compass",
                body = "Tap the screen to switch between true north and magnetic north.",
            )
            helpSection(
                title = "True north",
                body = "True north uses your location. If location is unavailable, the compass falls back to magnetic north.",
            )
            helpSection(
                title = "Low accuracy",
                body = "Move the watch in a figure-eight motion away from metal objects to recalibrate the compass.\n\n",
            )
            helpSection(
                title = "Developer",
                body = "WearTools is developed by\n\nHockWoo Tan.\n\nBuilt ${BuildConfig.BUILD_DATE_TIME}\n\n",
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope.helpSection(
    title: String,
    body: String,
    textAlign: TextAlign = TextAlign.Start,
) {
    item {
        Text(
            text = title,
            modifier = HelpTextModifier.padding(top = 8.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            textAlign = textAlign,
        )
    }
    item {
        Text(
            text = body,
            modifier = HelpTextModifier,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = textAlign,
        )
    }
}

private val HelpTextModifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)

@WearPreviewDevices
@Composable
private fun HelpScreenPreview() {
    WearToolsTheme {
        HelpScreen()
    }
}
