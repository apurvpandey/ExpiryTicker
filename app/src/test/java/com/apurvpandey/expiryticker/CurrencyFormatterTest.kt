package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
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
}
