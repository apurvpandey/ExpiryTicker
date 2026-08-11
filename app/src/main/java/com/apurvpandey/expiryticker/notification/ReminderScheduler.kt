package com.apurvpandey.expiryticker.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.apurvpandey.expiryticker.core.util.ReminderCalculator
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.notification.worker.ExpiryReminderWorker
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    // Notifications are delivered at this hour in device local time
    private val reminderHourOfDay = 9

    fun scheduleReminder(item: ExpiryItem) {
        if (item.isCompleted) return

        val reminderDate = ReminderCalculator.reminderDate(item.dueDate, item.reminderDaysBefore)
        val delayMs = ReminderCalculator.delayMillisUntil(reminderDate, reminderHourOfDay)

        // If both reminder date AND due date are in the past, skip — item is already overdue
        // and the dashboard exposes that state visually.
        if (delayMs < 0 && item.dueDate.isBefore(LocalDate.now())) return

        // If reminder date has passed but due date is still upcoming, fire ASAP (0 delay).
        val effectiveDelayMs = delayMs.coerceAtLeast(0L)

        val inputData = workDataOf(
            ExpiryReminderWorker.KEY_ITEM_ID to item.id,
            ExpiryReminderWorker.KEY_TITLE to item.title,
            ExpiryReminderWorker.KEY_CATEGORY_LABEL to item.category.displayName,
            ExpiryReminderWorker.KEY_DUE_DATE to item.dueDate.toString()
        )

        val request = OneTimeWorkRequestBuilder<ExpiryReminderWorker>()
            .setInitialDelay(effectiveDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(workTag(item.id))
            .build()

        // REPLACE cancels any previously queued reminder for this item and replaces it
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(item.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminder(itemId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(itemId))
    }

    companion object {
        fun uniqueWorkName(itemId: Long) = "expiry_reminder_$itemId"
        fun workTag(itemId: Long) = "expiry_reminder_$itemId"
    }
}
