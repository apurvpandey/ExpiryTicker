package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.RecurrenceCalculator
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurrenceCalculatorTest {

    @Test
    fun `NONE recurrence returns same date`() {
        val date = LocalDate.of(2024, 6, 15)
        assertEquals(date, RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.NONE))
    }

    @Test
    fun `MONTHLY adds one month`() {
        val date = LocalDate.of(2024, 6, 15)
        assertEquals(
            LocalDate.of(2024, 7, 15),
            RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.MONTHLY)
        )
    }

    @Test
    fun `MONTHLY from January 31 produces February end-of-month`() {
        val date = LocalDate.of(2024, 1, 31) // 2024 is leap year
        val next = RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.MONTHLY)
        // LocalDate.plusMonths clamps to last valid day: Feb 29 in 2024
        assertEquals(LocalDate.of(2024, 2, 29), next)
    }

    @Test
    fun `MONTHLY from January 31 in non-leap year produces Feb 28`() {
        val date = LocalDate.of(2023, 1, 31)
        val next = RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.MONTHLY)
        assertEquals(LocalDate.of(2023, 2, 28), next)
    }

    @Test
    fun `QUARTERLY adds three months`() {
        val date = LocalDate.of(2024, 3, 15)
        assertEquals(
            LocalDate.of(2024, 6, 15),
            RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.QUARTERLY)
        )
    }

    @Test
    fun `HALF_YEARLY adds six months`() {
        val date = LocalDate.of(2024, 1, 15)
        assertEquals(
            LocalDate.of(2024, 7, 15),
            RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.HALF_YEARLY)
        )
    }

    @Test
    fun `YEARLY adds one year`() {
        val date = LocalDate.of(2024, 6, 15)
        assertEquals(
            LocalDate.of(2025, 6, 15),
            RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.YEARLY)
        )
    }

    @Test
    fun `YEARLY from Feb 29 leap year produces Feb 28 next year`() {
        val date = LocalDate.of(2024, 2, 29)
        val next = RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.YEARLY)
        // 2025 is not a leap year; LocalDate.plusYears clamps to Feb 28
        assertEquals(LocalDate.of(2025, 2, 28), next)
    }

    @Test
    fun `MONTHLY December to January transitions correctly`() {
        val date = LocalDate.of(2024, 12, 15)
        assertEquals(
            LocalDate.of(2025, 1, 15),
            RecurrenceCalculator.calculateNextDueDate(date, RecurrenceType.MONTHLY)
        )
    }
}
