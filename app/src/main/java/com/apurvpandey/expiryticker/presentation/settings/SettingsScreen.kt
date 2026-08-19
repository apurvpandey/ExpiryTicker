package com.apurvpandey.expiryticker.presentation.settings

import android.content.Intent
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
import com.apurvpandey.expiryticker.presentation.theme.AppTheme

private val reminderOptions = listOf(
    1 to "1 day before",
    3 to "3 days before",
    7 to "7 days before",
    14 to "14 days before",
    30 to "30 days before"
)

private val themeOptions = listOf(
    AppTheme.LIGHT to "Light",
    AppTheme.SYSTEM to "System",
    AppTheme.DARK to "Dark"
)

@Composable
fun SettingsRoute(
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.appPreferences)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDefaultReminderChange = viewModel::setDefaultReminderDays,
        onThemeChange = viewModel::setAppTheme
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onDefaultReminderChange: (Int) -> Unit,
    onThemeChange: (AppTheme) -> Unit
) {
    val context = LocalContext.current
    var showReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            SectionLabel("Notifications")
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                val currentReminderLabel = reminderOptions
                    .find { it.first == uiState.defaultReminderDays }?.second
                    ?: "${uiState.defaultReminderDays} days before"

                ListItem(
                    headlineContent = { Text("Default reminder") },
                    supportingContent = { Text(currentReminderLabel) },
                    leadingContent = {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.clickable { showReminderDialog = true }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Notification permission") },
                        supportingContent = { Text("Manage in system settings") },
                        leadingContent = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.clickable {
                            val intent = Intent(SystemSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(SystemSettings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Appearance ──────────────────────────────────────────────
            SectionLabel("Appearance")
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (theme, label) ->
                            SegmentedButton(
                                selected = uiState.appTheme == theme,
                                onClick = { onThemeChange(theme) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = themeOptions.size
                                ),
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── About ───────────────────────────────────────────────────
            SectionLabel("About")
            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                ListItem(
                    headlineContent = { Text("ExpiryTicker") },
                    supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Default reminder") },
            text = {
                Column {
                    reminderOptions.forEach { (days, label) ->
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
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large
    ) {
        content()
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
