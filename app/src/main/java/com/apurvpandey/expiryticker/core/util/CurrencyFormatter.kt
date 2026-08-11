package com.apurvpandey.expiryticker.core.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    // INR locale — swap currency here to support other currencies in future
    private val locale = Locale.forLanguageTag("en-IN")
    private val currency = Currency.getInstance("INR")

    fun format(amountPaise: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        return formatter.format(amountPaise / 100.0)
    }

    // Parses a user-entered decimal string (e.g. "1500" or "1500.50") to paise.
    // Returns null if the string is blank or cannot be parsed as a non-negative number.
    fun parseToPaise(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val amount = trimmed.toDoubleOrNull() ?: return null
        if (amount < 0) return null
        return (amount * 100).toLong()
    }

    fun formatFromPaise(amountPaise: Long?): String =
        if (amountPaise != null) format(amountPaise) else ""
}
