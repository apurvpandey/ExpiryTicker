package com.apurvpandey.expiryticker.presentation.addedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.apurvpandey.expiryticker.presentation.components.CategoryIconBadge
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
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
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
        onRenewalUrlChange = viewModel::onRenewalUrlChange,
        onPhoneChange = viewModel::onPhoneChange,
        onExtraReminderToggle = viewModel::onExtraReminderToggle,
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
    onRenewalUrlChange: (String) -> Unit = {},
    onPhoneChange: (String) -> Unit = {},
    onExtraReminderToggle: (Int) -> Unit = {},
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoading) {
                // Pinned so the primary action is always reachable without scrolling the form.
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.imePadding()
                ) {
                    Button(
                        onClick = onSave,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                }
            }
        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text("Title *") },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(Modifier.height(16.dp))
            FormCard(title = "Category", isError = uiState.categoryError != null) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val gap = 8.dp
                    val tileWidth = (maxWidth - gap * 3) / 4
                    FlowRow(
                        maxItemsInEachRow = 4,
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        RenewalCategory.entries.forEach { cat ->
                            CategoryTile(
                                category = cat,
                                selected = uiState.selectedCategory == cat,
                                onClick = { onCategoryChange(cat) },
                                modifier = Modifier.width(tileWidth)
                            )
                        }
                    }
                }
                uiState.categoryError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            FormCard(title = "When") {
                // Due date: the whole field is tappable
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
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClickLabel = "Pick due date") { showDatePicker = true }
                    )
                }

                // Quick picks: most renewals are a round period from today.
                val today = LocalDate.now()
                val quickDates = listOf(
                    "Today" to today,
                    "+1 month" to today.plusMonths(1),
                    "+3 months" to today.plusMonths(3),
                    "+6 months" to today.plusMonths(6),
                    "+1 year" to today.plusYears(1)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    quickDates.forEach { (label, date) ->
                        FilterChip(
                            selected = uiState.dueDate == date,
                            onClick = { onDueDateChange(date) },
                            label = { Text(label) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                SectionLabel("Remind me")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    reminderOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = uiState.reminderDaysBefore == days,
                            onClick = { onReminderChange(days) },
                            label = { Text(label) }
                        )
                    }
                }

                SectionLabel("Also remind me")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    reminderOptions
                        .filter { (days, _) -> days != uiState.reminderDaysBefore }
                        .forEach { (days, label) ->
                            FilterChip(
                                selected = days in uiState.extraReminders,
                                onClick = { onExtraReminderToggle(days) },
                                label = { Text(label) }
                            )
                        }
                }

                SectionLabel("Repeats")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    RecurrenceType.entries.forEach { rec ->
                        FilterChip(
                            selected = uiState.recurrence == rec,
                            onClick = { onRecurrenceChange(rec) },
                            label = { Text(rec.displayName) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FormCard(title = "Details (optional)") {
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
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.renewalUrl,
                    onValueChange = onRenewalUrlChange,
                    label = { Text("Renewal website") },
                    placeholder = { Text("e.g. insurer.com/renew") },
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    isError = uiState.renewalUrlError != null,
                    supportingText = uiState.renewalUrlError?.let { { Text(it) } },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Provider phone") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    isError = uiState.phoneError != null,
                    supportingText = uiState.phoneError?.let { { Text(it) } },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
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
private fun FormCard(
    title: String,
    isError: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel(title, isError)
            content()
        }
    }
}

@Composable
private fun CategoryTile(
    category: RenewalCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else Color.Transparent
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                shape = shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CategoryIconBadge(category = category, size = 40.dp, iconSize = 20.dp)
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2
        )
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
