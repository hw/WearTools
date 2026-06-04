package app.tanh.tools_ftw.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import app.tanh.tools_ftw.BuildConfig
import app.tanh.tools_ftw.R
import app.tanh.tools_ftw.presentation.theme.ToolsFtwTheme

@Composable
fun HelpScreen() {
    val listState = rememberTransformingLazyColumnState()
    val appName = stringResource(R.string.app_name)

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
                    text = stringResource(R.string.help_header),
                    modifier = HelpTextModifier,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            helpSection(
                title = R.string.help_navigate_title,
                body = R.string.help_navigate_body,
            )
            helpSection(
                title = R.string.help_level_title,
                body = R.string.help_level_body,
            )
            helpSection(
                title = R.string.help_compass_title,
                body = R.string.help_compass_body,
            )
            helpSection(
                title = R.string.help_true_north_title,
                body = R.string.help_true_north_body,
            )
            helpSection(
                title = R.string.help_low_accuracy_title,
                body = R.string.help_low_accuracy_body,
            )
            helpSection(
                title = R.string.help_developer_title,
                body = R.string.help_developer_body,
                textAlign = TextAlign.Center,
                bodyArgs = arrayOf(appName, BuildConfig.BUILD_DATE_TIME),
            )

        }
    }
}

private fun androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope.helpSection(
    @StringRes title: Int,
    @StringRes body: Int,
    textAlign: TextAlign = TextAlign.Start,
    bodyArgs: Array<Any> = emptyArray(),
) {
    item {
        Text(
            text = stringResource(title),
            modifier = HelpTextModifier.padding(top = 8.dp),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            textAlign = textAlign,
        )
    }
    item {
        Text(
            text = if (bodyArgs.isEmpty()) stringResource(body) else stringResource(body, *bodyArgs),
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
    ToolsFtwTheme {
        HelpScreen()
    }
}
