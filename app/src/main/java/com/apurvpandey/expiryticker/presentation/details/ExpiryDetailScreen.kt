package com.apurvpandey.expiryticker.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.presentation.components.CategoryIconBadge
import com.apurvpandey.expiryticker.presentation.components.StatusChip
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

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    ExpiryDetailScreen(
        uiState = uiState,
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
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onMarkRenewed: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit
) {
    val item = uiState.item

    Scaffold(
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
                Column {
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero — neutral surface, status conveyed only through the chip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CategoryIconBadge(
                    category = item.category,
                    size = 56.dp,
                    iconSize = 28.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (status != null) {
                        StatusChip(status = status)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow(
                        label = "Due date",
                        value = item.dueDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    DetailRow(
                        label = "Reminder",
                        value = if (item.reminderDaysBefore == 0) "On due date"
                                else "${item.reminderDaysBefore} days before expiry"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    DetailRow(label = "Recurrence", value = item.recurrence.displayName)
                    if (item.amountPaise != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow(
                            label = "Expected cost",
                            value = CurrencyFormatter.format(item.amountPaise)
                        )
                    }
                    item.lastRenewedAt?.let { instant ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow(
                            label = "Last renewed",
                            value = instant
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                        )
                    }
                }
            }

            if (item.notes.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodyMedium
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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text("Delete item", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.55f)
        )
    }
}
