package com.apurvpandey.expiryticker.domain.model

import java.time.Instant
import java.time.LocalDate

/** One completed renewal (or completion) of an item; the basis of history and price tracking. */
data class RenewalRecord(
    val id: Long = 0L,
    val itemId: Long,
    val renewedAt: Instant,
    val dueBefore: LocalDate,
    /** Next due date for recurring items; null when the item was simply completed. */
    val dueAfter: LocalDate?,
    val amountPaise: Long?
)
