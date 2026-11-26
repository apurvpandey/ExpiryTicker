package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.FileSize
import com.apurvpandey.expiryticker.core.util.PriceHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceHistoryTest {
    @Test fun risesAndFalls() {
        assertEquals(10, PriceHistory.changePercent(100_000, 110_000))
        assertEquals(-25, PriceHistory.changePercent(80_000, 60_000))
        assertEquals(0, PriceHistory.changePercent(50_000, 50_000))
    }

    @Test fun roundsToWholePercent() = assertEquals(3, PriceHistory.changePercent(300, 309)) // 3.0%

    @Test fun unknownOrZeroBaselineGivesNothing() {
        assertNull(PriceHistory.changePercent(null, 100))
        assertNull(PriceHistory.changePercent(100, null))
        assertNull(PriceHistory.changePercent(0, 100))
    }

    @Test fun fileSizesAreReadable() {
        assertEquals("512 B", FileSize.format(512))
        assertEquals("2 KB", FileSize.format(2048))
        assertEquals("1.5 MB", FileSize.format(1_572_864))
    }
}
