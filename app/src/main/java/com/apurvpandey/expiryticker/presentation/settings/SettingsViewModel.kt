package com.apurvpandey.expiryticker.presentation.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.core.util.CsvExporter
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
    val appTheme: AppTheme = AppTheme.SYSTEM
)

class SettingsViewModel(
    private val appPreferences: AppPreferencesDataStore,
    private val repository: ExpiryItemRepository
) : ViewModel() {

    val uiState = combine(
        appPreferences.defaultReminderDays,
        appPreferences.reminderHour,
        appPreferences.appTheme
    ) { days, hour, theme ->
        SettingsUiState(defaultReminderDays = days, reminderHour = hour, appTheme = theme)
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

    companion object {
        fun factory(
            appPreferences: AppPreferencesDataStore,
            repository: ExpiryItemRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(appPreferences, repository) as T
        }
    }
}
