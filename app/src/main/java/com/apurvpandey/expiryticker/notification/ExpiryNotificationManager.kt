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

object ExpiryNotificationManager {

    const val CHANNEL_ID = "expiry_reminders"
    private const val CHANNEL_NAME = "Expiry reminders"
    private const val CHANNEL_DESCRIPTION = "Reminders for upcoming expiries and renewals"

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

    fun showReminder(
        context: Context,
        itemId: Long,
        itemTitle: String,
        daysUntilDue: Int
    ) {
        val body = when (daysUntilDue) {
            0 -> "Expires today — tap to view or mark as renewed."
            1 -> "Expires tomorrow."
            else -> "Expires in $daysUntilDue days."
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ITEM_ID, itemId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(itemTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Each item has its own notification slot so updates replace, not stack.
        try {
            NotificationManagerCompat.from(context).notify(itemId.toInt(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted; silently skip.
        }
    }
}
