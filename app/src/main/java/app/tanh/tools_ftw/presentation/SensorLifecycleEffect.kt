package app.tanh.tools_ftw.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
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

@Composable
fun <Controller, Reading> ActiveSensorLifecycleEffect(
    isActive: Boolean,
    keys: Array<Any?>,
    createController: (onReading: (Reading) -> Unit) -> Controller,
    onStart: (Controller) -> Unit,
    onStop: (Controller) -> Unit,
    onReading: (Reading) -> Unit,
    onFirstReading: () -> Unit = {},
) {
    val currentOnReading = rememberUpdatedState(onReading)
    val currentOnFirstReading = rememberUpdatedState(onFirstReading)
    if (!isActive) return

    val firstReading = remember(*keys) { mutableStateOf(true) }
    val controller =
        remember(*keys) {
            createController { reading ->
                currentOnReading.value(reading)
                if (firstReading.value) {
                    firstReading.value = false
                    currentOnFirstReading.value()
                }
            }
        }
    SensorLifecycleEffect(controller, onStart = { onStart(controller) }, onStop = { onStop(controller) })
}
