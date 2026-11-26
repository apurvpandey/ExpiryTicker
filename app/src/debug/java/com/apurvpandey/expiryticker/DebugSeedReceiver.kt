package com.apurvpandey.expiryticker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
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
                        paise: Long? = null, remind: Int = 7
                    ) = ExpiryItem(
                        title = title, category = category, dueDate = today.plusDays(days),
                        recurrence = recurrence, amountPaise = paise, reminderDaysBefore = remind
                    )
                    listOf(
                        item("Driving licence", RenewalCategory.LICENCE, -9, paise = 120000),
                        item("Home insurance", RenewalCategory.INSURANCE, -2, paise = 1850000),
                        item("Passport", RenewalCategory.TRAVEL, 0),
                        item("Netflix", RenewalCategory.SUBSCRIPTION, 3, RecurrenceType.MONTHLY, 64900, 1),
                        item("Car insurance", RenewalCategory.INSURANCE, 5, RecurrenceType.YEARLY, 1450000),
                        item("Domain example.com", RenewalCategory.DOMAIN, 21, RecurrenceType.YEARLY, 99900),
                        item("Fridge warranty", RenewalCategory.WARRANTY, 120),
                    ).forEach { container.itemActions.restore(it.copy(id = repo.add(it))) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
