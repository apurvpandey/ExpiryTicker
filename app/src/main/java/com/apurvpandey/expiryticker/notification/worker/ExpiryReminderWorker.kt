package com.apurvpandey.expiryticker.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apurvpandey.expiryticker.ExpiryTickerApplication
import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ExpiryReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L).takeIf { it > 0 } ?: return Result.failure()
        val container = (applicationContext as ExpiryTickerApplication).container
        // Read the current row so edits, completion and deletion are respected.
        val item = container.expiryItemRepository.observeById(itemId).first()
            ?: return Result.success()
        if (item.isCompleted) return Result.success()

        ExpiryNotificationManager.showReminder(
            context = applicationContext,
            item = item,
            daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), item.dueDate).toInt()
        )
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val KEY_KIND = "kind"
    }
}
