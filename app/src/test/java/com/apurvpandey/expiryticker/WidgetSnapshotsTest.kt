package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.WidgetSnapshots
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetSnapshotsTest {
    private val today = LocalDate.of(2026, 11, 26)

    private fun item(id: Long, title: String, offset: Long, done: Boolean = false) = ExpiryItem(
        id = id, title = title, category = RenewalCategory.OTHER,
        dueDate = today.plusDays(offset), isCompleted = done
    )

    @Test fun emptyListGivesEmptySnapshot() {
        val s = WidgetSnapshots.build(emptyList(), today)
        assertTrue(s.rows.isEmpty())
        assertEquals(0, s.openCount)
    }

    @Test fun mostUrgentFirstAndCompletedExcluded() {
        val s = WidgetSnapshots.build(
            listOf(item(1, "Later", 20), item(2, "Done", -5, done = true), item(3, "Soon", 2), item(4, "Late", -3)),
            today
        )
        assertEquals(listOf("Late", "Soon", "Later"), s.rows.map { it.title })
        assertEquals(1, s.overdueCount)
        assertEquals(3, s.openCount)
    }

    @Test fun limitsRowsButCountsEverything() {
        val items = (1L..8L).map { item(it, "Item $it", it) }
        val s = WidgetSnapshots.build(items, today, limit = 3)
        assertEquals(3, s.rows.size)
        assertEquals(8, s.openCount)
    }

    @Test fun rowsCarryStatus() {
        val s = WidgetSnapshots.build(listOf(item(1, "A", 0), item(2, "B", -2)), today)
        assertEquals(ExpiryStatus.Overdue(2), s.rows[0].status)
        assertEquals(ExpiryStatus.DueToday, s.rows[1].status)
    }

    @Test fun tiesBreakAlphabetically() {
        val s = WidgetSnapshots.build(listOf(item(1, "banana", 1), item(2, "Apple", 1)), today)
        assertEquals(listOf("Apple", "banana"), s.rows.map { it.title })
    }
}
