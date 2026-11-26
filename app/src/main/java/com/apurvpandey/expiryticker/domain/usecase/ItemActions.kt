package com.apurvpandey.expiryticker.domain.usecase

import com.apurvpandey.expiryticker.core.util.RecurrenceCalculator
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.domain.repository.RenewalHistoryRepository
import com.apurvpandey.expiryticker.notification.ReminderScheduler
import java.time.Instant

/**
 * Shared item mutations used by the dashboard, the detail screen, the widget and notification
 * actions, so repository state, history and scheduled reminders always change together.
 */
class ItemActions(
    private val repository: ExpiryItemRepository,
    private val history: RenewalHistoryRepository,
    private val scheduler: ReminderScheduler
) {
    /** Result of [markDone]; [previous] and [historyId] let the caller offer an undo. */
    data class Outcome(val previous: ExpiryItem, val renewed: Boolean, val historyId: Long)

    /**
     * Completes a one-off item or renews a recurring one, and records it in the history.
     * [amountPaise] is what this renewal actually cost; it also becomes the expected cost
     * of the next cycle. Defaults to the item's current expected cost.
     */
    suspend fun markDone(item: ExpiryItem, amountPaise: Long? = item.amountPaise): Outcome {
        val now = Instant.now()
        val recurring = item.recurrence != RecurrenceType.NONE
        val nextDue = if (recurring) RecurrenceCalculator.calculateNextDueDate(item.dueDate, item.recurrence) else null

        val recordId = history.add(
            RenewalRecord(
                itemId = item.id, renewedAt = now, dueBefore = item.dueDate,
                dueAfter = nextDue, amountPaise = amountPaise
            )
        )
        if (nextDue != null) {
            val next = item.copy(dueDate = nextDue, lastRenewedAt = now, updatedAt = now, amountPaise = amountPaise)
            repository.update(next)
            scheduler.scheduleReminder(next)
        } else {
            repository.update(item.copy(isCompleted = true, lastRenewedAt = now, updatedAt = now, amountPaise = amountPaise))
            scheduler.cancelReminder(item.id)
        }
        return Outcome(item, renewed = recurring, historyId = recordId)
    }

    /** Puts a completed item back on the list (its history entry is kept). */
    suspend fun reopen(item: ExpiryItem) {
        val reopened = item.copy(isCompleted = false, updatedAt = Instant.now())
        repository.update(reopened)
        scheduler.scheduleReminder(reopened)
    }

    suspend fun delete(item: ExpiryItem) {
        scheduler.cancelReminder(item.id)
        repository.delete(item)
    }

    /** Restores a state captured before [markDone] or [delete]; drops the history entry [markDone] added. */
    suspend fun restore(item: ExpiryItem, historyId: Long? = null) {
        repository.add(item) // insert uses REPLACE, so this also restores an edited row
        historyId?.let { history.deleteById(it) }
        scheduler.scheduleReminder(item)
    }
}
