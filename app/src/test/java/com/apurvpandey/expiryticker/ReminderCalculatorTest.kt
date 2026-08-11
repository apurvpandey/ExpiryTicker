package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.ReminderCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReminderCalculatorTest {

    @Test
    fun `reminder date is daysBeforeReminder days before due date`() {
        val dueDate = LocalDate.of(2024, 7, 15)
        assertEquals(
            LocalDate.of(2024, 7, 8),
            ReminderCalculator.reminderDate(dueDate, reminderDaysBefore = 7)
        )
    }

    @Test
    fun `reminder date with 0 days before equals due date`() {
        val dueDate = LocalDate.of(2024, 7, 15)
        assertEquals(dueDate, ReminderCalculator.reminderDate(dueDate, reminderDaysBefore = 0))
    }

    @Test
    fun `reminder date with 30 days before crosses month boundary`() {
        val dueDate = LocalDate.of(2024, 3, 10)
        assertEquals(
            LocalDate.of(2024, 2, 9),
            ReminderCalculator.reminderDate(dueDate, reminderDaysBefore = 30)
        )
    }

    @Test
    fun `reminder date with 1 day before due on Jan 1 falls in December`() {
        val dueDate = LocalDate.of(2024, 1, 1)
        assertEquals(
            LocalDate.of(2023, 12, 31),
            ReminderCalculator.reminderDate(dueDate, reminderDaysBefore = 1)
        )
    }
}
