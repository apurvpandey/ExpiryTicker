package com.apurvpandey.expiryticker.domain.model

import java.time.Instant
import java.time.LocalDate

data class ExpiryItem(
    val id: Long = 0L,
    val title: String,
    val category: RenewalCategory,
    val dueDate: LocalDate,
    val reminderDaysBefore: Int = 7,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val notes: String = "",
    // stored in paise (1 INR = 100 paise) to avoid floating-point issues; null if not set
    val amountPaise: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val lastRenewedAt: Instant? = null
)
