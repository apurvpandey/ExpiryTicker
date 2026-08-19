package com.apurvpandey.expiryticker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apurvpandey.expiryticker.domain.model.RenewalCategory

@Composable
fun CategoryIconBadge(
    category: RenewalCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    val (bg, tint) = category.badgeColors()
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun RenewalCategory.badgeColors(): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (this) {
        RenewalCategory.VEHICLE, RenewalCategory.TRAVEL, RenewalCategory.AMC ->
            cs.secondaryContainer to cs.onSecondaryContainer
        RenewalCategory.SUBSCRIPTION, RenewalCategory.DOMAIN,
        RenewalCategory.MEMBERSHIP, RenewalCategory.CERTIFICATION ->
            cs.tertiaryContainer to cs.onTertiaryContainer
        else ->
            cs.primaryContainer to cs.onPrimaryContainer
    }
}
