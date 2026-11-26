package com.apurvpandey.expiryticker.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.CsvExporter
import com.apurvpandey.expiryticker.data.backup.BackupFormatException
import com.apurvpandey.expiryticker.data.backup.BackupManager
import com.apurvpandey.expiryticker.data.preferences.AppPreferencesDataStore
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.presentation.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val defaultReminderDays: Int = 7,
    val reminderHour: Int = 9,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val appLockEnabled: Boolean = false
)

class SettingsViewModel(
    private val appPreferences: AppPreferencesDataStore,
    private val repository: ExpiryItemRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val uiState = combine(
        appPreferences.defaultReminderDays,
        appPreferences.reminderHour,
        appPreferences.appTheme,
        appPreferences.appLockEnabled
    ) { days, hour, theme, lock ->
        SettingsUiState(defaultReminderDays = days, reminderHour = hour, appTheme = theme, appLockEnabled = lock)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setDefaultReminderDays(days: Int) {
        viewModelScope.launch { appPreferences.setDefaultReminderDays(days) }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch { appPreferences.setReminderHour(hour) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAppLockEnabled(enabled) }
    }

    fun showMessage(message: String) {
        _messages.tryEmit(message)
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { appPreferences.setAppTheme(theme) }
    }

    fun exportTo(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val items = repository.observeAll().first()
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(CsvExporter.export(items)) }
                        ?: error("Could not open file")
                }
                items.size
            }
            _messages.emit(
                result.fold(
                    onSuccess = { "Exported $it items" },
                    onFailure = { "Export failed" }
                )
            )
        }
    }

    fun backupTo(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val out = resolver.openOutputStream(uri) ?: error("Could not open file")
                    backupManager.export(out)
                }
            }
            _messages.emit(
                result.fold(
                    onSuccess = { "Backed up $it items" },
                    onFailure = { "Backup failed" }
                )
            )
        }
    }

    fun restoreFrom(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val input = resolver.openInputStream(uri) ?: error("Could not open file")
                    input.use { backupManager.restore(it) }
                }
            }
            _messages.emit(
                result.fold(
                    onSuccess = {
                        val skipped = if (it.skippedAttachments > 0) " (${it.skippedAttachments} attachments missing)" else ""
                        "Restored ${it.items} items$skipped"
                    },
                    onFailure = { e -> if (e is BackupFormatException) e.message.orEmpty() else "Restore failed" }
                )
            )
        }
    }

    companion object {
        fun factory(
            appPreferences: AppPreferencesDataStore,
            repository: ExpiryItemRepository,
            backupManager: BackupManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(appPreferences, repository, backupManager) as T
        }
    }
}
