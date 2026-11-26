package com.apurvpandey.expiryticker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.apurvpandey.expiryticker.data.local.entity.RenewalRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RenewalHistoryDao {
    @Insert
    suspend fun insert(entity: RenewalRecordEntity): Long

    @Query("SELECT * FROM renewal_history WHERE item_id = :itemId ORDER BY renewed_at DESC")
    fun observeForItem(itemId: Long): Flow<List<RenewalRecordEntity>>

    @Query("SELECT * FROM renewal_history ORDER BY renewed_at DESC")
    fun observeAll(): Flow<List<RenewalRecordEntity>>

    @Query("SELECT * FROM renewal_history")
    suspend fun getAll(): List<RenewalRecordEntity>

    @Query("DELETE FROM renewal_history")
    suspend fun deleteAll()

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RenewalRecordEntity>)

    @Query("DELETE FROM renewal_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM renewal_history WHERE item_id NOT IN (SELECT id FROM expiry_items)")
    suspend fun deleteOrphans()
}
