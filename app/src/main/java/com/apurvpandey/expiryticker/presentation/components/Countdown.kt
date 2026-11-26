package com.apurvpandey.expiryticker.presentation.components

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.apurvpandey.expiryticker.R
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus

/** Big number plus a short caption, e.g. "5" / "days left". */
data class Countdown(val value: String, val caption: String)

fun ExpiryStatus.countdown(res: Resources): Countdown = when (this) {
    is ExpiryStatus.Completed ->
        Countdown(res.getString(R.string.countdown_done_value), res.getString(R.string.countdown_done_caption))
    is ExpiryStatus.DueToday ->
        Countdown(res.getString(R.string.countdown_today_value), res.getString(R.string.countdown_today_caption))
    is ExpiryStatus.Overdue ->
        Countdown(daysOverdue.toString(), res.getQuantityString(R.plurals.caption_days_overdue, daysOverdue))
    is ExpiryStatus.Active ->
        Countdown(daysRemaining.toString(), res.getQuantityString(R.plurals.caption_days_left, daysRemaining))
}

@Composable
fun ExpiryStatus.countdown(): Countdown = countdown(LocalContext.current.resources)

/** Compact one-line form for tight spaces such as the widget: "5d left", "Today", "9d overdue". */
fun ExpiryStatus.shortLabel(res: Resources): String = when (this) {
    is ExpiryStatus.Completed -> res.getString(R.string.short_done)
    is ExpiryStatus.DueToday -> res.getString(R.string.short_today)
    is ExpiryStatus.Overdue -> res.getString(R.string.short_overdue, daysOverdue)
    is ExpiryStatus.Active -> res.getString(R.string.short_left, daysRemaining)
}

/** The large heading on cards and headers: "9 days overdue", "Due today", "5 days left". */
fun ExpiryStatus.headline(res: Resources): String = when (this) {
    is ExpiryStatus.Completed -> res.getString(R.string.completed)
    is ExpiryStatus.DueToday -> res.getString(R.string.hero_due_today)
    is ExpiryStatus.Overdue -> res.getQuantityString(R.plurals.hero_days_overdue, daysOverdue, daysOverdue)
    is ExpiryStatus.Active -> res.getQuantityString(R.plurals.hero_days_left, daysRemaining, daysRemaining)
}
