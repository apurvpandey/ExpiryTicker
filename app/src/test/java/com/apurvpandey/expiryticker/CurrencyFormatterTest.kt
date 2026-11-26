package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {
    @Test fun wholeRupeesHaveNoDecimals() {
        val text = CurrencyFormatter.format(150000L)
        assertTrue(text, text.contains("1,500"))
        assertFalse(text, text.contains("."))
    }

    @Test fun paiseAreKeptWhenPresent() {
        assertTrue(CurrencyFormatter.format(150050L).contains("1,500.50"))
    }

    @Test fun editableTextHasNoSymbolOrDecimalNoise() {
        assertTrue(CurrencyFormatter.toEditableText(120000L) == "1200")
        assertTrue(CurrencyFormatter.toEditableText(150050L) == "1500.50")
        assertTrue(CurrencyFormatter.toEditableText(105L) == "1.05")
    }

    // --- selectable currency ---------------------------------------------------------------

    private val savedLocale = java.util.Locale.getDefault()

    @org.junit.After fun restoreDefaults() {
        CurrencyFormatter.setCurrency("INR")
        java.util.Locale.setDefault(savedLocale)
    }

    @Test fun usdFormatsWithDollarAndWesternGrouping() {
        java.util.Locale.setDefault(java.util.Locale.US)
        CurrencyFormatter.setCurrency("USD")
        assertTrue(CurrencyFormatter.format(145_000_000L).contains("1,450,000"))
        assertTrue(CurrencyFormatter.format(150_050L).contains("$1,500.50"))
        assertEquals("$", CurrencyFormatter.symbol())
    }

    // Lakh grouping (14,50,000) comes from Android's ICU and is not available on the plain JVM
    // used by unit tests, so only the symbol and digits are checked here.
    @Test fun rupeesUseTheRupeeSymbolWhateverTheDeviceLocale() {
        java.util.Locale.setDefault(java.util.Locale.US)
        CurrencyFormatter.setCurrency("INR")
        assertEquals("₹", CurrencyFormatter.symbol())
        assertEquals("1450000", CurrencyFormatter.format(145_000_000L).filter { it.isDigit() })
    }

    @Test fun currencyWithoutMinorUnitsHasNoDecimals() {
        java.util.Locale.setDefault(java.util.Locale.US)
        CurrencyFormatter.setCurrency("JPY")
        val text = CurrencyFormatter.format(50_000L) // 500 in the main unit
        assertTrue(text, text.contains("500"))
        assertFalse(text, text.contains("."))
    }

    @Test fun unknownCodeFallsBackToRupees() {
        CurrencyFormatter.setCurrency("NOT_A_CODE")
        assertEquals("INR", CurrencyFormatter.currencyCode)
    }

    @Test fun everyOfferedCurrencyIsValid() {
        CurrencyFormatter.supportedCodes.forEach { code ->
            CurrencyFormatter.setCurrency(code)
            assertEquals(code, CurrencyFormatter.currencyCode)
            assertTrue(CurrencyFormatter.format(12_345L).isNotBlank())
        }
    }

    @Test fun parsingRoundsInsteadOfTruncating() {
        // 19.99 * 100 is 1998.9999999999998 in floating point.
        assertEquals(1999L, CurrencyFormatter.parseToPaise("19.99"))
        assertEquals(150_050L, CurrencyFormatter.parseToPaise("1500.50"))
        assertEquals(null, CurrencyFormatter.parseToPaise("-1"))
    }
}
