package com.apurvpandey.expiryticker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.apurvpandey.expiryticker.data.local.dao.ExpiryItemDao
import com.apurvpandey.expiryticker.data.local.entity.ExpiryItemEntity

@Database(
    entities = [ExpiryItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ExpiryTickerDatabase : RoomDatabase() {
    abstract fun expiryItemDao(): ExpiryItemDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // DOCUMENT was split into TRAVEL (passport/visa) and LICENCE (driving licence).
        // Remap existing DOCUMENT entries to TRAVEL as the closer semantic match.
        db.execSQL("UPDATE expiry_items SET category = 'TRAVEL' WHERE category = 'DOCUMENT'")
    }
}
