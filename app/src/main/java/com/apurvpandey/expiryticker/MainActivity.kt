package com.apurvpandey.expiryticker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.apurvpandey.expiryticker.presentation.navigation.ExpiryTickerApp
import com.apurvpandey.expiryticker.presentation.theme.AppTheme
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTickerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ExpiryTickerApplication).container
        val initialItemId = intent?.getLongExtra(EXTRA_ITEM_ID, -1L)?.takeIf { it > 0 }

        setContent {
            val theme by container.appPreferences.appTheme
                .collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)

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
