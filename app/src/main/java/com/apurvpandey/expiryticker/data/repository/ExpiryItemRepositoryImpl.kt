package com.apurvpandey.expiryticker.data.repository

import com.apurvpandey.expiryticker.data.local.dao.ExpiryItemDao
import com.apurvpandey.expiryticker.data.local.entity.ExpiryItemEntity
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class ExpiryItemRepositoryImpl(private val dao: ExpiryItemDao) : ExpiryItemRepository {

    override fun observeAll(): Flow<List<ExpiryItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<ExpiryItem?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun add(item: ExpiryItem): Long = dao.insert(item.toEntity())

    override suspend fun update(item: ExpiryItem) = dao.update(item.toEntity())

    override suspend fun delete(item: ExpiryItem) = dao.delete(item.toEntity())

    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}

private fun ExpiryItemEntity.toDomain(): ExpiryItem = ExpiryItem(
    id = id,
    title = title,
    category = RenewalCategory.valueOf(category),
    dueDate = LocalDate.parse(dueDate),
    reminderDaysBefore = reminderDaysBefore,
    recurrence = RecurrenceType.valueOf(recurrence),
    notes = notes,
    amountPaise = amountPaise,
    isCompleted = isCompleted,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    lastRenewedAt = lastRenewedAt?.let { Instant.ofEpochMilli(it) }
)

private fun ExpiryItem.toEntity(): ExpiryItemEntity = ExpiryItemEntity(
    id = id,
    title = title,
    category = category.name,
    dueDate = dueDate.toString(),
    reminderDaysBefore = reminderDaysBefore,
    recurrence = recurrence.name,
    notes = notes,
    amountPaise = amountPaise,
    isCompleted = isCompleted,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    lastRenewedAt = lastRenewedAt?.toEpochMilli()
)
