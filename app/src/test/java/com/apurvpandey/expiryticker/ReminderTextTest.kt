package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.ReminderText
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReminderTextTest {
    private fun item(main: Int, extra: List<Int>) = ExpiryItem(
        title = "x", category = RenewalCategory.OTHER, dueDate = LocalDate.of(2026, 1, 1),
        reminderDaysBefore = main, extraReminderDays = extra
    )

    @Test fun offsetsAreSortedAndDeduplicated() =
        assertEquals(listOf(30, 7, 1, 0), ReminderText.allOffsets(item(7, listOf(1, 30, 7, 0))))

    @Test fun negativeOffsetsAreIgnored() = assertEquals(listOf(3), ReminderText.allOffsets(item(3, listOf(-2))))

}
