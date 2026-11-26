package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.LinkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkUtilsTest {
    @Test fun addsHttpsWhenMissing() = assertEquals("https://example.com/renew", LinkUtils.normalizeUrl("example.com/renew"))

    @Test fun keepsExistingScheme() {
        assertEquals("http://example.com", LinkUtils.normalizeUrl(" http://example.com "))
        assertEquals("HTTPS://Example.com", LinkUtils.normalizeUrl("HTTPS://Example.com"))
    }

    @Test fun rejectsNonWebSchemesAndJunk() {
        assertNull(LinkUtils.normalizeUrl("javascript:alert(1)"))
        assertNull(LinkUtils.normalizeUrl("file:///etc/passwd"))
        assertNull(LinkUtils.normalizeUrl("tel:123456"))
        assertNull(LinkUtils.normalizeUrl("not a url"))
        assertNull(LinkUtils.normalizeUrl("localhost"))
        assertNull(LinkUtils.normalizeUrl(""))
    }

    @Test fun phoneKeepsDigitsAndLeadingPlus() {
        assertEquals("+919876543210", LinkUtils.normalizePhone("+91 98765-43210"))
        assertEquals("1800123456", LinkUtils.normalizePhone("1800 123 456"))
    }

    @Test fun phoneRejectsTooShortOrTooLong() {
        assertNull(LinkUtils.normalizePhone("12"))
        assertNull(LinkUtils.normalizePhone("1234567890123456"))
        assertNull(LinkUtils.normalizePhone("call me"))
    }

    @Test fun blankIsValidForOptionalFields() {
        assertTrue(LinkUtils.isValidUrl(""))
        assertTrue(LinkUtils.isValidPhone("  "))
        assertFalse(LinkUtils.isValidUrl("nope"))
    }
}
