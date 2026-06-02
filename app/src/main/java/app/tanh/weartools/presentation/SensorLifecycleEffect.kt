package app.tanh.weartools.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Binds a sensor controller (or any start/stop resource) to the host's lifecycle: starts on
 * ON_START, stops on ON_STOP, and stops when leaving composition. Pass the controller and any
 * inputs it depends on as [keys] so the effect restarts when they change.
 */
@Composable
fun SensorLifecycleEffect(
    vararg keys: Any?,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val lifecycle = (LocalContext.current as LifecycleOwner).lifecycle
    DisposableEffect(lifecycle, *keys) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> onStart()
                    Lifecycle.Event.ON_STOP -> onStop()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) onStart()
        onDispose {
            lifecycle.removeObserver(observer)
            onStop()
        }
    }
}
