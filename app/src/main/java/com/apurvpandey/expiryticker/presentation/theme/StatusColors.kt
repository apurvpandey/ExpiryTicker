package com.apurvpandey.expiryticker.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus

/** Accent for strong marks (bars, numbers), plus a soft container with readable content. */
data class UrgencyColors(val accent: Color, val container: Color, val onContainer: Color)

/** One semantic colour set per urgency level, used consistently across the app. */
data class ExpiryColors(
    val overdue: UrgencyColors,
    val today: UrgencyColors,
    val soon: UrgencyColors,
    val ok: UrgencyColors,
    val done: UrgencyColors
)

val LightExpiryColors = ExpiryColors(
    overdue = UrgencyColors(Color(0xFFD92D3A), Color(0xFFFFE4E3), Color(0xFF7A1119)),
    today = UrgencyColors(Color(0xFFE0580C), Color(0xFFFFE8D8), Color(0xFF7A2E04)),
    soon = UrgencyColors(Color(0xFFC77700), Color(0xFFFFF0D2), Color(0xFF6B4100)),
    ok = UrgencyColors(Color(0xFF0F8F83), Color(0xFFD3F4EF), Color(0xFF00403A)),
    done = UrgencyColors(Color(0xFF7A7F90), Color(0xFFE6E8F0), Color(0xFF4A4E5E))
)

val DarkExpiryColors = ExpiryColors(
    overdue = UrgencyColors(Color(0xFFFF8A91), Color(0xFF4A1A20), Color(0xFFFFD6D8)),
    today = UrgencyColors(Color(0xFFFFA06A), Color(0xFF47250F), Color(0xFFFFDCC8)),
    soon = UrgencyColors(Color(0xFFF5C04A), Color(0xFF3F3010), Color(0xFFFFE8B0)),
    ok = UrgencyColors(Color(0xFF5FD9CB), Color(0xFF123A36), Color(0xFFB5F2EA)),
    done = UrgencyColors(Color(0xFF9AA0B2), Color(0xFF262B38), Color(0xFFC6CAD8))
)

val LocalExpiryColors = staticCompositionLocalOf { LightExpiryColors }

object ExpiryTheme {
    val colors: ExpiryColors
        @Composable get() = LocalExpiryColors.current
}

/** Items due within this many days read as "soon" rather than "ok". */
const val SOON_THRESHOLD_DAYS = 7

@Composable
fun ExpiryStatus.urgencyColors(): UrgencyColors {
    val c = ExpiryTheme.colors
    return when (this) {
        is ExpiryStatus.Overdue -> c.overdue
        is ExpiryStatus.DueToday -> c.today
        is ExpiryStatus.Active -> if (daysRemaining <= SOON_THRESHOLD_DAYS) c.soon else c.ok
        is ExpiryStatus.Completed -> c.done
    }
}
