package com.apurvpandey.expiryticker.domain.model

enum class RecurrenceType(val displayName: String) {
    NONE("Does not repeat"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    HALF_YEARLY("Every 6 months"),
    YEARLY("Yearly")
}
