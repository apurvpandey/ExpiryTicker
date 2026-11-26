package com.apurvpandey.expiryticker.notification

import com.apurvpandey.expiryticker.presentation.common.labelRes
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import android.content.res.Resources
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
    private const val TEST_NOTIFICATION_ID = 0

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notif_channel_desc) }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /** What a reminder says about timing, independent of language so it can be unit tested. */
    data class DayPhrase(val kind: Kind, val days: Int, val renews: Boolean) {
        enum class Kind { OVERDUE_DAYS, OVERDUE_YESTERDAY, TODAY, TOMORROW, IN_DAYS }
    }

    fun phraseFor(daysUntilDue: Int, renews: Boolean): DayPhrase = when {
        daysUntilDue < -1 -> DayPhrase(DayPhrase.Kind.OVERDUE_DAYS, -daysUntilDue, renews)
        daysUntilDue == -1 -> DayPhrase(DayPhrase.Kind.OVERDUE_YESTERDAY, 1, renews)
        daysUntilDue == 0 -> DayPhrase(DayPhrase.Kind.TODAY, 0, renews)
        daysUntilDue == 1 -> DayPhrase(DayPhrase.Kind.TOMORROW, 1, renews)
        else -> DayPhrase(DayPhrase.Kind.IN_DAYS, daysUntilDue, renews)
    }

    fun DayPhrase.format(res: Resources): String = when (kind) {
        DayPhrase.Kind.OVERDUE_DAYS -> res.getQuantityString(R.plurals.notif_overdue_days, days, days)
        DayPhrase.Kind.OVERDUE_YESTERDAY -> res.getString(R.string.notif_overdue_yesterday)
        DayPhrase.Kind.TODAY ->
            res.getString(if (renews) R.string.notif_today_renews else R.string.notif_today_expires)
        DayPhrase.Kind.TOMORROW ->
            res.getString(if (renews) R.string.notif_tomorrow_renews else R.string.notif_tomorrow_expires)
        DayPhrase.Kind.IN_DAYS -> res.getQuantityString(
            if (renews) R.plurals.notif_in_days_renews else R.plurals.notif_in_days_expires, days, days
        )
    }

    fun showReminder(context: Context, item: ExpiryItem, daysUntilDue: Int) {
        val body = phraseFor(daysUntilDue, renews = item.recurrence != RecurrenceType.NONE).format(context.resources)
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
            .setSubText(context.getString(item.category.labelRes()))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, context.getString(R.string.notif_action_done), actionIntent(context, itemId, NotificationActionReceiver.ACTION_DONE))
            .addAction(0, context.getString(R.string.notif_action_snooze), actionIntent(context, itemId, NotificationActionReceiver.ACTION_SNOOZE))
            .build()

        post(context, itemId.toInt(), notification)
    }

    /** Lets the user verify that notifications reach them (used from Settings). */
    fun showTest(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_test_title))
            .setContentText(context.getString(R.string.notif_test_text))
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
