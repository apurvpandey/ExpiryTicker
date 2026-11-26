package com.apurvpandey.expiryticker.presentation.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.apurvpandey.expiryticker.presentation.components.countdown
import com.apurvpandey.expiryticker.presentation.components.urgencyBrush
import com.apurvpandey.expiryticker.presentation.theme.urgencyColors
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.presentation.components.CategoryIconBadge
import com.apurvpandey.expiryticker.presentation.components.StatusChip
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTickerTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ExpiryDetailRoute(
    container: AppContainer,
    itemId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val viewModel: ExpiryDetailViewModel = viewModel(
        key = "detail_$itemId",
        factory = ExpiryDetailViewModel.factory(
            itemId = itemId,
            repository = container.expiryItemRepository,
            reminderScheduler = container.reminderScheduler
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    ExpiryDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = { uiState.item?.id?.let(onNavigateToEdit) },
        onMarkRenewed = viewModel::markRenewed,
        onDeleteClicked = viewModel::showDeleteConfirmation,
        onDeleteConfirmed = viewModel::deleteItem,
        onDeleteDismissed = viewModel::dismissDeleteConfirmation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDetailScreen(
    uiState: ExpiryDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onMarkRenewed: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit
) {
    val item = uiState.item

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        },
        bottomBar = {
            if (item != null && !item.isCompleted) {
                val renewLabel = if (item.recurrence != RecurrenceType.NONE)
                    "Mark as renewed" else "Mark as completed"
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider()
                    Button(
                        onClick = onMarkRenewed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(renewLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            item == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("Item not found.") }
            }
            else -> {
                DetailContent(
                    item = item,
                    status = uiState.status,
                    onDeleteClicked = onDeleteClicked,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text("Delete item?") },
            text = {
                Text("\"${item?.title}\" will be permanently removed and its reminder cancelled.")
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirmed,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismissed) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailContent(
    item: ExpiryItem,
    status: ExpiryStatus?,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val longDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailHero(item = item, status = status)

        InfoCard {
            DetailRow(
                icon = Icons.Outlined.Event,
                label = "Due date",
                value = item.dueDate.format(longDate)
            )
            DetailRow(
                icon = Icons.Outlined.NotificationsActive,
                label = "Reminder",
                value = when (item.reminderDaysBefore) {
                    0 -> "On due date"
                    1 -> "1 day before"
                    else -> "${item.reminderDaysBefore} days before"
                }
            )
            DetailRow(
                icon = Icons.Outlined.Repeat,
                label = "Repeats",
                value = item.recurrence.displayName
            )
            if (item.amountPaise != null) {
                DetailRow(
                    icon = Icons.Outlined.Payments,
                    label = "Expected cost",
                    value = CurrencyFormatter.format(item.amountPaise)
                )
            }
            item.lastRenewedAt?.let { instant ->
                DetailRow(
                    icon = Icons.Outlined.History,
                    label = "Last renewed",
                    value = instant.atZone(ZoneId.systemDefault()).toLocalDate().format(longDate)
                )
            }
        }

        if (item.notes.isNotBlank()) {
            InfoCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "NOTES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        TextButton(
            onClick = onDeleteClicked,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Delete item", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** Gradient header that follows the item's urgency and leads with the countdown. */
@Composable
private fun DetailHero(item: ExpiryItem, status: ExpiryStatus?) {
    val accent = status?.urgencyColors()?.accent ?: MaterialTheme.colorScheme.primary
    val countdown = status?.countdown()
    val headline = when (status) {
        is ExpiryStatus.Overdue, is ExpiryStatus.Active -> "${countdown?.value} ${countdown?.caption}"
        is ExpiryStatus.DueToday -> "Due today"
        is ExpiryStatus.Completed -> "Completed"
        null -> ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(urgencyBrush(accent))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CategoryIconBadge(category = item.category, size = 52.dp, iconSize = 26.dp)
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        if (headline.isNotEmpty()) {
            Text(
                text = headline,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

private val previewItem = ExpiryItem(
    id = 1L,
    title = "Car Insurance",
    category = RenewalCategory.INSURANCE,
    dueDate = LocalDate.of(2026, 8, 24),
    recurrence = RecurrenceType.YEARLY,
    reminderDaysBefore = 7,
    amountPaise = 1200000L,
    notes = "Renew via HDFC ERGO portal. Policy number: HE-2024-001."
)

@Preview(showBackground = true, name = "Detail – Upcoming")
@Composable
private fun DetailPreviewUpcoming() {
    ExpiryTickerTheme {
        ExpiryDetailScreen(
            uiState = ExpiryDetailUiState(
                isLoading = false,
                item = previewItem,
                status = ExpiryStatus.Active(5)
            ),
            onNavigateBack = {}, onNavigateToEdit = {},
            onMarkRenewed = {}, onDeleteClicked = {},
            onDeleteConfirmed = {}, onDeleteDismissed = {}
        )
    }
}

@Preview(showBackground = true, name = "Detail – Overdue")
@Composable
private fun DetailPreviewOverdue() {
    ExpiryTickerTheme {
        ExpiryDetailScreen(
            uiState = ExpiryDetailUiState(
                isLoading = false,
                item = previewItem.copy(dueDate = LocalDate.of(2026, 8, 15)),
                status = ExpiryStatus.Overdue(4)
            ),
            onNavigateBack = {}, onNavigateToEdit = {},
            onMarkRenewed = {}, onDeleteClicked = {},
            onDeleteConfirmed = {}, onDeleteDismissed = {}
        )
    }
}

@Preview(
    showBackground = true, name = "Detail – Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DetailPreviewDark() {
    ExpiryTickerTheme {
        ExpiryDetailScreen(
            uiState = ExpiryDetailUiState(
                isLoading = false,
                item = previewItem,
                status = ExpiryStatus.Active(5)
            ),
            onNavigateBack = {}, onNavigateToEdit = {},
            onMarkRenewed = {}, onDeleteClicked = {},
            onDeleteConfirmed = {}, onDeleteDismissed = {}
        )
    }
}
