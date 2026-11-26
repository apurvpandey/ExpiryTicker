package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager.bodyFor
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {
    @Test fun upcomingWording() {
        assertEquals("Expires in 5 days.", bodyFor(5, renews = false))
        assertEquals("Expires tomorrow.", bodyFor(1, renews = false))
        assertEquals("Expires today.", bodyFor(0, renews = false))
    }

    @Test fun recurringItemsSayRenews() {
        assertEquals("Renews in 3 days.", bodyFor(3, renews = true))
    }

    @Test fun overdueWording() {
        assertEquals("Overdue since yesterday.", bodyFor(-1, renews = false))
        assertEquals("Overdue by 4 days.", bodyFor(-4, renews = true))
    }
}
