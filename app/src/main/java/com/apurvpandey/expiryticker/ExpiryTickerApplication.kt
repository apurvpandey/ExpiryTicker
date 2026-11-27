package com.apurvpandey.expiryticker

import android.app.Application
import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager

class ExpiryTickerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) enableStrictMode()
        val start = android.os.SystemClock.elapsedRealtime()
        container = AppContainer(this)
        android.util.Log.d("Startup", "AppContainer built in ${android.os.SystemClock.elapsedRealtime() - start} ms")
        ExpiryNotificationManager.createChannel(this)
    }

    /** Debug builds log any disk or network work on the main thread, with the stack that did it. */
    private fun enableStrictMode() {
        android.os.StrictMode.setThreadPolicy(
            android.os.StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
        )
        android.os.StrictMode.setVmPolicy(
            android.os.StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedSqlLiteObjects().penaltyLog().build()
        )
    }
}
