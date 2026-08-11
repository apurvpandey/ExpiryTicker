package com.apurvpandey.expiryticker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.apurvpandey.expiryticker.data.local.dao.ExpiryItemDao
import com.apurvpandey.expiryticker.data.local.entity.ExpiryItemEntity

@Database(
    entities = [ExpiryItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ExpiryTickerDatabase : RoomDatabase() {
    abstract fun expiryItemDao(): ExpiryItemDao
}
