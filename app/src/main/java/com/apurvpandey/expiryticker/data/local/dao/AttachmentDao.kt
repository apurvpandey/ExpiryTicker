package com.apurvpandey.expiryticker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.apurvpandey.expiryticker.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert
    suspend fun insert(entity: AttachmentEntity): Long

    @Query("SELECT * FROM attachments WHERE item_id = :itemId ORDER BY created_at ASC")
    fun observeForItem(itemId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AttachmentEntity>)

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): AttachmentEntity?

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM attachments WHERE item_id NOT IN (SELECT id FROM expiry_items)")
    suspend fun getOrphans(): List<AttachmentEntity>

    @Query("SELECT file_name FROM attachments")
    suspend fun getAllFileNames(): List<String>

    @Query("DELETE FROM attachments WHERE item_id NOT IN (SELECT id FROM expiry_items)")
    suspend fun deleteOrphans()
}
