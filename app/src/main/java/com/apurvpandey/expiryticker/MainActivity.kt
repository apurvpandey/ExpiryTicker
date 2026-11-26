package com.apurvpandey.expiryticker

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apurvpandey.expiryticker.presentation.navigation.ExpiryTickerApp
import com.apurvpandey.expiryticker.presentation.navigation.LaunchRequest
import com.apurvpandey.expiryticker.presentation.theme.AppTheme
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTickerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var themeReady = false
        splash.setKeepOnScreenCondition { !themeReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ExpiryTickerApplication).container
        launchRequest = LaunchRequest.from(intent)

        setContent {
            // Hold the splash until the saved theme is known, so dark mode never flashes light.
            val savedTheme by container.appPreferences.appTheme
                .collectAsStateWithLifecycle(initialValue = null)
            if (savedTheme != null) themeReady = true
            val theme = savedTheme ?: AppTheme.SYSTEM

            val darkTheme = when (theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            // Re-apply system bar icon colours when the in-app theme overrides the system one.
            DisposableEffect(darkTheme) {
                val style = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            ExpiryTickerTheme(appTheme = theme) {
                ExpiryTickerApp(
                    container = container,
                    launchRequest = launchRequest
                )
            }
        }
    }

    // singleTop: a notification or widget tap while the app is open arrives here, not onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequest = LaunchRequest.from(intent)
    }

    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_OPEN_ADD = "open_add"
    }
}
