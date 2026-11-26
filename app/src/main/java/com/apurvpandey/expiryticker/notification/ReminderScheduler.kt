package com.apurvpandey.expiryticker.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.apurvpandey.expiryticker.core.util.ReminderCalculator
import com.apurvpandey.expiryticker.core.util.ReminderText
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.notification.worker.ExpiryReminderWorker
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Schedules up to two reminders per item: one [ExpiryItem.reminderDaysBefore] days ahead and one
 * on the due date. A third, short-lived "remind me tomorrow" reminder comes from [snooze].
 */
class ReminderScheduler(private val context: Context) {

    /** Hour of day (device local time) at which reminders fire; kept in sync with settings. */
    @Volatile
    var reminderHour: Int = DEFAULT_HOUR

    fun scheduleReminder(item: ExpiryItem) {
        cancelReminder(item.id)
        if (item.isCompleted) return
        val today = LocalDate.now()

        // Every offset becomes one reminder at the chosen hour; 0 is the due day itself.
        ReminderText.allOffsets(item).forEach { days ->
            val date = ReminderCalculator.reminderDate(item.dueDate, days)
            val delay = ReminderCalculator.delayMillisUntil(date, reminderHour)
            when {
                days == item.reminderDaysBefore && days > 0 ->
                    // A missed main reminder still fires right away while the item is not overdue.
                    if (delay >= 0 || !item.dueDate.isBefore(today)) {
                        enqueue(item.id, KIND_ADVANCE, advanceName(item.id), delay.coerceAtLeast(0L))
                    }
                days == 0 -> if (delay >= 0) enqueue(item.id, KIND_DUE, dueName(item.id), delay)
                // Extra reminders that are already in the past are simply skipped.
                else -> if (delay >= 0) enqueue(item.id, KIND_ADVANCE, extraName(item.id, days), delay)
            }
        }
        // The due-day reminder is always there, whatever the offsets say.
        if (0 !in ReminderText.allOffsets(item)) {
            val dueDelay = ReminderCalculator.delayMillisUntil(item.dueDate, reminderHour)
            if (dueDelay >= 0) enqueue(item.id, KIND_DUE, dueName(item.id), dueDelay)
        }
    }

    /** One-off reminder tomorrow at the usual hour; does not change the due date. */
    fun snooze(itemId: Long, days: Long = 1) {
        val delay = ReminderCalculator.delayMillisUntil(LocalDate.now().plusDays(days), reminderHour)
        enqueue(itemId, KIND_SNOOZE, snoozeName(itemId), delay.coerceAtLeast(0L))
    }

    fun cancelReminder(itemId: Long) {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(itemTag(itemId))
        wm.cancelUniqueWork(advanceName(itemId))
        wm.cancelUniqueWork(dueName(itemId))
        wm.cancelUniqueWork(snoozeName(itemId))
    }

    private fun enqueue(itemId: Long, kind: String, uniqueName: String, delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<ExpiryReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(itemTag(itemId))
            .setInputData(
                workDataOf(
                    ExpiryReminderWorker.KEY_ITEM_ID to itemId,
                    ExpiryReminderWorker.KEY_KIND to kind
                )
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val DEFAULT_HOUR = 9
        const val KIND_ADVANCE = "advance"
        const val KIND_DUE = "due"
        const val KIND_SNOOZE = "snooze"

        fun advanceName(itemId: Long) = "expiry_reminder_$itemId"
        fun dueName(itemId: Long) = "expiry_due_$itemId"
        fun snoozeName(itemId: Long) = "expiry_snooze_$itemId"
        fun extraName(itemId: Long, days: Int) = "expiry_extra_${itemId}_$days"
        fun itemTag(itemId: Long) = "expiry_item_$itemId"
    }
}
