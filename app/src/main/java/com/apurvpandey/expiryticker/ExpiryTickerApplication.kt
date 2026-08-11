package com.apurvpandey.expiryticker

import android.app.Application
import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager

class ExpiryTickerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ExpiryNotificationManager.createChannel(this)
    }
}
