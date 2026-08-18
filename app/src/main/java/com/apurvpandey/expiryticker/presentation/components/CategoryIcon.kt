package com.apurvpandey.expiryticker.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import com.apurvpandey.expiryticker.domain.model.RenewalCategory

fun RenewalCategory.icon(): ImageVector = when (this) {
    RenewalCategory.VEHICLE -> Icons.Default.DirectionsCar
    RenewalCategory.INSURANCE -> Icons.Default.Shield
    RenewalCategory.TRAVEL -> Icons.Default.Flight
    RenewalCategory.LICENCE -> Icons.Default.Badge
    RenewalCategory.WARRANTY -> Icons.Default.Verified
    RenewalCategory.SUBSCRIPTION -> Icons.Default.Subscriptions
    RenewalCategory.DOMAIN -> Icons.Default.Language
    RenewalCategory.CERTIFICATION -> Icons.Default.Star
    RenewalCategory.MEMBERSHIP -> Icons.Default.CardMembership
    RenewalCategory.AMC -> Icons.Default.Build
    RenewalCategory.FINANCE -> Icons.Default.AccountBalance
    RenewalCategory.HEALTH -> Icons.Default.MedicalServices
    RenewalCategory.HOME -> Icons.Default.Home
    RenewalCategory.OTHER -> Icons.Default.Category
}
