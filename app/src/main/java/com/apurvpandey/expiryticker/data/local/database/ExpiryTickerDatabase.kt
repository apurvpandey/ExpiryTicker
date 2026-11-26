package com.apurvpandey.expiryticker.data.local.database

import androidx.room.Database
import com.apurvpandey.expiryticker.data.local.dao.AttachmentDao
import com.apurvpandey.expiryticker.data.local.dao.RenewalHistoryDao
import com.apurvpandey.expiryticker.data.local.entity.AttachmentEntity
import com.apurvpandey.expiryticker.data.local.entity.RenewalRecordEntity
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.apurvpandey.expiryticker.data.local.dao.ExpiryItemDao
import com.apurvpandey.expiryticker.data.local.entity.ExpiryItemEntity

@Database(
    entities = [ExpiryItemEntity::class, RenewalRecordEntity::class, AttachmentEntity::class],
    version = 3,
    exportSchema = true
)
abstract class ExpiryTickerDatabase : RoomDatabase() {
    abstract fun expiryItemDao(): ExpiryItemDao
    abstract fun renewalHistoryDao(): RenewalHistoryDao
    abstract fun attachmentDao(): AttachmentDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // DOCUMENT was split into TRAVEL (passport/visa) and LICENCE (driving licence).
        // Remap existing DOCUMENT entries to TRAVEL as the closer semantic match.
        db.execSQL("UPDATE expiry_items SET category = 'TRAVEL' WHERE category = 'DOCUMENT'")
    }
}

/** Adds renewal link, phone and extra reminders to items, plus renewal history and attachments. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expiry_items ADD COLUMN renewal_url TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE expiry_items ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE expiry_items ADD COLUMN extra_reminders TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS renewal_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "item_id INTEGER NOT NULL, renewed_at INTEGER NOT NULL, " +
                "due_before TEXT NOT NULL, due_after TEXT, amount_paise INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_renewal_history_item_id ON renewal_history (item_id)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS attachments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "item_id INTEGER NOT NULL, display_name TEXT NOT NULL, mime_type TEXT NOT NULL, " +
                "file_name TEXT NOT NULL, size_bytes INTEGER NOT NULL, created_at INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_item_id ON attachments (item_id)")
    }
}
