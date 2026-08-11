package com.apurvpandey.expiryticker.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.apurvpandey.expiryticker.domain.model.toDisplayText
import com.apurvpandey.expiryticker.presentation.components.icon
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.item?.title ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDeleteClicked) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
            uiState.item == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) { Text("Item not found.") }
            }
            else -> {
                DetailContent(
                    item = uiState.item,
                    status = uiState.status,
                    onMarkRenewed = onMarkRenewed,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text("Delete item?") },
            text = { Text("This will permanently remove \"${uiState.item?.title}\" and cancel its reminder. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = onDeleteConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
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
    onMarkRenewed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header with icon and status
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = item.category.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (status != null) {
            val statusColor = when (status) {
                is ExpiryStatus.Overdue -> MaterialTheme.colorScheme.error
                is ExpiryStatus.DueToday -> MaterialTheme.colorScheme.tertiary
                is ExpiryStatus.Active -> if (status.daysRemaining <= 7)
                    MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurface
                is ExpiryStatus.Completed -> MaterialTheme.colorScheme.outline
            }
            Text(
                text = status.toDisplayText(),
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
        }

        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        DetailRow("Due date", item.dueDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
        DetailRow("Reminder", "${item.reminderDaysBefore} days before")
        DetailRow("Recurrence", item.recurrence.displayName)
        if (item.amountPaise != null) {
            DetailRow("Expected cost", CurrencyFormatter.format(item.amountPaise))
        }
        if (item.notes.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))

        if (!item.isCompleted) {
            val renewLabel = if (item.recurrence != RecurrenceType.NONE)
                "Mark as renewed"
            else
                "Mark as completed"
            Button(
                onClick = onMarkRenewed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(renewLabel)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
