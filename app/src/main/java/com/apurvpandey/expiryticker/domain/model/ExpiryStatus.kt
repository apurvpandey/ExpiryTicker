package com.apurvpandey.expiryticker.domain.model

sealed class ExpiryStatus {
    data object Completed : ExpiryStatus()
    data class Overdue(val daysOverdue: Int) : ExpiryStatus()
    data object DueToday : ExpiryStatus()
    data class Active(val daysRemaining: Int) : ExpiryStatus()
}

fun ExpiryStatus.toDisplayText(): String = when (this) {
    is ExpiryStatus.Completed -> "Completed"
    is ExpiryStatus.DueToday -> "Expires today"
    is ExpiryStatus.Overdue -> when (daysOverdue) {
        1 -> "Expired yesterday"
        else -> "Expired $daysOverdue days ago"
    }
    is ExpiryStatus.Active -> when (daysRemaining) {
        1 -> "Expires tomorrow"
        else -> "$daysRemaining days remaining"
    }
}
