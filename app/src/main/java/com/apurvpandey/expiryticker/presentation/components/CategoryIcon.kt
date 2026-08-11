package com.apurvpandey.expiryticker.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import com.apurvpandey.expiryticker.domain.model.RenewalCategory

fun RenewalCategory.icon(): ImageVector = when (this) {
    RenewalCategory.VEHICLE -> Icons.Default.DirectionsCar
    RenewalCategory.INSURANCE -> Icons.Default.Shield
    RenewalCategory.DOCUMENT -> Icons.Default.Description
    RenewalCategory.SUBSCRIPTION -> Icons.Default.Subscriptions
    RenewalCategory.WARRANTY -> Icons.Default.Verified
    RenewalCategory.FINANCE -> Icons.Default.AccountBalance
    RenewalCategory.HEALTH -> Icons.Default.MedicalServices
    RenewalCategory.CERTIFICATION -> Icons.Default.CardMembership
    RenewalCategory.HOME -> Icons.Default.Home
    RenewalCategory.OTHER -> Icons.Default.Category
}
