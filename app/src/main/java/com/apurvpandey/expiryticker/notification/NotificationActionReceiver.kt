package com.apurvpandey.expiryticker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apurvpandey.expiryticker.ExpiryTickerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Handles the "Mark done" and "Remind tomorrow" buttons on a reminder notification. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L).takeIf { it > 0 } ?: return
        val container = (context.applicationContext as ExpiryTickerApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = container.expiryItemRepository.observeById(itemId).first()
                if (item != null) {
                    when (intent.action) {
                        ACTION_DONE -> container.itemActions.markDone(item)
                        ACTION_SNOOZE -> container.reminderScheduler.snooze(itemId)
                    }
                }
                ExpiryNotificationManager.cancel(context, itemId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.apurvpandey.expiryticker.action.DONE"
        const val ACTION_SNOOZE = "com.apurvpandey.expiryticker.action.SNOOZE"
        const val EXTRA_ITEM_ID = "item_id"
    }
}
