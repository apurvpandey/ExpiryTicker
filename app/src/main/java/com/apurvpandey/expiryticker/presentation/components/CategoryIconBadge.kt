package com.apurvpandey.expiryticker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apurvpandey.expiryticker.domain.model.RenewalCategory

@Composable
fun CategoryIconBadge(
    category: RenewalCategory,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    iconSize: Dp = 22.dp,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hue = category.hue()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(if (dark) hue.darkContainer else hue.lightContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = if (dark) hue.darkAccent else hue.lightAccent,
            modifier = Modifier.size(iconSize)
        )
    }
}

private class Hue(
    val lightAccent: Color, val lightContainer: Color,
    val darkAccent: Color, val darkContainer: Color
)

private fun hue(light: Long, lightBg: Long, dark: Long, darkBg: Long) =
    Hue(Color(light), Color(lightBg), Color(dark), Color(darkBg))

/** Every category gets its own hue so a list is scannable at a glance. */
private fun RenewalCategory.hue(): Hue = when (this) {
    RenewalCategory.VEHICLE -> hue(0xFF2563EB, 0xFFDDE8FF, 0xFF8DB4FF, 0xFF1C2D55)
    RenewalCategory.INSURANCE -> hue(0xFF4F46E5, 0xFFE3E2FF, 0xFFA5A0FF, 0xFF26255A)
    RenewalCategory.TRAVEL -> hue(0xFF0284C7, 0xFFD8F0FF, 0xFF7CD0FF, 0xFF103650)
    RenewalCategory.LICENCE -> hue(0xFF7C3AED, 0xFFEBE0FF, 0xFFC3A3FF, 0xFF32205A)
    RenewalCategory.WARRANTY -> hue(0xFF16A34A, 0xFFD9F6E3, 0xFF7FE0A3, 0xFF143C25)
    RenewalCategory.SUBSCRIPTION -> hue(0xFFDB2777, 0xFFFFDFEC, 0xFFFF9FC9, 0xFF54172F)
    RenewalCategory.DOMAIN -> hue(0xFF0891B2, 0xFFD3F3FA, 0xFF72DCEF, 0xFF0F3945)
    RenewalCategory.CERTIFICATION -> hue(0xFFD97706, 0xFFFFEBCB, 0xFFFFC163, 0xFF44300F)
    RenewalCategory.MEMBERSHIP -> hue(0xFFE11D48, 0xFFFFDDE3, 0xFFFF9BAE, 0xFF4F1722)
    RenewalCategory.AMC -> hue(0xFF475569, 0xFFE2E8F0, 0xFFB4C0D4, 0xFF28303D)
    RenewalCategory.FINANCE -> hue(0xFF059669, 0xFFD1F5E8, 0xFF6EE0B8, 0xFF0F3B2E)
    RenewalCategory.HEALTH -> hue(0xFFDC2626, 0xFFFFE0E0, 0xFFFF9A9A, 0xFF4A1A1A)
    RenewalCategory.HOME -> hue(0xFFEA580C, 0xFFFFE6D6, 0xFFFFAA7C, 0xFF45240F)
    RenewalCategory.OTHER -> hue(0xFF64748B, 0xFFE6EAF0, 0xFFAAB4C6, 0xFF2A313D)
}
