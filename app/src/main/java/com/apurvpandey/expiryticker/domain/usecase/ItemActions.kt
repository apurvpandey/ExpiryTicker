package com.apurvpandey.expiryticker.domain.usecase

import com.apurvpandey.expiryticker.core.util.RecurrenceCalculator
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.notification.ReminderScheduler
import java.time.Instant

/**
 * Shared item mutations used by the dashboard, the detail screen and notification actions,
 * so repository state and scheduled reminders always change together.
 */
class ItemActions(
    private val repository: ExpiryItemRepository,
    private val scheduler: ReminderScheduler
) {
    /** Result of [markDone]; [previous] lets the caller offer an undo. */
    data class Outcome(val previous: ExpiryItem, val renewed: Boolean)

    suspend fun markDone(item: ExpiryItem): Outcome {
        val now = Instant.now()
        return if (item.recurrence != RecurrenceType.NONE) {
            val next = item.copy(
                dueDate = RecurrenceCalculator.calculateNextDueDate(item.dueDate, item.recurrence),
                lastRenewedAt = now,
                updatedAt = now
            )
            repository.update(next)
            scheduler.scheduleReminder(next)
            Outcome(item, renewed = true)
        } else {
            repository.update(item.copy(isCompleted = true, lastRenewedAt = now, updatedAt = now))
            scheduler.cancelReminder(item.id)
            Outcome(item, renewed = false)
        }
    }

    suspend fun delete(item: ExpiryItem) {
        scheduler.cancelReminder(item.id)
        repository.delete(item)
    }

    /** Restores a state captured before [markDone] or [delete]. */
    suspend fun restore(item: ExpiryItem) {
        repository.add(item) // insert uses REPLACE, so this also restores an edited row
        scheduler.scheduleReminder(item)
    }
}
