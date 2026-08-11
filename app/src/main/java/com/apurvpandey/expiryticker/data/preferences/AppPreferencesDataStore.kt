package com.apurvpandey.expiryticker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apurvpandey.expiryticker.presentation.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferencesDataStore(private val context: Context) {

    private val defaultReminderDaysKey = intPreferencesKey("default_reminder_days")
    private val appThemeKey = stringPreferencesKey("app_theme")

    val defaultReminderDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[defaultReminderDaysKey] ?: 7
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.entries.find { it.name == prefs[appThemeKey] } ?: AppTheme.SYSTEM
    }

    suspend fun setDefaultReminderDays(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[defaultReminderDaysKey] = days
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[appThemeKey] = theme.name
        }
    }
}
