package com.apurvpandey.expiryticker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apurvpandey.expiryticker.ExpiryTickerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reminders are scheduled as delays from "now", so a new time zone or a manually changed clock
 * would leave them firing at the wrong hour. Plan them again whenever either changes.
 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ExpiryTickerApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                container.rescheduleAllReminders()
            } finally {
                pending.finish()
            }
        }
    }
}
