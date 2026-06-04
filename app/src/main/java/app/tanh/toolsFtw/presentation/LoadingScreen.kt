package app.tanh.toolsFtw.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.tanh.toolsFtw.R
import app.tanh.toolsFtw.presentation.theme.ToolsFtwTheme

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@WearPreviewDevices
@Composable
private fun LoadingPreview() {
    ToolsFtwTheme {
        LoadingScreen()
    }
}
