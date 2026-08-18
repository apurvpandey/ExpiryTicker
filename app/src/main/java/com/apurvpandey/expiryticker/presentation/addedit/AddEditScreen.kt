package com.apurvpandey.expiryticker.presentation.addedit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.presentation.components.icon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val reminderOptions = listOf(
    0 to "On due date",
    1 to "1 day before",
    3 to "3 days before",
    7 to "7 days before",
    14 to "14 days before",
    30 to "30 days before"
)

@Composable
fun AddEditRoute(
    container: AppContainer,
    editItemId: Long?,
    onNavigateBack: () -> Unit
) {
    val viewModel: AddEditViewModel = viewModel(
        key = "addedit_${editItemId ?: "new"}",
        factory = AddEditViewModel.factory(
            repository = container.expiryItemRepository,
            reminderScheduler = container.reminderScheduler,
            appPreferences = container.appPreferences,
            editItemId = editItemId
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Notifications are best-effort; we proceed regardless of grant result */ }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    AddEditScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onTitleChange = viewModel::onTitleChange,
        onCategoryChange = viewModel::onCategoryChange,
        onDueDateChange = viewModel::onDueDateChange,
        onReminderChange = viewModel::onReminderChange,
        onRecurrenceChange = viewModel::onRecurrenceChange,
        onAmountChange = viewModel::onAmountChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    uiState: AddEditUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (RenewalCategory) -> Unit,
    onDueDateChange: (LocalDate) -> Unit,
    onReminderChange: (Int) -> Unit,
    onRecurrenceChange: (RecurrenceType) -> Unit,
    onAmountChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderExpanded by remember { mutableStateOf(false) }
    var recurrenceExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit reminder" else "Add reminder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Basic Info ---
            FormCard {
                SectionHeader("Basic Info")
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = { Text("Title *") },
                    isError = uiState.titleError != null,
                    supportingText = uiState.titleError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                FieldLabel("Category *", isError = uiState.categoryError != null)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RenewalCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = uiState.selectedCategory == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                uiState.categoryError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // --- Schedule ---
            FormCard {
                SectionHeader("Schedule")
                OutlinedTextField(
                    value = uiState.dueDate?.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    ) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due date *") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                        }
                    },
                    isError = uiState.dueDateError != null,
                    supportingText = uiState.dueDateError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = reminderExpanded,
                    onExpandedChange = { reminderExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val reminderLabel = reminderOptions.find { it.first == uiState.reminderDaysBefore }?.second
                        ?: "${uiState.reminderDaysBefore} days before"
                    OutlinedTextField(
                        value = reminderLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reminder") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = reminderExpanded,
                        onDismissRequest = { reminderExpanded = false }
                    ) {
                        reminderOptions.forEach { (days, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onReminderChange(days)
                                    reminderExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = recurrenceExpanded,
                    onExpandedChange = { recurrenceExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.recurrence.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recurrence") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = recurrenceExpanded,
                        onDismissRequest = { recurrenceExpanded = false }
                    ) {
                        RecurrenceType.entries.forEach { rec ->
                            DropdownMenuItem(
                                text = { Text(rec.displayName) },
                                onClick = {
                                    onRecurrenceChange(rec)
                                    recurrenceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- Details ---
            FormCard {
                SectionHeader("Details")
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = onAmountChange,
                    label = { Text("Expected renewal cost (₹)") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    Text(if (uiState.isEditMode) "Save changes" else "Save reminder")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dueDate
                ?.atStartOfDay(ZoneId.of("UTC"))
                ?.toInstant()
                ?.toEpochMilli()
                ?: Instant.now().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDueDateChange(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun FieldLabel(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
