package com.apurvpandey.expiryticker.core.util

import com.apurvpandey.expiryticker.domain.model.ExpiryItem

/** Builds a spreadsheet-friendly CSV of all items (RFC 4180 quoting). */
object CsvExporter {
    private val header = listOf(
        "Title", "Category", "Due date", "Status", "Reminder days before",
        "Repeats", "Amount", "Notes"
    )

    fun export(items: List<ExpiryItem>): String = buildString {
        appendLine(header.joinToString(","))
        items.sortedBy { it.dueDate }.forEach { item ->
            val amount = item.amountPaise?.let { "%d.%02d".format(it / 100, it % 100) } ?: ""
            val row = listOf(
                item.title,
                item.category.displayName,
                item.dueDate.toString(),
                if (item.isCompleted) "Completed" else "Open",
                item.reminderDaysBefore.toString(),
                item.recurrence.displayName,
                amount,
                item.notes
            )
            appendLine(row.joinToString(",") { escape(it) })
        }
    }

    internal fun escape(value: String): String {
        // Leading = + - @ can be interpreted as a formula by spreadsheets; neutralise it.
        val safe = if (value.firstOrNull() in setOf('=', '+', '-', '@')) "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else safe
    }
}
