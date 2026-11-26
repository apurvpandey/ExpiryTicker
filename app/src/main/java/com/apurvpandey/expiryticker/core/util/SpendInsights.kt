package com.apurvpandey.expiryticker.core.util

import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MonthSpend(val month: YearMonth, val paise: Long)
data class CategorySpend(val category: RenewalCategory, val annualPaise: Long)
data class PriceRise(val itemId: Long, val title: String, val fromPaise: Long, val toPaise: Long, val percent: Int)

data class Insights(
    /** What the open items cost over a year, assuming each keeps repeating at its current price. */
    val annualPaise: Long,
    val paidLast12MonthsPaise: Long,
    /** Next 12 months starting with the current one; overdue items count towards the current month. */
    val upcoming: List<MonthSpend>,
    val byCategory: List<CategorySpend>,
    val priceRises: List<PriceRise>,
    val openItemsWithoutCost: Int
) {
    val hasData: Boolean get() = annualPaise > 0 || paidLast12MonthsPaise > 0 || priceRises.isNotEmpty()
}

object SpendInsights {

    fun timesPerYear(recurrence: RecurrenceType): Int = when (recurrence) {
        RecurrenceType.NONE -> 1
        RecurrenceType.MONTHLY -> 12
        RecurrenceType.QUARTERLY -> 4
        RecurrenceType.HALF_YEARLY -> 2
        RecurrenceType.YEARLY -> 1
    }

    fun compute(
        items: List<ExpiryItem>,
        history: List<RenewalRecord>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Insights {
        val open = items.filterNot { it.isCompleted }
        val priced = open.filter { (it.amountPaise ?: 0L) > 0L }

        val windowStart = YearMonth.from(today)
        val months = (0..11).map { windowStart.plusMonths(it.toLong()) }
        val windowEnd = months.last().atEndOfMonth()
        val perMonth = months.associateWith { 0L }.toMutableMap()

        priced.forEach { item ->
            val amount = item.amountPaise ?: return@forEach
            dueDatesInWindow(item, today, windowEnd).forEach { date ->
                val month = maxOf(YearMonth.from(date), windowStart)
                perMonth[month] = (perMonth[month] ?: 0L) + amount
            }
        }

        // One-offs only cost money once, so they count towards the year only when due within it.
        val annual = priced.sumOf { item ->
            val amount = item.amountPaise ?: 0L
            if (item.recurrence == RecurrenceType.NONE) {
                if (!item.dueDate.isAfter(windowEnd)) amount else 0L
            } else amount * timesPerYear(item.recurrence)
        }

        val byCategory = priced
            .groupBy { it.category }
            .map { (category, list) ->
                CategorySpend(category, list.sumOf { item ->
                    val amount = item.amountPaise ?: 0L
                    if (item.recurrence == RecurrenceType.NONE) {
                        if (!item.dueDate.isAfter(windowEnd)) amount else 0L
                    } else amount * timesPerYear(item.recurrence)
                })
            }
            .filter { it.annualPaise > 0 }
            .sortedByDescending { it.annualPaise }

        val cutoff = today.minusYears(1)
        val paid = history
            .filter { !it.renewedAt.atZone(zone).toLocalDate().isBefore(cutoff) }
            .sumOf { it.amountPaise ?: 0L }

        return Insights(
            annualPaise = annual,
            paidLast12MonthsPaise = paid,
            upcoming = months.map { MonthSpend(it, perMonth[it] ?: 0L) },
            byCategory = byCategory,
            priceRises = priceRises(items, history),
            openItemsWithoutCost = open.count { (it.amountPaise ?: 0L) <= 0L }
        )
    }

    /** Every date this item falls due between now and [windowEnd]; an overdue first date counts once. */
    internal fun dueDatesInWindow(item: ExpiryItem, today: LocalDate, windowEnd: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var date = item.dueDate
        if (date.isBefore(today)) {
            dates += today // overdue: money is owed now, counted once
            if (item.recurrence == RecurrenceType.NONE) return dates
            while (date.isBefore(today)) date = RecurrenceCalculator.calculateNextDueDate(date, item.recurrence)
        }
        while (!date.isAfter(windowEnd)) {
            dates += date
            if (item.recurrence == RecurrenceType.NONE) break
            date = RecurrenceCalculator.calculateNextDueDate(date, item.recurrence)
        }
        return dates
    }

    internal fun priceRises(items: List<ExpiryItem>, history: List<RenewalRecord>): List<PriceRise> {
        val titles = items.associate { it.id to it.title }
        return history
            .filter { it.amountPaise != null && it.itemId in titles }
            .groupBy { it.itemId }
            .mapNotNull { (id, records) ->
                val sorted = records.sortedBy { it.renewedAt }
                val last = sorted.last().amountPaise ?: return@mapNotNull null
                val before = sorted.getOrNull(sorted.size - 2)?.amountPaise ?: return@mapNotNull null
                val percent = PriceHistory.changePercent(before, last) ?: return@mapNotNull null
                if (percent <= 0) null else PriceRise(id, titles.getValue(id), before, last, percent)
            }
            .sortedByDescending { it.percent }
            .take(5)
    }
}
