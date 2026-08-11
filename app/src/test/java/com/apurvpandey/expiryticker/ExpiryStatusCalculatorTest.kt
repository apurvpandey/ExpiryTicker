package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.ExpiryStatusCalculator
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpiryStatusCalculatorTest {

    private val today = LocalDate.of(2024, 6, 15)

    @Test
    fun `completed item returns Completed status`() {
        val result = ExpiryStatusCalculator.calculate(today, isCompleted = true, today)
        assertEquals(ExpiryStatus.Completed, result)
    }

    @Test
    fun `due today returns DueToday status`() {
        val result = ExpiryStatusCalculator.calculate(today, isCompleted = false, today)
        assertEquals(ExpiryStatus.DueToday, result)
    }

    @Test
    fun `due tomorrow returns Active with 1 day remaining`() {
        val dueDate = today.plusDays(1)
        val result = ExpiryStatusCalculator.calculate(dueDate, isCompleted = false, today)
        assertEquals(ExpiryStatus.Active(daysRemaining = 1), result)
    }

    @Test
    fun `due in 7 days returns Active with 7 days remaining`() {
        val dueDate = today.plusDays(7)
        val result = ExpiryStatusCalculator.calculate(dueDate, isCompleted = false, today)
        assertEquals(ExpiryStatus.Active(daysRemaining = 7), result)
    }

    @Test
    fun `expired yesterday returns Overdue with 1 day overdue`() {
        val dueDate = today.minusDays(1)
        val result = ExpiryStatusCalculator.calculate(dueDate, isCompleted = false, today)
        assertEquals(ExpiryStatus.Overdue(daysOverdue = 1), result)
    }

    @Test
    fun `expired 30 days ago returns Overdue with 30 days overdue`() {
        val dueDate = today.minusDays(30)
        val result = ExpiryStatusCalculator.calculate(dueDate, isCompleted = false, today)
        assertEquals(ExpiryStatus.Overdue(daysOverdue = 30), result)
    }

    @Test
    fun `completed item takes precedence over overdue status`() {
        val pastDate = today.minusDays(10)
        val result = ExpiryStatusCalculator.calculate(pastDate, isCompleted = true, today)
        assertEquals(ExpiryStatus.Completed, result)
    }
}
