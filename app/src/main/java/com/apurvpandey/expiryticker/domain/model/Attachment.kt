package com.apurvpandey.expiryticker.domain.model

import java.time.Instant

/** A file (photo, PDF, ...) attached to an item; the bytes live in the app's private storage. */
data class Attachment(
    val id: Long = 0L,
    val itemId: Long,
    val displayName: String,
    val mimeType: String,
    /** Name of the file inside the app's attachments folder. */
    val fileName: String,
    val sizeBytes: Long,
    val createdAt: Instant = Instant.now()
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}
