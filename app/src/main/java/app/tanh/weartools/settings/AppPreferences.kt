package app.tanh.weartools.settings

import android.content.Context

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
        context.getSharedPreferences("wear_tools_preferences", Context.MODE_PRIVATE)

    var lastTool: Tool
        get() =
            runCatching {
                Tool.valueOf(preferences.getString(KEY_LAST_TOOL, Tool.LEVEL.name)!!)
            }.getOrDefault(Tool.LEVEL)
        set(value) = preferences.edit().putString(KEY_LAST_TOOL, value.name).apply()

    var trueNorthEnabled: Boolean
        get() = preferences.getBoolean(KEY_TRUE_NORTH, true)
        set(value) = preferences.edit().putBoolean(KEY_TRUE_NORTH, value).apply()

    var altitudeUnit: AltitudeUnit
        get() =
            runCatching {
                AltitudeUnit.valueOf(preferences.getString(KEY_ALTITUDE_UNIT, AltitudeUnit.METERS.name)!!)
            }.getOrDefault(AltitudeUnit.METERS)
        set(value) = preferences.edit().putString(KEY_ALTITUDE_UNIT, value.name).apply()

    var levelZeroX: Float
        get() = preferences.getFloat(KEY_LEVEL_ZERO_X, 0f)
        set(value) = preferences.edit().putFloat(KEY_LEVEL_ZERO_X, value).apply()

    var levelZeroY: Float
        get() = preferences.getFloat(KEY_LEVEL_ZERO_Y, 0f)
        set(value) = preferences.edit().putFloat(KEY_LEVEL_ZERO_Y, value).apply()

    fun resetLevelZero() {
        preferences
            .edit()
            .remove(KEY_LEVEL_ZERO_X)
            .remove(KEY_LEVEL_ZERO_Y)
            .apply()
    }

    private companion object {
        const val KEY_LAST_TOOL = "last_tool"
        const val KEY_TRUE_NORTH = "true_north_enabled"
        const val KEY_ALTITUDE_UNIT = "altitude_unit"
        const val KEY_LEVEL_ZERO_X = "level_zero_x"
        const val KEY_LEVEL_ZERO_Y = "level_zero_y"
    }
}
