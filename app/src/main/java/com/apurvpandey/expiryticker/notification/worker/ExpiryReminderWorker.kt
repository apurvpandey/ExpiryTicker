package com.apurvpandey.expiryticker.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ExpiryReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L).takeIf { it > 0 } ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val dueDateStr = inputData.getString(KEY_DUE_DATE) ?: return Result.failure()

        val dueDate = runCatching { LocalDate.parse(dueDateStr) }.getOrNull() ?: return Result.failure()
        val daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate).toInt()

        ExpiryNotificationManager.showReminder(
            context = applicationContext,
            itemId = itemId,
            itemTitle = title,
            daysUntilDue = daysUntilDue
        )

        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val KEY_TITLE = "title"
        const val KEY_DUE_DATE = "due_date"
    }
}
