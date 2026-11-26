package com.apurvpandey.expiryticker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.presentation.lock.LockScreen
import com.apurvpandey.expiryticker.presentation.navigation.ExpiryTickerApp
import com.apurvpandey.expiryticker.presentation.navigation.LaunchRequest
import com.apurvpandey.expiryticker.presentation.theme.AppTheme
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTickerTheme
import com.apurvpandey.expiryticker.security.AppLock
import com.apurvpandey.expiryticker.security.LockPolicy

// FragmentActivity (still a ComponentActivity) because the biometric prompt needs one.
class MainActivity : FragmentActivity() {

    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    // App lock state. `decided` flips once the saved settings are known and the first lock
    // decision has been made; until then nothing is drawn, so no data can flash before the lock.
    private var locked by mutableStateOf(false)
    private var decided by mutableStateOf(false)
    private var lockEnabled = false
    private var coldStart = true
    private var stoppedAt: Long? = null
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !decided }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ExpiryTickerApplication).container
        launchRequest = LaunchRequest.from(intent)

        setContent {
            val savedTheme by container.appPreferences.appTheme
                .collectAsStateWithLifecycle(initialValue = null)
            val lockPref by container.appPreferences.appLockEnabled
                .collectAsStateWithLifecycle(initialValue = null)
            val currencyPref by container.appPreferences.currencyCode
                .collectAsStateWithLifecycle(initialValue = null)
            val theme = savedTheme ?: AppTheme.SYSTEM

            // Currency is applied before the first frame so amounts never flash in the wrong one.
            LaunchedEffect(currencyPref) { currencyPref?.let(CurrencyFormatter::setCurrency) }

            LaunchedEffect(savedTheme, lockPref, currencyPref) {
                val pref = lockPref ?: return@LaunchedEffect
                if (savedTheme == null || currencyPref == null) return@LaunchedEffect
                CurrencyFormatter.setCurrency(currencyPref!!)
                lockEnabled = pref
                if (coldStart) {
                    coldStart = false
                    locked = LockPolicy.shouldLock(enabled = pref, coldStart = true, msSinceBackgrounded = null)
                } else if (!pref) {
                    locked = false
                }
                decided = true
                promptIfNeeded()
            }

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
                when {
                    !decided -> Unit
                    locked -> LockScreen(onUnlock = ::promptIfNeeded)
                    else -> ExpiryTickerApp(container = container, launchRequest = launchRequest)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!coldStart) {
            val away = stoppedAt?.let { SystemClock.elapsedRealtime() - it }
            if (LockPolicy.shouldLock(lockEnabled, coldStart = false, msSinceBackgrounded = away)) locked = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Visible again: drop the privacy flag set in onPause and ask for the lock if needed.
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        promptIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        // Hides the app preview in the recent-apps switcher while the lock is on.
        if (lockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    private fun promptIfNeeded() {
        if (!decided || !locked || prompting) return
        if (!AppLock.isAvailable(this)) {
            // The device screen lock was removed: staying locked would trap the user out of their data.
            locked = false
            return
        }
        prompting = true
        AppLock.prompt(
            activity = this,
            onSuccess = { prompting = false; locked = false },
            onFailure = { prompting = false }
        )
    }

    // singleTop: a notification or widget tap while the app is open arrives here, not onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequest = LaunchRequest.from(intent)
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_OPEN_ADD = "open_add"
    }
}
