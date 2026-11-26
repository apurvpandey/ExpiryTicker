package com.apurvpandey.expiryticker.core.util

import kotlin.math.roundToInt

object PriceHistory {
    /** Whole-percent change from [previous] to [current]; null when either is unknown or previous is 0. */
    fun changePercent(previous: Long?, current: Long?): Int? {
        if (previous == null || current == null || previous == 0L) return null
        return ((current - previous) * 100.0 / previous).roundToInt()
    }
}
