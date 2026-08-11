package com.apurvpandey.expiryticker.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ReminderCalculator {

    fun reminderDate(dueDate: LocalDate, reminderDaysBefore: Int): LocalDate =
        dueDate.minusDays(reminderDaysBefore.toLong())

    // Returns ms delay from now until reminderDate at hourOfDay in device local time.
    // Negative value means the reminder time is already in the past.
    fun delayMillisUntil(reminderDate: LocalDate, hourOfDay: Int = 9): Long {
        val target = reminderDate
            .atTime(hourOfDay, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        return ChronoUnit.MILLIS.between(Instant.now(), target)
    }
}
