package com.apurvpandey.expiryticker.presentation.settings

import com.apurvpandey.expiryticker.presentation.common.UiText
import androidx.compose.ui.res.stringResource
import com.apurvpandey.expiryticker.R
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.apurvpandey.expiryticker.notification.ExpiryNotificationManager
import com.apurvpandey.expiryticker.presentation.components.openNotificationSettings
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.security.AppLock
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.BuildConfig
import com.apurvpandey.expiryticker.presentation.common.reminderLabel
import com.apurvpandey.expiryticker.presentation.common.resources
import com.apurvpandey.expiryticker.presentation.theme.AppTheme

private val reminderDayOptions = listOf(1, 3, 7, 14, 30)

private val themeOptions = listOf(
    AppTheme.LIGHT to R.string.light,
    AppTheme.SYSTEM to R.string.system,
    AppTheme.DARK to R.string.dark
)

@Composable
fun SettingsRoute(
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.appPreferences, container.expiryItemRepository, container.backupManager)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportTo(context.contentResolver, it) } }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.backupTo(context.contentResolver, it) } }
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestore = uri }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context.resources)) }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onDefaultReminderChange = viewModel::setDefaultReminderDays,
        onReminderHourChange = viewModel::setReminderHour,
        onSendTestNotification = { ExpiryNotificationManager.showTest(context) },
        onExport = { exportLauncher.launch("expiryticker-export.csv") },
        onBackup = { backupLauncher.launch("expiryticker-backup-${java.time.LocalDate.now()}.zip") },
        onRestore = { restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
        pendingRestore = pendingRestore,
        onRestoreConfirmed = { uri ->
            pendingRestore = null
            viewModel.restoreFrom(context.contentResolver, uri)
        },
        onRestoreDismissed = { pendingRestore = null },
        onThemeChange = viewModel::setAppTheme,
        onCurrencyChange = viewModel::setCurrency,
        onAppLockChange = { enabled ->
            if (enabled && !AppLock.isAvailable(context)) {
                viewModel.showMessage(UiText.of(R.string.err_set_up_screen_lock))
            } else {
                viewModel.setAppLockEnabled(enabled)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onDefaultReminderChange: (Int) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onReminderHourChange: (Int) -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onExport: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    pendingRestore: android.net.Uri? = null,
    onRestoreConfirmed: (android.net.Uri) -> Unit = {},
    onRestoreDismissed: () -> Unit = {},
    onAppLockChange: (Boolean) -> Unit = {},
    onCurrencyChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showReminderDialog by remember { mutableStateOf(false) }
    var showHourDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ─── Notifications ───────────────────────────────────────────
            SectionLabel(stringResource(R.string.notifications))
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                val currentReminderLabel = reminderLabel(resources(), uiState.defaultReminderDays)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.default_reminder)) },
                    supportingContent = { Text(currentReminderLabel) },
                    leadingContent = {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable { showReminderDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.reminder_time)) },
                    supportingContent = { Text(formatHour(uiState.reminderHour)) },
                    leadingContent = {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable { showHourDialog = true }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.notification_permission)) },
                        supportingContent = { Text(stringResource(R.string.manage_in_system_settings)) },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clickable { openNotificationSettings(context) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.send_test_notification)) },
                    supportingContent = { Text(stringResource(R.string.check_that_reminders_reach_you)) },
                    leadingContent = {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable(onClick = onSendTestNotification)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── Appearance ──────────────────────────────────────────────
            SectionLabel(stringResource(R.string.appearance))
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (theme, labelRes) ->
                            SegmentedButton(
                                selected = uiState.appTheme == theme,
                                onClick = { onThemeChange(theme) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = themeOptions.size
                                ),
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                }
            }

            SettingsGroup {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.currency)) },
                    supportingContent = { Text(stringResource(R.string.currency_summary, uiState.currencyCode)) },
                    leadingContent = { Icon(Icons.Default.Payments, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable { showCurrencyDialog = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── Privacy ─────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.privacy))
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_lock)) },
                    supportingContent = {
                        Text(stringResource(R.string.ask_for_fingerprint_face_or_screen))
                    },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingContent = {
                        androidx.compose.material3.Switch(
                            checked = uiState.appLockEnabled,
                            onCheckedChange = onAppLockChange
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable { onAppLockChange(!uiState.appLockEnabled) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── Data ────────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.data))
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.export_to_csv)) },
                    supportingContent = { Text(stringResource(R.string.save_all_items_as_a_spreadsheet)) },
                    leadingContent = {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable(onClick = onExport)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.back_up_everything)) },
                    supportingContent = { Text(stringResource(R.string.items_history_and_attachments_in_one)) },
                    leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable(onClick = onBackup)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.restore_from_backup)) },
                    supportingContent = { Text(stringResource(R.string.replaces_everything_currently_in_the_app)) },
                    leadingContent = { Icon(Icons.Default.Restore, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable(onClick = onRestore)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── About ───────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.about))
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME)) },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = onRestoreDismissed,
            title = { Text(stringResource(R.string.replace_everything)) },
            text = {
                Text(stringResource(R.string.restore_body))
            },
            confirmButton = {
                TextButton(
                    onClick = { onRestoreConfirmed(uri) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.replace)) }
            },
            dismissButton = { TextButton(onClick = onRestoreDismissed) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(stringResource(R.string.currency)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CurrencyFormatter.supportedCodes.forEach { code ->
                        val name = java.util.Currency.getInstance(code).displayName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCurrencyChange(code); showCurrencyDialog = false }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.currencyCode == code,
                                onClick = { onCurrencyChange(code); showCurrencyDialog = false }
                            )
                            Text("$code  ·  $name", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCurrencyDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showHourDialog) {
        AlertDialog(
            onDismissRequest = { showHourDialog = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    (6..22).forEach { hour ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onReminderHourChange(hour)
                                    showHourDialog = false
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.reminderHour == hour,
                                onClick = {
                                    onReminderHourChange(hour)
                                    showHourDialog = false
                                }
                            )
                            Text(
                                text = formatHour(hour),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showHourDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text(stringResource(R.string.default_reminder)) },
            text = {
                Column {
                    reminderDayOptions.forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDefaultReminderChange(days)
                                    showReminderDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.defaultReminderDays == days,
                                onClick = {
                                    onDefaultReminderChange(days)
                                    showReminderDialog = false
                                }
                            )
                            Text(
                                text = reminderLabel(resources(), days),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

private fun formatHour(hour: Int): String =
    java.time.LocalTime.of(hour, 0).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        // Rows must be stacked vertically; a bare Surface would draw them on top of each other.
        Column { content() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}
