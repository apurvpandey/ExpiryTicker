package com.apurvpandey.expiryticker.core.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DueBucket(val label: String) {
    OVERDUE("Overdue"),
    TODAY("Due today"),
    THIS_WEEK("Next 7 days"),
    THIS_MONTH("Next 30 days"),
    LATER("Later")
}

object ItemGrouping {
    fun bucketFor(dueDate: LocalDate, today: LocalDate): DueBucket {
        val days = ChronoUnit.DAYS.between(today, dueDate)
        return when {
            days < 0 -> DueBucket.OVERDUE
            days == 0L -> DueBucket.TODAY
            days <= 7 -> DueBucket.THIS_WEEK
            days <= 30 -> DueBucket.THIS_MONTH
            else -> DueBucket.LATER
        }
    }
}
