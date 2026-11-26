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
}
