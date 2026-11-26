package com.apurvpandey.expiryticker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.apurvpandey.expiryticker.presentation.theme.urgencyColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus

@Composable
fun StatusChip(status: ExpiryStatus, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = statusColors(status)
    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon(status),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = status.shortLabel(LocalContext.current.resources),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun statusIcon(status: ExpiryStatus): ImageVector = when (status) {
    is ExpiryStatus.Overdue -> Icons.Outlined.Warning
    is ExpiryStatus.DueToday -> Icons.Outlined.Alarm
    is ExpiryStatus.Active -> if (status.daysRemaining <= 7) Icons.Outlined.Schedule else Icons.Outlined.DateRange
    is ExpiryStatus.Completed -> Icons.Outlined.CheckCircle
}

@Composable
fun statusColors(status: ExpiryStatus): Pair<Color, Color> {
    val u = status.urgencyColors()
    return u.container to u.onContainer
}
