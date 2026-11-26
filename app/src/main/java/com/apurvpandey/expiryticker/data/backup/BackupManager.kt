package com.apurvpandey.expiryticker.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.apurvpandey.expiryticker.data.files.AttachmentStore
import com.apurvpandey.expiryticker.data.local.database.ExpiryTickerDatabase
import com.apurvpandey.expiryticker.domain.repository.AttachmentRepository
import com.apurvpandey.expiryticker.domain.repository.ExpiryItemRepository
import com.apurvpandey.expiryticker.domain.repository.RenewalHistoryRepository
import com.apurvpandey.expiryticker.notification.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Writes and restores a single zip holding all items, history and attachment files. */
class BackupManager(
    private val context: Context,
    private val database: ExpiryTickerDatabase,
    private val items: ExpiryItemRepository,
    private val history: RenewalHistoryRepository,
    private val attachments: AttachmentRepository,
    private val store: AttachmentStore,
    private val scheduler: ReminderScheduler
) {
    data class RestoreResult(val items: Int, val attachments: Int, val skippedAttachments: Int)

    suspend fun export(out: OutputStream): Int = withContext(Dispatchers.IO) {
        val data = BackupData(items.getAll(), history.getAll(), attachments.getAll())
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(BackupCodec.encode(data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            data.attachments.forEach { a ->
                val file = store.file(a.fileName)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry("$FILES_PREFIX${a.fileName}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        data.items.size
    }

    /**
     * Replaces everything with the contents of the backup. Nothing is touched until the whole
     * backup has been read and validated; the database swap itself is one transaction.
     */
    suspend fun restore(input: InputStream): RestoreResult = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val json = unpack(input, temp)
            val data = BackupCodec.decode(json)

            val present = data.attachments.filter { File(temp, it.fileName).exists() }
            present.forEach { File(temp, it.fileName).copyTo(store.file(it.fileName), overwrite = true) }

            // Old reminders belong to old rows; they are planned again from the restored data.
            items.getAll().forEach { scheduler.cancelReminder(it.id) }
            database.withTransaction {
                attachments.deleteAll()
                history.deleteAll()
                items.deleteAll()
                items.addAll(data.items)
                history.addAll(data.history)
                attachments.addAll(present)
            }
            store.purgeUnreferenced(present.map { it.fileName }.toSet())
            data.items.filterNot { it.isCompleted }.forEach(scheduler::scheduleReminder)

            RestoreResult(data.items.size, present.size, data.attachments.size - present.size)
        } finally {
            temp.deleteRecursively()
        }
    }

    /** Extracts backup.json (returned) and the attachment files (into [dir]), enforcing size limits. */
    private fun unpack(input: InputStream, dir: File): String {
        var json: String? = null
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                when {
                    entry.name == JSON_ENTRY -> json = String(readLimited(zip, MAX_JSON_BYTES), Charsets.UTF_8)
                    entry.name.startsWith(FILES_PREFIX) -> {
                        val name = entry.name.removePrefix(FILES_PREFIX)
                        // Only plain file names are extracted; anything else could escape the folder.
                        if (!BackupCodec.isSafeFileName(name)) continue
                        val bytes = readLimited(zip, AttachmentStore.MAX_BYTES)
                        total += bytes.size
                        if (total > MAX_TOTAL_BYTES) throw BackupFormatException(BackupError.TOO_LARGE, "The backup is too large")
                        File(dir, name).writeBytes(bytes)
                    }
                    // Unknown entries are ignored, never extracted.
                }
            }
        }
        return json ?: throw BackupFormatException(BackupError.MISSING_DATA, "The backup file is missing its data")
    }

    private fun readLimited(stream: InputStream, limit: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var read = 0L
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) break
            read += n
            if (read > limit) throw BackupFormatException(BackupError.TOO_LARGE, "The backup contains a file that is too large")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    companion object {
        const val JSON_ENTRY = "backup.json"
        const val FILES_PREFIX = "files/"
        private const val MAX_JSON_BYTES = 20L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 300L * 1024 * 1024
    }
}
