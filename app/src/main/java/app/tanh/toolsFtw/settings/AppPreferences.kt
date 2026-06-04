package app.tanh.toolsFtw.settings

import android.content.Context
import androidx.core.content.edit

enum class Tool {
    LEVEL,
    COMPASS,
}

enum class AltitudeUnit {
    METERS,
    FEET,
}

class AppPreferences(context: Context) {
    private val preferences =
        context.getSharedPreferences("tools_ftw_preferences", Context.MODE_PRIVATE)

    /** The last tool the user opened, or null if none has been saved yet (fresh install). */
    var lastTool: Tool?
        get() =
            preferences.getString(KEY_LAST_TOOL, null)?.let { name ->
                runCatching { Tool.valueOf(name) }.getOrNull()
            }
        set(value) {
            if (value != null) preferences.edit { putString(KEY_LAST_TOOL, value.name) }
        }

    var trueNorthEnabled: Boolean
        get() = preferences.getBoolean(KEY_TRUE_NORTH, true)
        set(value) = preferences.edit { putBoolean(KEY_TRUE_NORTH, value) }

    var altitudeUnit: AltitudeUnit
        get() =
            runCatching {
                AltitudeUnit.valueOf(preferences.getString(KEY_ALTITUDE_UNIT, AltitudeUnit.METERS.name)!!)
            }.getOrDefault(AltitudeUnit.METERS)
        set(value) = preferences.edit { putString(KEY_ALTITUDE_UNIT, value.name) }

    var levelZeroX: Float
        get() = preferences.getFloat(KEY_LEVEL_ZERO_X, 0f)
        set(value) = preferences.edit { putFloat(KEY_LEVEL_ZERO_X, value) }

    var levelZeroY: Float
        get() = preferences.getFloat(KEY_LEVEL_ZERO_Y, 0f)
        set(value) = preferences.edit { putFloat(KEY_LEVEL_ZERO_Y, value) }

    fun resetLevelZero() {
        preferences.edit {
            remove(KEY_LEVEL_ZERO_X)
            remove(KEY_LEVEL_ZERO_Y)
        }
    }

    private companion object {
        const val KEY_LAST_TOOL = "last_tool"
        const val KEY_TRUE_NORTH = "true_north_enabled"
        const val KEY_ALTITUDE_UNIT = "altitude_unit"
        const val KEY_LEVEL_ZERO_X = "level_zero_x"
        const val KEY_LEVEL_ZERO_Y = "level_zero_y"
    }
}
