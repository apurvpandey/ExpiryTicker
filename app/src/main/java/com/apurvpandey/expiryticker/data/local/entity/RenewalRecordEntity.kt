package com.apurvpandey.expiryticker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// No foreign key on purpose: item inserts use REPLACE (which would cascade-delete children)
// and undoing a delete must bring the history back. Orphans are purged at app start instead.
@Entity(tableName = "renewal_history", indices = [Index("item_id")])
data class RenewalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "item_id") val itemId: Long,
    @ColumnInfo(name = "renewed_at") val renewedAt: Long,
    @ColumnInfo(name = "due_before") val dueBefore: String,
    @ColumnInfo(name = "due_after") val dueAfter: String?,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long?
)
