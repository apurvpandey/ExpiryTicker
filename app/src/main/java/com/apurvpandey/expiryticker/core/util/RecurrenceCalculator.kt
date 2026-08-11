package com.apurvpandey.expiryticker.core.util

import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import java.time.LocalDate

object RecurrenceCalculator {

    // Uses LocalDate.plusMonths/plusYears which handles end-of-month correctly
    // e.g. Jan 31 + 1 month = Feb 28/29 (not March 3)
    fun calculateNextDueDate(currentDueDate: LocalDate, recurrence: RecurrenceType): LocalDate =
        when (recurrence) {
            RecurrenceType.NONE -> currentDueDate
            RecurrenceType.MONTHLY -> currentDueDate.plusMonths(1)
            RecurrenceType.QUARTERLY -> currentDueDate.plusMonths(3)
            RecurrenceType.HALF_YEARLY -> currentDueDate.plusMonths(6)
            RecurrenceType.YEARLY -> currentDueDate.plusYears(1)
        }
}
