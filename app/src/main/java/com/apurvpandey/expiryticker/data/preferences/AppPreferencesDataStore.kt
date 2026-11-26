package com.apurvpandey.expiryticker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val currencyKey = stringPreferencesKey("currency_code")
    private val appLockKey = booleanPreferencesKey("app_lock_enabled")
    private val appThemeKey = stringPreferencesKey("app_theme")

    val defaultReminderDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[defaultReminderDaysKey] ?: 7
    }

    val reminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[reminderHourKey] ?: 9
    }

    suspend fun setReminderHour(hour: Int) {
        context.dataStore.edit { prefs -> prefs[reminderHourKey] = hour.coerceIn(0, 23) }
    }

    val currencyCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[currencyKey] ?: "INR"
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { prefs -> prefs[currencyKey] = code }
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[appLockKey] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[appLockKey] = enabled }
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
