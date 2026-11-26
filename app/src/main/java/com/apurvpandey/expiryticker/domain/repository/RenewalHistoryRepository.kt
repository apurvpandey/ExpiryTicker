package com.apurvpandey.expiryticker.domain.repository

import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import kotlinx.coroutines.flow.Flow

interface RenewalHistoryRepository {
    fun observeForItem(itemId: Long): Flow<List<RenewalRecord>>
    fun observeAll(): Flow<List<RenewalRecord>>
    suspend fun getAll(): List<RenewalRecord>
    suspend fun add(record: RenewalRecord): Long
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
    suspend fun addAll(records: List<RenewalRecord>)
}
