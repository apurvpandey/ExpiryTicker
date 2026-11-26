package com.apurvpandey.expiryticker

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apurvpandey.expiryticker.presentation.navigation.ExpiryTickerApp
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
        val initialItemId = intent?.getLongExtra(EXTRA_ITEM_ID, -1L)?.takeIf { it > 0 }

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
                    initialItemId = initialItemId
                )
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }
}
