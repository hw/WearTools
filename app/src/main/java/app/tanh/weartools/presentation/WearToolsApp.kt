package app.tanh.weartools.presentation

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
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.TimeText
import app.tanh.weartools.location.LOCATION_PERMISSIONS
import app.tanh.weartools.location.hasLocationPermission
import app.tanh.weartools.settings.AppPreferences
import app.tanh.weartools.settings.Tool
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WearToolsApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val initialPage = if (preferences.lastTool == Tool.COMPASS) COMPASS_PAGE else LEVEL_PAGE
    val pagerState = rememberPagerState(initialPage = initialPage) { PAGE_COUNT }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionRefresh++
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
