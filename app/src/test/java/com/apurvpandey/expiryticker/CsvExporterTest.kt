package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.CsvExporter
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {
    private fun item(title: String, date: String, notes: String = "", paise: Long? = null, done: Boolean = false) =
        ExpiryItem(
            title = title, category = RenewalCategory.INSURANCE, dueDate = LocalDate.parse(date),
            notes = notes, amountPaise = paise, isCompleted = done, recurrence = RecurrenceType.YEARLY
        )

    @Test fun headerOnlyWhenEmpty() {
        assertEquals(1, CsvExporter.export(emptyList()).lines().filter { it.isNotBlank() }.size)
    }

    @Test fun rowsAreSortedByDueDateAndFormatted() {
        val csv = CsvExporter.export(
            listOf(item("B", "2027-01-02", paise = 150050), item("A", "2026-05-01", done = true))
        ).lines()
        assertEquals("A,Insurance,2026-05-01,Completed,7,Yearly,,", csv[1])
        assertEquals("B,Insurance,2027-01-02,Open,7,Yearly,1500.50,", csv[2])
    }

    @Test fun quotesAndCommasAreEscaped() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"))
    }

    @Test fun formulaPrefixesAreNeutralised() {
        assertEquals("'=SUM(A1)", CsvExporter.escape("=SUM(A1)"))
        assertEquals("'-5", CsvExporter.escape("-5"))
    }
}
