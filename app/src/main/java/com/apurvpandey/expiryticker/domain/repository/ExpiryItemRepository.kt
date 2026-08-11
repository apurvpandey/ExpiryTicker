package com.apurvpandey.expiryticker.domain.repository

import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import kotlinx.coroutines.flow.Flow

interface ExpiryItemRepository {
    fun observeAll(): Flow<List<ExpiryItem>>
    fun observeById(id: Long): Flow<ExpiryItem?>
    suspend fun add(item: ExpiryItem): Long
    suspend fun update(item: ExpiryItem)
    suspend fun delete(item: ExpiryItem)
    suspend fun deleteById(id: Long)
}
