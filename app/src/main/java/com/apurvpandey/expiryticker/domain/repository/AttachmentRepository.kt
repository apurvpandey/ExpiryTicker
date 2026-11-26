package com.apurvpandey.expiryticker.domain.repository

import com.apurvpandey.expiryticker.domain.model.Attachment
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun observeForItem(itemId: Long): Flow<List<Attachment>>
    suspend fun getById(id: Long): Attachment?
    suspend fun add(attachment: Attachment): Long
    suspend fun deleteById(id: Long)
}
