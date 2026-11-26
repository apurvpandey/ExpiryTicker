package com.apurvpandey.expiryticker.data.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.apurvpandey.expiryticker.domain.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Keeps attachment bytes in the app's private storage and hands them out through a FileProvider. */
class AttachmentStore(private val context: Context) {

    private val dir: File get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    data class Stored(val displayName: String, val mimeType: String, val fileName: String, val sizeBytes: Long)

    /** Copies the content behind [uri] into private storage. Throws if it is too big or unreadable. */
    suspend fun import(uri: Uri): Stored = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        var displayName = "attachment"
        var declaredSize = -1L
        resolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { displayName = c.getString(it) ?: displayName }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { declaredSize = c.getLong(it) }
            }
        }
        require(declaredSize < MAX_BYTES) { "File is larger than ${MAX_BYTES / (1024 * 1024)} MB" }

        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: displayName.substringAfterLast('.', "bin")
        val fileName = "${UUID.randomUUID()}.$ext"
        val target = File(dir, fileName)
        var copied = 0L
        try {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { out ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        copied += n
                        require(copied <= MAX_BYTES) { "File is larger than ${MAX_BYTES / (1024 * 1024)} MB" }
                        out.write(buffer, 0, n)
                    }
                }
            } ?: error("Could not open file")
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        Stored(displayName, mime, fileName, copied)
    }

    /** A new, empty file for the camera to write into; returns its name and a shareable Uri. */
    fun newPhotoTarget(): Pair<String, Uri> {
        val name = "${UUID.randomUUID()}.jpg"
        return name to uriFor(File(dir, name))
    }

    fun file(fileName: String): File = File(dir, fileName)

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun uriFor(attachment: Attachment): Uri = uriFor(file(attachment.fileName))

    fun delete(fileName: String) {
        file(fileName).delete()
    }

    /** Removes files that no attachment row refers to any more. */
    fun purgeUnreferenced(referenced: Set<String>) {
        dir.listFiles()?.filter { it.name !in referenced }?.forEach { it.delete() }
    }

    companion object {
        const val DIR_NAME = "attachments"
        const val MAX_BYTES = 15L * 1024 * 1024
    }
}
