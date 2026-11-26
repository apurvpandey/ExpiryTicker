package com.apurvpandey.expiryticker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTheme
import com.apurvpandey.expiryticker.presentation.theme.urgencyColors

/**
 * Top-of-dashboard summary of the single most urgent open item, with a one-tap "Mark done".
 * The gradient follows the item's urgency, so the colour matches the cards below.
 */
@Composable
fun NextUpHero(
    item: ExpiryItem,
    status: ExpiryStatus,
    onOpen: () -> Unit,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = status.urgencyColors().accent
    val countdown = status.countdown()
    val headline = when (status) {
        is ExpiryStatus.Overdue -> "${countdown.value} ${countdown.caption}"
        is ExpiryStatus.DueToday -> "Due today"
        is ExpiryStatus.Active -> "${countdown.value} ${countdown.caption}"
        is ExpiryStatus.Completed -> "Done"
    }
    HeroSurface(
        brush = urgencyBrush(accent),
        modifier = modifier.clickable(role = Role.Button, onClickLabel = "Open ${item.title}", onClick = onOpen)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "NEXT UP",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
                letterSpacing = 1.2.sp
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryIconBadge(category = item.category, size = 40.dp, iconSize = 20.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.amountPaise?.let { CurrencyFormatter.format(it) } ?: item.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(role = Role.Button, onClickLabel = "Mark ${item.title} done", onClick = onMarkDone)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("Done", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Shown when there is nothing open: a calm confirmation instead of an empty gap. */
@Composable
fun AllClearHero(modifier: Modifier = Modifier) {
    HeroSurface(
        brush = urgencyBrush(ExpiryTheme.colors.ok.accent),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            }
            Column {
                Text("All clear", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Nothing is waiting on you right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun HeroSurface(
    brush: Brush,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column { content() }
    }
}

/** Darkens the accent enough that white text keeps at least 4.5:1 contrast in both themes. */
@Composable
fun urgencyBrush(accent: Color): Brush {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val black = Color.Black
    val start = if (dark) accent.mix(black, 0.38f) else accent.mix(black, 0.1f)
    val end = if (dark) accent.mix(black, 0.62f) else accent.mix(black, 0.45f)
    return Brush.linearGradient(listOf(start, end))
}

private fun Color.mix(other: Color, fraction: Float): Color =
    androidx.compose.ui.graphics.lerp(this, other, fraction)
