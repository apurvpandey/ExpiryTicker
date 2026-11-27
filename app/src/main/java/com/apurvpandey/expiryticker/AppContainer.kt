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
import com.apurvpandey.expiryticker.data.backup.BackupManager
import com.apurvpandey.expiryticker.data.files.AttachmentStore
import com.apurvpandey.expiryticker.data.local.database.MIGRATION_2_3
import com.apurvpandey.expiryticker.data.repository.AttachmentRepositoryImpl
import com.apurvpandey.expiryticker.data.repository.RenewalHistoryRepositoryImpl
import com.apurvpandey.expiryticker.domain.repository.AttachmentRepository
import com.apurvpandey.expiryticker.domain.repository.RenewalHistoryRepository
import com.apurvpandey.expiryticker.domain.usecase.ItemActions
import com.apurvpandey.expiryticker.notification.ReminderScheduler

@OptIn(FlowPreview::class)
class AppContainer(context: Context) {

    private val database: ExpiryTickerDatabase = Room.databaseBuilder(
        context.applicationContext,
        ExpiryTickerDatabase::class.java,
        "expiry_ticker.db"
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    val expiryItemRepository: ExpiryItemRepository =
        ExpiryItemRepositoryImpl(database.expiryItemDao())

    val appPreferences: AppPreferencesDataStore =
        AppPreferencesDataStore(context.applicationContext)

    val reminderScheduler: ReminderScheduler =
        ReminderScheduler(context.applicationContext)

    val renewalHistoryRepository: RenewalHistoryRepository =
        RenewalHistoryRepositoryImpl(database.renewalHistoryDao())

    val attachmentRepository: AttachmentRepository =
        AttachmentRepositoryImpl(database.attachmentDao())

    val attachmentStore = AttachmentStore(context.applicationContext)

    val backupManager = BackupManager(
        context.applicationContext, database, expiryItemRepository, renewalHistoryRepository,
        attachmentRepository, attachmentStore, reminderScheduler
    )

    val itemActions: ItemActions =
        ItemActions(expiryItemRepository, renewalHistoryRepository, reminderScheduler)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Plans every open item's reminders again from scratch. */
    suspend fun rescheduleAllReminders() {
        expiryItemRepository.getAll()
            .filterNot { it.isCompleted }
            .forEach(reminderScheduler::scheduleReminder)
    }

    init {
        // Deleting an item leaves its history and files behind so that Undo can bring them back;
        // by the next start the undo window is long gone, so tidy up what no item refers to.
        appScope.launch {
            runCatching {
                database.renewalHistoryDao().deleteOrphans()
                database.attachmentDao().deleteOrphans()
                attachmentStore.purgeUnreferenced(
                    database.attachmentDao().getAllFileNames().toSet()
                )
                // A cloud restore brings the database back without the attachment files.
                database.attachmentDao().getAll()
                    .filterNot { attachmentStore.file(it.fileName).exists() }
                    .forEach { database.attachmentDao().deleteById(it.id) }
            }
        }

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
                rescheduleAllReminders()
            }
        }
    }
}
