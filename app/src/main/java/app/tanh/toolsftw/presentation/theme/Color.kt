package app.tanh.toolsftw.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * App-specific palette shared across the compass and spirit-level faces. These are domain colors
 * (pointers, dial rings, the level bubble) that don't map onto Material color roles, so they live
 * here as named constants instead of being hard-coded per screen.
 */
internal object ToolsFtwColors {
    // Dial / bullseye rings, shared by both faces.
    val OuterRing = Color(0xFF57636D)
    val InnerRing = Color(0xFF35414A)

    // Compass.
    val MutedText = Color(0xFF9AA5AE)
    val MagneticPointer = Color.White
    val TruePointer = Color.Red
    val NorthFallback = Color(0xFFFFC84A)

    // Spirit level.
    val LevelGreen = Color(0xFF7CFF6B)
    val LevelAmber = Color(0xFFFFC84A)
}
