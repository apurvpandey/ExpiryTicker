package com.apurvpandey.expiryticker.core.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats and parses money. Amounts are stored as "paise": hundredths of the currency's main
 * unit, for every currency, so nothing stored changes when the user picks another currency
 * (existing amounts are not converted, only labelled differently).
 */
object CurrencyFormatter {
    const val DEFAULT_CODE = "INR"

    /** Currencies offered in Settings. */
    val supportedCodes = listOf("INR", "USD", "EUR", "GBP", "AED", "AUD", "CAD", "SGD", "JPY", "CHF", "SAR", "ZAR")

    // Snapshot state: composables that format money recompose by themselves when this changes.
    private var current by mutableStateOf(Currency.getInstance(DEFAULT_CODE))

    val currencyCode: String get() = current.currencyCode

    fun setCurrency(code: String) {
        current = runCatching { Currency.getInstance(code) }.getOrElse { Currency.getInstance(DEFAULT_CODE) }
    }

    /** The symbol as written in this currency's own formatting, e.g. "₹" or "$". */
    fun symbol(): String = current.getSymbol(localeFor(current))

    fun format(amountPaise: Long): String {
        val currency = current
        val formatter = numberFormat(currency)
        // Whole amounts read cleaner without decimals; keep them when they matter.
        if (amountPaise % 100 == 0L) formatter.maximumFractionDigits = 0
        return formatter.format(amountPaise / 100.0)
    }

    // Indian digit grouping (12,34,567) for rupees; everything else follows the device locale.
    private fun localeFor(currency: Currency): Locale =
        if (currency.currencyCode == "INR") Locale.forLanguageTag("en-IN") else Locale.getDefault()

    private fun numberFormat(currency: Currency): NumberFormat =
        NumberFormat.getCurrencyInstance(localeFor(currency)).also { it.currency = currency }

    // Parses a user-entered decimal string (e.g. "1500" or "1500.50") to paise.
    // Returns null if the string is blank or cannot be parsed as a non-negative number.
    fun parseToPaise(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val amount = trimmed.toDoubleOrNull() ?: return null
        if (amount < 0) return null
        return Math.round(amount * 100)
    }

    /** Plain editable text for an amount field: "1200" or "1200.50", no symbol or grouping. */
    fun toEditableText(amountPaise: Long): String =
        if (amountPaise % 100 == 0L) (amountPaise / 100).toString()
        else "%d.%02d".format(amountPaise / 100, amountPaise % 100)

    fun formatFromPaise(amountPaise: Long?): String =
        if (amountPaise != null) format(amountPaise) else ""
}
