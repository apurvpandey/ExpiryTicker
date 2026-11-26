package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager.DayPhrase
import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager.DayPhrase.Kind
import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager.phraseFor
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {
    @Test fun upcomingPhrases() {
        assertEquals(DayPhrase(Kind.IN_DAYS, 5, false), phraseFor(5, renews = false))
        assertEquals(DayPhrase(Kind.TOMORROW, 1, false), phraseFor(1, renews = false))
        assertEquals(DayPhrase(Kind.TODAY, 0, false), phraseFor(0, renews = false))
    }

    @Test fun recurringItemsRenewInsteadOfExpire() {
        assertEquals(DayPhrase(Kind.IN_DAYS, 3, true), phraseFor(3, renews = true))
        assertEquals(true, phraseFor(0, renews = true).renews)
    }

    @Test fun overduePhrases() {
        assertEquals(DayPhrase(Kind.OVERDUE_YESTERDAY, 1, false), phraseFor(-1, renews = false))
        assertEquals(DayPhrase(Kind.OVERDUE_DAYS, 4, true), phraseFor(-4, renews = true))
    }
}
