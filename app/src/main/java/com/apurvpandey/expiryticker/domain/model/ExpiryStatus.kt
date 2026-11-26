package com.apurvpandey.expiryticker.domain.model

sealed class ExpiryStatus {
    data object Completed : ExpiryStatus()
    data class Overdue(val daysOverdue: Int) : ExpiryStatus()
    data object DueToday : ExpiryStatus()
    data class Active(val daysRemaining: Int) : ExpiryStatus()
}
