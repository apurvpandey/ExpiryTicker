package com.apurvpandey.expiryticker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expiry_items")
data class ExpiryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val category: String,                       // RenewalCategory.name
    @ColumnInfo(name = "due_date")
    val dueDate: String,                        // ISO-8601 date "YYYY-MM-DD"
    @ColumnInfo(name = "reminder_days_before")
    val reminderDaysBefore: Int,
    val recurrence: String,                     // RecurrenceType.name
    val notes: String,
    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long?,                     // in paise; null means not set
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,                        // epoch millis
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,                        // epoch millis
    @ColumnInfo(name = "last_renewed_at")
    val lastRenewedAt: Long?                    // epoch millis; null if never renewed
)
