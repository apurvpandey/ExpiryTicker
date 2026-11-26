package com.apurvpandey.expiryticker.data.repository

import com.apurvpandey.expiryticker.data.local.dao.RenewalHistoryDao
import com.apurvpandey.expiryticker.data.local.entity.RenewalRecordEntity
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import com.apurvpandey.expiryticker.domain.repository.RenewalHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class RenewalHistoryRepositoryImpl(private val dao: RenewalHistoryDao) : RenewalHistoryRepository {
    override fun observeForItem(itemId: Long): Flow<List<RenewalRecord>> =
        dao.observeForItem(itemId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<RenewalRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<RenewalRecord> = dao.getAll().map { it.toDomain() }

    override suspend fun add(record: RenewalRecord): Long = dao.insert(record.toEntity())

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun addAll(records: List<RenewalRecord>) = dao.insertAll(records.map { it.toEntity() })
}

private fun RenewalRecordEntity.toDomain() = RenewalRecord(
    id = id,
    itemId = itemId,
    renewedAt = Instant.ofEpochMilli(renewedAt),
    dueBefore = LocalDate.parse(dueBefore),
    dueAfter = dueAfter?.let { LocalDate.parse(it) },
    amountPaise = amountPaise
)

private fun RenewalRecord.toEntity() = RenewalRecordEntity(
    id = id,
    itemId = itemId,
    renewedAt = renewedAt.toEpochMilli(),
    dueBefore = dueBefore.toString(),
    dueAfter = dueAfter?.toString(),
    amountPaise = amountPaise
)
