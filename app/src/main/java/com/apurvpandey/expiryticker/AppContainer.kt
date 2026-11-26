package com.apurvpandey.expiryticker

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import androidx.glance.appwidget.updateAll
import com.apurvpandey.expiryticker.widget.ExpiryWidget
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.room.Room
import com.apurvpandey.expiryticker.data.local.database.ExpiryTickerDatabase
import com.apurvpandey.expiryticker.data.local.database.MIGRATION_1_2
import com.apurvpandey.expiryticker.data.preferences.AppPreferencesDataStore
import com.apurvpandey.expiryticker.data.repository.ExpiryItemRepositoryImpl
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.domain.usecase.ItemActions
import com.apurvpandey.expiryticker.notification.ReminderScheduler

@OptIn(FlowPreview::class)
class AppContainer(context: Context) {

    private val database: ExpiryTickerDatabase = Room.databaseBuilder(
        context.applicationContext,
        ExpiryTickerDatabase::class.java,
        "expiry_ticker.db"
    )
        .addMigrations(MIGRATION_1_2)
        .build()

    val expiryItemRepository: ExpiryItemRepository =
        ExpiryItemRepositoryImpl(database.expiryItemDao())

    val appPreferences: AppPreferencesDataStore =
        AppPreferencesDataStore(context.applicationContext)

    val reminderScheduler: ReminderScheduler =
        ReminderScheduler(context.applicationContext)

    val itemActions: ItemActions = ItemActions(expiryItemRepository, reminderScheduler)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Refresh the home-screen widget whenever the data changes (and once at start).
        appScope.launch {
            expiryItemRepository.observeAll().debounce(500).collect {
                runCatching { ExpiryWidget().updateAll(context.applicationContext) }
            }
        }

        // Keep the scheduler's reminder hour in sync with settings; a change (or app start)
        // re-plans every open item so reminders always match the current configuration.
        appScope.launch {
            appPreferences.reminderHour.collectLatest { hour ->
                reminderScheduler.reminderHour = hour
                expiryItemRepository.observeAll().first()
                    .filterNot { it.isCompleted }
                    .forEach(reminderScheduler::scheduleReminder)
            }
        }
    }
}
