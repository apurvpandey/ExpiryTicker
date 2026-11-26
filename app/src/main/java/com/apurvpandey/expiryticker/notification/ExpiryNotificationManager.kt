package com.apurvpandey.expiryticker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.apurvpandey.expiryticker.MainActivity
import com.apurvpandey.expiryticker.R
import com.apurvpandey.expiryticker.domain.model.ExpiryItem

object ExpiryNotificationManager {

    const val CHANNEL_ID = "expiry_reminders"
    private const val CHANNEL_NAME = "Expiry reminders"
    private const val CHANNEL_DESCRIPTION = "Reminders for upcoming expiries and renewals"
    private const val TEST_NOTIFICATION_ID = 0

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESCRIPTION }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun bodyFor(daysUntilDue: Int, renews: Boolean): String {
        val verb = if (renews) "Renews" else "Expires"
        return when {
            daysUntilDue < -1 -> "Overdue by ${-daysUntilDue} days."
            daysUntilDue == -1 -> "Overdue since yesterday."
            daysUntilDue == 0 -> "$verb today."
            daysUntilDue == 1 -> "$verb tomorrow."
            else -> "$verb in $daysUntilDue days."
        }
    }

    fun showReminder(context: Context, item: ExpiryItem, daysUntilDue: Int) {
        val body = bodyFor(daysUntilDue, renews = item.recurrence.name != "NONE")
        val itemId = item.id

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ITEM_ID, itemId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, itemId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.title)
            .setContentText(body)
            .setSubText(item.category.displayName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Mark done", actionIntent(context, itemId, NotificationActionReceiver.ACTION_DONE))
            .addAction(0, "Remind tomorrow", actionIntent(context, itemId, NotificationActionReceiver.ACTION_SNOOZE))
            .build()

        post(context, itemId.toInt(), notification)
    }

    /** Lets the user verify that notifications reach them (used from Settings). */
    fun showTest(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Reminders are working")
            .setContentText("You will get notifications like this before items expire.")
            .setAutoCancel(true)
            .build()
        post(context, TEST_NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context, itemId: Long) {
        NotificationManagerCompat.from(context).cancel(itemId.toInt())
    }

    private fun actionIntent(context: Context, itemId: Long, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_ITEM_ID, itemId)
        }
        // Distinct request code per (item, action) so the two buttons never share an intent.
        val requestCode = itemId.toInt() * 2 + if (action == NotificationActionReceiver.ACTION_DONE) 0 else 1
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Each item has its own notification slot so updates replace, not stack.
    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted; silently skip.
        }
    }
}
