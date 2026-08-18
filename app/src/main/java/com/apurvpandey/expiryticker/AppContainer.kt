package com.apurvpandey.expiryticker

import android.content.Context
import androidx.room.Room
import com.apurvpandey.expiryticker.data.local.database.ExpiryTickerDatabase
import com.apurvpandey.expiryticker.data.local.database.MIGRATION_1_2
import com.apurvpandey.expiryticker.data.preferences.AppPreferencesDataStore
import com.apurvpandey.expiryticker.data.repository.ExpiryItemRepositoryImpl
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.notification.ReminderScheduler

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
}
