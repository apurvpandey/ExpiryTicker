package com.apurvpandey.expiryticker.core.util

import com.apurvpandey.expiryticker.domain.model.ExpiryItem

object ReminderText {
    /** All reminder offsets for an item, most advance notice first, without duplicates. */
    fun allOffsets(item: ExpiryItem): List<Int> =
        (listOf(item.reminderDaysBefore) + item.extraReminderDays)
            .filter { it >= 0 }
            .distinct()
            .sortedDescending()
}
