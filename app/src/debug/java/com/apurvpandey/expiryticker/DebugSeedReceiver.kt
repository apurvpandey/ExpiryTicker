package com.apurvpandey.expiryticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Debug builds only: seeds or clears sample data so UI flows can be tested quickly. */
class DebugSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ExpiryTickerApplication).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = container.expiryItemRepository
                if (intent.action == "com.apurvpandey.expiryticker.CLEAR") {
                    repo.observeAll().first().forEach { container.itemActions.delete(it) }
                } else {
                    val today = LocalDate.now()
                    fun item(
                        title: String, category: RenewalCategory, days: Long,
                        recurrence: RecurrenceType = RecurrenceType.NONE,
                        paise: Long? = null, remind: Int = 7, url: String = "", phone: String = "",
                        extra: List<Int> = emptyList()
                    ) = ExpiryItem(
                        title = title, category = category, dueDate = today.plusDays(days),
                        recurrence = recurrence, amountPaise = paise, reminderDaysBefore = remind,
                        renewalUrl = url, phone = phone, extraReminderDays = extra
                    )
                    // A realistic set, including price history, so screenshots and demos look lived-in.
                    val demo = listOf(
                        item("Driving licence", RenewalCategory.LICENCE, -9, paise = 120000),
                        item("Home insurance", RenewalCategory.INSURANCE, -2, RecurrenceType.YEARLY, 1850000),
                        item("Passport", RenewalCategory.TRAVEL, 0),
                        item("Netflix", RenewalCategory.SUBSCRIPTION, 3, RecurrenceType.MONTHLY, 64900, 1),
                        item(
                            "Car insurance", RenewalCategory.INSURANCE, 5, RecurrenceType.YEARLY, 1450000,
                            url = "insurer.example.com/renew", phone = "+911800123456", extra = listOf(30, 3)
                        ),
                        item("Domain example.com", RenewalCategory.DOMAIN, 21, RecurrenceType.YEARLY, 99900),
                        item("Gym membership", RenewalCategory.MEMBERSHIP, 40, RecurrenceType.QUARTERLY, 360000),
                        item("Health check-up", RenewalCategory.HEALTH, 62, RecurrenceType.YEARLY, 450000),
                        item("Fridge warranty", RenewalCategory.WARRANTY, 120),
                    )
                    val ids = demo.associate { it.title to repo.add(it) }
                    demo.forEach { container.itemActions.restore(it.copy(id = ids.getValue(it.title))) }

                    fun record(title: String, daysAgo: Long, paise: Long, recurrence: RecurrenceType) =
                        RenewalRecord(
                            itemId = ids.getValue(title),
                            renewedAt = java.time.Instant.now().minus(daysAgo, java.time.temporal.ChronoUnit.DAYS),
                            dueBefore = today.minusDays(daysAgo), dueAfter = today.minusDays(daysAgo - 30),
                            amountPaise = paise
                        )
                    listOf(
                        record("Car insurance", 730, 1200000, RecurrenceType.YEARLY),
                        record("Car insurance", 365, 1300000, RecurrenceType.YEARLY),
                        record("Netflix", 60, 59900, RecurrenceType.MONTHLY),
                        record("Netflix", 30, 64900, RecurrenceType.MONTHLY),
                        record("Home insurance", 365, 1700000, RecurrenceType.YEARLY),
                    ).forEach { container.renewalHistoryRepository.add(it) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
