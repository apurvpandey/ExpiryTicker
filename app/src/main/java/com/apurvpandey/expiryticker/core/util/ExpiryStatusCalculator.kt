package com.apurvpandey.expiryticker.core.util

import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ExpiryStatusCalculator {

    fun calculate(
        dueDate: LocalDate,
        isCompleted: Boolean,
        today: LocalDate = LocalDate.now()
    ): ExpiryStatus {
        if (isCompleted) return ExpiryStatus.Completed
        val daysBetween = ChronoUnit.DAYS.between(today, dueDate).toInt()
        return when {
            daysBetween < 0 -> ExpiryStatus.Overdue(daysOverdue = -daysBetween)
            daysBetween == 0 -> ExpiryStatus.DueToday
            else -> ExpiryStatus.Active(daysRemaining = daysBetween)
        }
    }
}
