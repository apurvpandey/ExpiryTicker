package com.apurvpandey.expiryticker.presentation.components

import com.apurvpandey.expiryticker.domain.model.ExpiryStatus

/** Big number plus a short caption, e.g. "5" / "days left". */
data class Countdown(val value: String, val caption: String)

fun ExpiryStatus.countdown(): Countdown = when (this) {
    is ExpiryStatus.Completed -> Countdown("Done", "completed")
    is ExpiryStatus.DueToday -> Countdown("Today", "due")
    is ExpiryStatus.Overdue ->
        Countdown(daysOverdue.toString(), if (daysOverdue == 1) "day overdue" else "days overdue")
    is ExpiryStatus.Active ->
        Countdown(daysRemaining.toString(), if (daysRemaining == 1) "day left" else "days left")
}
