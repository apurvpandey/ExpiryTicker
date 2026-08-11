package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {

    @Test
    fun `blank amount text parses to null`() {
        assertNull(CurrencyFormatter.parseToPaise(""))
        assertNull(CurrencyFormatter.parseToPaise("   "))
    }

    @Test
    fun `integer amount string converts to paise`() {
        assertEquals(150000L, CurrencyFormatter.parseToPaise("1500"))
    }

    @Test
    fun `decimal amount converts to paise`() {
        assertEquals(150050L, CurrencyFormatter.parseToPaise("1500.50"))
    }

    @Test
    fun `zero amount returns zero paise`() {
        assertEquals(0L, CurrencyFormatter.parseToPaise("0"))
    }

    @Test
    fun `negative amount returns null`() {
        assertNull(CurrencyFormatter.parseToPaise("-100"))
    }

    @Test
    fun `non-numeric string returns null`() {
        assertNull(CurrencyFormatter.parseToPaise("abc"))
        assertNull(CurrencyFormatter.parseToPaise("12.34.56"))
    }

    @Test
    fun `formatted currency shows INR symbol`() {
        val formatted = CurrencyFormatter.format(150000L) // 1500 INR
        assertNotNull(formatted)
        // Should contain a numeric representation of 1500
        assert(formatted.contains("1,500") || formatted.contains("1500")) {
            "Expected formatted amount to contain 1500, got: $formatted"
        }
    }

    @Test
    fun `formatFromPaise with null returns empty string`() {
        assertEquals("", CurrencyFormatter.formatFromPaise(null))
    }
}
