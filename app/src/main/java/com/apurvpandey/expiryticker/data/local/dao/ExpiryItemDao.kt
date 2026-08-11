package com.apurvpandey.expiryticker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.apurvpandey.expiryticker.data.local.entity.ExpiryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpiryItemDao {

    @Query("SELECT * FROM expiry_items ORDER BY due_date ASC")
    fun observeAll(): Flow<List<ExpiryItemEntity>>

    @Query("SELECT * FROM expiry_items WHERE id = :id")
    fun observeById(id: Long): Flow<ExpiryItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpiryItemEntity): Long

    @Update
    suspend fun update(entity: ExpiryItemEntity)

    @Delete
    suspend fun delete(entity: ExpiryItemEntity)

    @Query("DELETE FROM expiry_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
