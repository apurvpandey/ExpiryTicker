package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.DueBucket
import com.apurvpandey.expiryticker.core.util.ItemGrouping
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ItemGroupingTest {
    private val today = LocalDate.of(2026, 11, 26)

    private fun bucket(offsetDays: Long) = ItemGrouping.bucketFor(today.plusDays(offsetDays), today)

    @Test fun pastDatesAreOverdue() {
        assertEquals(DueBucket.OVERDUE, bucket(-1))
        assertEquals(DueBucket.OVERDUE, bucket(-400))
    }

    @Test fun todayIsItsOwnBucket() = assertEquals(DueBucket.TODAY, bucket(0))

    @Test fun weekBoundaryIsInclusive() {
        assertEquals(DueBucket.THIS_WEEK, bucket(1))
        assertEquals(DueBucket.THIS_WEEK, bucket(7))
        assertEquals(DueBucket.THIS_MONTH, bucket(8))
    }

    @Test fun monthBoundaryIsInclusive() {
        assertEquals(DueBucket.THIS_MONTH, bucket(30))
        assertEquals(DueBucket.LATER, bucket(31))
    }

    @Test fun bucketsSortFromMostUrgent() {
        val sorted = DueBucket.entries.shuffled().sorted()
        assertEquals(
            listOf(DueBucket.OVERDUE, DueBucket.TODAY, DueBucket.THIS_WEEK, DueBucket.THIS_MONTH, DueBucket.LATER),
            sorted
        )
    }
}
