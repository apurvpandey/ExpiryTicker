package com.apurvpandey.expiryticker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apurvpandey.expiryticker.data.preferences.AppPreferencesDataStore
import com.apurvpandey.expiryticker.presentation.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultReminderDays: Int = 7,
    val appTheme: AppTheme = AppTheme.SYSTEM
)

class SettingsViewModel(
    private val appPreferences: AppPreferencesDataStore
) : ViewModel() {

    val uiState = combine(
        appPreferences.defaultReminderDays,
        appPreferences.appTheme
    ) { days, theme ->
        SettingsUiState(defaultReminderDays = days, appTheme = theme)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setDefaultReminderDays(days: Int) {
        viewModelScope.launch { appPreferences.setDefaultReminderDays(days) }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { appPreferences.setAppTheme(theme) }
    }

    companion object {
        fun factory(appPreferences: AppPreferencesDataStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(appPreferences) as T
            }
    }
}
