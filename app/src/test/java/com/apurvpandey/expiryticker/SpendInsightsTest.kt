package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.SpendInsights
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class SpendInsightsTest {
    private val today = LocalDate.of(2026, 11, 26)

    private fun item(
        id: Long, due: LocalDate, paise: Long?, rec: RecurrenceType = RecurrenceType.NONE,
        cat: RenewalCategory = RenewalCategory.OTHER, done: Boolean = false, title: String = "Item $id"
    ) = ExpiryItem(id = id, title = title, category = cat, dueDate = due, amountPaise = paise, recurrence = rec, isCompleted = done)

    private fun record(itemId: Long, on: LocalDate, paise: Long?) = RenewalRecord(
        itemId = itemId, renewedAt = on.atStartOfDay().toInstant(ZoneOffset.UTC),
        dueBefore = on, dueAfter = null, amountPaise = paise
    )

    private fun compute(items: List<ExpiryItem>, history: List<RenewalRecord> = emptyList()) =
        SpendInsights.compute(items, history, today, ZoneOffset.UTC)

    @Test fun recurringItemsAreAnnualisedByFrequency() {
        val r = compute(listOf(
            item(1, today.plusDays(5), 10_000, RecurrenceType.MONTHLY),
            item(2, today.plusDays(5), 20_000, RecurrenceType.QUARTERLY),
            item(3, today.plusDays(5), 30_000, RecurrenceType.YEARLY)
        ))
        assertEquals(10_000 * 12 + 20_000 * 4 + 30_000L, r.annualPaise)
    }

    @Test fun oneOffCountsOnlyWhenDueWithinTheYear() {
        val r = compute(listOf(item(1, today.plusDays(100), 5_000), item(2, today.plusYears(2), 9_000)))
        assertEquals(5_000L, r.annualPaise)
    }

    @Test fun completedAndUnpricedItemsAreIgnoredForCost() {
        val r = compute(listOf(item(1, today, 5_000, done = true), item(2, today, null), item(3, today, 0)))
        assertEquals(0L, r.annualPaise)
        assertEquals(2, r.openItemsWithoutCost)
    }

    @Test fun upcomingCoversTwelveMonthsStartingNow() {
        val r = compute(emptyList())
        assertEquals(12, r.upcoming.size)
        assertEquals(YearMonth.of(2026, 11), r.upcoming.first().month)
        assertEquals(YearMonth.of(2027, 10), r.upcoming.last().month)
    }

    @Test fun monthlyItemAppearsInEveryMonth() {
        val r = compute(listOf(item(1, today.plusDays(2), 1_000, RecurrenceType.MONTHLY)))
        assertTrue(r.upcoming.all { it.paise == 1_000L })
    }

    @Test fun overdueCountsOnceInTheCurrentMonth() {
        val r = compute(listOf(item(1, today.minusDays(40), 7_000)))
        assertEquals(7_000L, r.upcoming.first().paise)
        assertEquals(7_000L, r.upcoming.sumOf { it.paise })
    }

    @Test fun overdueRecurringItemContinuesFromItsNextFutureDate() {
        // Yearly, last due 400 days ago: owed now once, then again next cycle only if inside the window.
        val r = compute(listOf(item(1, today.minusDays(400), 8_000, RecurrenceType.YEARLY)))
        assertEquals(8_000L, r.upcoming.first().paise)
        assertTrue(r.upcoming.sumOf { it.paise } in 8_000L..16_000L)
    }

    @Test fun categoriesAreSortedByCostDescending() {
        val r = compute(listOf(
            item(1, today.plusDays(3), 1_000, RecurrenceType.YEARLY, RenewalCategory.HOME),
            item(2, today.plusDays(3), 1_000, RecurrenceType.MONTHLY, RenewalCategory.INSURANCE)
        ))
        assertEquals(listOf(RenewalCategory.INSURANCE, RenewalCategory.HOME), r.byCategory.map { it.category })
    }

    @Test fun paidCountsOnlyTheLastTwelveMonths() {
        val r = compute(emptyList(), listOf(
            record(1, today.minusDays(30), 4_000),
            record(1, today.minusYears(2), 99_000),
            record(1, today.minusDays(10), null)
        ))
        assertEquals(4_000L, r.paidLast12MonthsPaise)
    }

    @Test fun priceRisesComparesTheLastTwoPricedRenewals() {
        val items = listOf(item(1, today, 1, title = "Car"), item(2, today, 1, title = "Phone"), item(3, today, 1, title = "Gym"))
        val history = listOf(
            record(1, today.minusYears(2), 100), record(1, today.minusYears(1), 100), record(1, today.minusDays(5), 125),
            record(2, today.minusYears(1), 200), record(2, today.minusDays(5), 150),   // fell: not a rise
            record(3, today.minusDays(5), 300)                                          // only one entry: nothing to compare
        )
        val rises = SpendInsights.compute(items, history, today, ZoneOffset.UTC).priceRises
        assertEquals(1, rises.size)
        assertEquals("Car", rises[0].title)
        assertEquals(25, rises[0].percent)
    }

    @Test fun deletedItemsDoNotShowInPriceRises() {
        val rises = SpendInsights.priceRises(
            emptyList(), listOf(record(9, today.minusYears(1), 100), record(9, today, 200))
        )
        assertTrue(rises.isEmpty())
    }

    @Test fun emptyInputHasNoData() = assertEquals(false, compute(emptyList()).hasData)

    @Test fun instantsUseTheGivenZone() {
        // 23:30 UTC on the cutoff day is already the next day in IST, but still inside the window.
        val onCutoff = Instant.parse("2025-11-26T23:30:00Z")
        val rec = RenewalRecord(itemId = 1, renewedAt = onCutoff, dueBefore = today, dueAfter = null, amountPaise = 500)
        assertEquals(500L, SpendInsights.compute(emptyList(), listOf(rec), today, ZoneOffset.UTC).paidLast12MonthsPaise)
    }
}
