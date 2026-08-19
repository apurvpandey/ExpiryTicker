package com.apurvpandey.expiryticker.presentation.addedit

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.presentation.components.icon
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTickerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val reminderOptions = listOf(
    0 to "On date",
    1 to "1 day",
    3 to "3 days",
    7 to "7 days",
    14 to "14 days",
    30 to "30 days"
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
    ) { /* Notifications are best-effort; proceed regardless of grant result */ }

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
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit reminder" else "Add reminder",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text("Title *") },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Category", isError = uiState.categoryError != null)
            Spacer(Modifier.height(8.dp))

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
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            // Due date — entire field is tappable
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.dueDate?.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    ) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due date *") },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    },
                    isError = uiState.dueDateError != null,
                    supportingText = uiState.dueDateError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Remind me")
            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                reminderOptions.forEach { (days, label) ->
                    FilterChip(
                        selected = uiState.reminderDaysBefore == days,
                        onClick = { onReminderChange(days) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Recurrence")
            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RecurrenceType.entries.forEach { rec ->
                    FilterChip(
                        selected = uiState.recurrence == rec,
                        onClick = { onRecurrenceChange(rec) },
                        label = { Text(rec.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            // Optional details
            SectionLabel("Details (optional)")
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = onAmountChange,
                label = { Text("Expected renewal cost (₹)") },
                placeholder = { Text("e.g. 5000") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (uiState.isEditMode) "Save changes" else "Save reminder")
                }
            }

            Spacer(Modifier.height(24.dp))
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
private fun SectionLabel(text: String, isError: Boolean = false) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "AddEdit – New")
@Composable
private fun AddEditPreviewNew() {
    ExpiryTickerTheme {
        AddEditScreen(
            uiState = AddEditUiState(),
            onNavigateBack = {}, onTitleChange = {}, onCategoryChange = {},
            onDueDateChange = {}, onReminderChange = {}, onRecurrenceChange = {},
            onAmountChange = {}, onNotesChange = {}, onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "AddEdit – Populated")
@Composable
private fun AddEditPreviewPopulated() {
    ExpiryTickerTheme {
        AddEditScreen(
            uiState = AddEditUiState(
                isEditMode = true,
                title = "Car Insurance",
                selectedCategory = RenewalCategory.INSURANCE,
                dueDate = LocalDate.of(2026, 9, 15),
                reminderDaysBefore = 7,
                recurrence = RecurrenceType.YEARLY,
                amountText = "12000",
                notes = "HDFC ERGO portal"
            ),
            onNavigateBack = {}, onTitleChange = {}, onCategoryChange = {},
            onDueDateChange = {}, onReminderChange = {}, onRecurrenceChange = {},
            onAmountChange = {}, onNotesChange = {}, onSave = {}
        )
    }
}

@Preview(
    showBackground = true, name = "AddEdit – Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AddEditPreviewDark() {
    ExpiryTickerTheme {
        AddEditScreen(
            uiState = AddEditUiState(
                title = "Netflix",
                selectedCategory = RenewalCategory.SUBSCRIPTION,
                reminderDaysBefore = 3,
                recurrence = RecurrenceType.MONTHLY
            ),
            onNavigateBack = {}, onTitleChange = {}, onCategoryChange = {},
            onDueDateChange = {}, onReminderChange = {}, onRecurrenceChange = {},
            onAmountChange = {}, onNotesChange = {}, onSave = {}
        )
    }
}
