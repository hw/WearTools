package app.tanh.toolsftw.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.tanh.toolsftw.presentation.theme.ToolsFtwTheme
import app.tanh.toolsftw.settings.AppPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Build prefs here so getSharedPreferences() kicks off its background disk load before
        // composition reads it, giving the load a head start and minimizing the first-read wait.
        val preferences = AppPreferences(applicationContext)
        setContent {
            ToolsFtwTheme {
                ToolsFtwApp(preferences)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }
}
