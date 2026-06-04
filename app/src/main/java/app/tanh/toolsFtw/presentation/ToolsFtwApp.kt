package app.tanh.toolsFtw.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.TimeText
import app.tanh.toolsFtw.location.LOCATION_PERMISSIONS
import app.tanh.toolsFtw.location.hasLocationPermission
import app.tanh.toolsFtw.settings.AppPreferences
import app.tanh.toolsFtw.settings.Tool
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ToolsFtwApp() {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val initialPage =
        when (preferences.lastTool) {
            Tool.COMPASS -> COMPASS_PAGE
            Tool.LEVEL -> LEVEL_PAGE
            null -> HELP_PAGE
        }
    val pagerState = rememberPagerState(initialPage = initialPage) { PAGE_COUNT }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionRefresh++
        }

    if (initialPage == HELP_PAGE) {
        LaunchedEffect(Unit) {
            delay(HELP_LOADING_TIMEOUT_MS)
            isLoading = false
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val tool =
                    when (page) {
                        LEVEL_PAGE -> Tool.LEVEL
                        COMPASS_PAGE -> Tool.COMPASS
                        else -> null
                    }
                if (tool == null) return@collect
                preferences.lastTool = tool
                if (
                    tool == Tool.COMPASS &&
                        preferences.trueNorthEnabled &&
                        !hasLocationPermission(context)
                ) {
                    locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                }
            }
    }

    Box {
        AppScaffold(
            timeText = { TimeText() },
        ) {
            HorizontalPagerScaffold(
                pagerState = pagerState,
                pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimationSpec,
            ) {
                HorizontalPager(
                    state = pagerState,
                    flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(pagerState),
                ) { page ->
                    AnimatedPage(pageIndex = page, pagerState = pagerState) {
                        when (page) {
                            LEVEL_PAGE ->
                                SpiritLevelScreen(
                                    preferences = preferences,
                                    isActive = pagerState.currentPage == LEVEL_PAGE,
                                    onFirstReading = { isLoading = false },
                                )
                            COMPASS_PAGE ->
                                CompassScreen(
                                    preferences = preferences,
                                    permissionRefresh = permissionRefresh,
                                    isActive = pagerState.currentPage == COMPASS_PAGE,
                                    onRequestLocationPermission = {
                                        locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                                    },
                                    onFirstReading = { isLoading = false },
                                )
                            HELP_PAGE -> HelpScreen()
                        }
                    }
                }
            }
        }
        if (isLoading) {
            LoadingScreen(modifier = Modifier.fillMaxSize())
        }
    }
}

private const val COMPASS_PAGE = 0
private const val LEVEL_PAGE = 1
private const val HELP_PAGE = 2
private const val PAGE_COUNT = 3
private const val HELP_LOADING_TIMEOUT_MS = 400L
