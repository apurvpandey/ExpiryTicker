package com.apurvpandey.expiryticker.data.repository

import com.apurvpandey.expiryticker.data.local.dao.AttachmentDao
import com.apurvpandey.expiryticker.data.local.entity.AttachmentEntity
import com.apurvpandey.expiryticker.domain.model.Attachment
import com.apurvpandey.expiryticker.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class AttachmentRepositoryImpl(private val dao: AttachmentDao) : AttachmentRepository {
    override fun observeForItem(itemId: Long): Flow<List<Attachment>> =
        dao.observeForItem(itemId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Attachment? = dao.getById(id)?.toDomain()

    override suspend fun add(attachment: Attachment): Long = dao.insert(attachment.toEntity())

    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}

internal fun AttachmentEntity.toDomain() = Attachment(
    id = id, itemId = itemId, displayName = displayName, mimeType = mimeType,
    fileName = fileName, sizeBytes = sizeBytes, createdAt = Instant.ofEpochMilli(createdAt)
)

private fun Attachment.toEntity() = AttachmentEntity(
    id = id, itemId = itemId, displayName = displayName, mimeType = mimeType,
    fileName = fileName, sizeBytes = sizeBytes, createdAt = createdAt.toEpochMilli()
)
