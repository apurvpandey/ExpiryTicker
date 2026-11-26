package com.apurvpandey.expiryticker.core.util

import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import java.time.LocalDate

data class WidgetRow(
    val id: Long,
    val title: String,
    val category: RenewalCategory,
    val status: ExpiryStatus
)

/** What the home-screen widget shows: the most urgent open items plus headline counts. */
data class WidgetSnapshot(
    val rows: List<WidgetRow>,
    val overdueCount: Int,
    val openCount: Int
)

object WidgetSnapshots {
    fun build(items: List<ExpiryItem>, today: LocalDate, limit: Int = 5): WidgetSnapshot {
        val open = items
            .filterNot { it.isCompleted }
            .sortedWith(compareBy({ it.dueDate }, { it.title.lowercase() }))
        return WidgetSnapshot(
            rows = open.take(limit).map {
                WidgetRow(it.id, it.title, it.category, ExpiryStatusCalculator.calculate(it.dueDate, it.isCompleted, today))
            },
            overdueCount = open.count { it.dueDate.isBefore(today) },
            openCount = open.size
        )
    }
}
