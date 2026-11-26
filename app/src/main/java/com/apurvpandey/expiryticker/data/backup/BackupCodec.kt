package com.apurvpandey.expiryticker.data.backup

import com.apurvpandey.expiryticker.domain.model.Attachment
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class BackupData(
    val items: List<ExpiryItem>,
    val history: List<RenewalRecord>,
    val attachments: List<Attachment>
)

/** Why a backup was refused; the UI maps each reason to a message in the user's language. */
enum class BackupError {
    NOT_A_BACKUP, UNREADABLE_VERSION, NEWER_VERSION, DUPLICATE_ITEMS, DAMAGED, INVALID_DATE,
    ITEM_WITHOUT_TITLE, TOO_LARGE, MISSING_DATA
}

class BackupFormatException(
    val error: BackupError,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/** Turns the app's data into a versioned JSON document and back, validating what it reads. */
object BackupCodec {
    const val FORMAT = "expiryticker-backup"
    const val VERSION = 1

    fun encode(data: BackupData, exportedAt: Instant = Instant.now()): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt.toEpochMilli())
            .put("items", JSONArray(data.items.map(::itemJson)))
            .put("history", JSONArray(data.history.map(::historyJson)))
            .put("attachments", JSONArray(data.attachments.map(::attachmentJson)))
        return root.toString()
    }

    /** Throws [BackupFormatException] with a user-readable message when the document cannot be trusted. */
    fun decode(json: String): BackupData {
        try {
            val root = JSONObject(json)
            if (root.optString("format") != FORMAT) throw BackupFormatException(BackupError.NOT_A_BACKUP, "This is not an ExpiryTicker backup")
            val version = root.optInt("version", -1)
            if (version < 1) throw BackupFormatException(BackupError.UNREADABLE_VERSION, "Unreadable backup version")
            if (version > VERSION) throw BackupFormatException(BackupError.NEWER_VERSION, "This backup was made by a newer version of the app")

            val items = root.getJSONArray("items").objects().map(::itemFrom)
            val ids = items.map { it.id }.toSet()
            if (ids.size != items.size) throw BackupFormatException(BackupError.DUPLICATE_ITEMS, "The backup contains duplicate items")
            // Anything pointing at a missing item is dropped rather than failing the whole restore.
            val history = root.optJSONArray("history")?.objects()?.map(::historyFrom).orEmpty().filter { it.itemId in ids }
            val attachments = root.optJSONArray("attachments")?.objects()?.map(::attachmentFrom).orEmpty()
                .filter { it.itemId in ids && isSafeFileName(it.fileName) }
            return BackupData(items, history, attachments)
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: JSONException) {
            throw BackupFormatException(BackupError.DAMAGED, "The backup file is damaged", e)
        } catch (e: DateTimeParseException) {
            throw BackupFormatException(BackupError.INVALID_DATE, "The backup contains an invalid date", e)
        }
    }

    /** Attachment file names become paths on disk, so refuse anything that could escape the folder. */
    fun isSafeFileName(name: String): Boolean =
        name.isNotBlank() && name.length <= 128 && '/' !in name && '\\' !in name && !name.startsWith(".")

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)

    private fun itemJson(i: ExpiryItem) = JSONObject()
        .put("id", i.id)
        .put("title", i.title)
        .put("category", i.category.name)
        .put("dueDate", i.dueDate.toString())
        .put("reminderDaysBefore", i.reminderDaysBefore)
        .put("recurrence", i.recurrence.name)
        .put("notes", i.notes)
        .put("amountPaise", i.amountPaise ?: JSONObject.NULL)
        .put("isCompleted", i.isCompleted)
        .put("createdAt", i.createdAt.toEpochMilli())
        .put("updatedAt", i.updatedAt.toEpochMilli())
        .put("lastRenewedAt", i.lastRenewedAt?.toEpochMilli() ?: JSONObject.NULL)
        .put("renewalUrl", i.renewalUrl)
        .put("phone", i.phone)
        .put("extraReminderDays", JSONArray(i.extraReminderDays))

    private fun itemFrom(o: JSONObject): ExpiryItem {
        val title = o.getString("title").trim()
        if (title.isEmpty()) throw BackupFormatException(BackupError.ITEM_WITHOUT_TITLE, "The backup contains an item without a title")
        return ExpiryItem(
            id = o.getLong("id"),
            title = title,
            category = runCatching { RenewalCategory.valueOf(o.getString("category")) }.getOrDefault(RenewalCategory.OTHER),
            dueDate = LocalDate.parse(o.getString("dueDate")),
            reminderDaysBefore = o.optInt("reminderDaysBefore", 7).coerceIn(0, 365),
            recurrence = runCatching { RecurrenceType.valueOf(o.getString("recurrence")) }.getOrDefault(RecurrenceType.NONE),
            notes = o.optString("notes", ""),
            amountPaise = o.longOrNull("amountPaise")?.takeIf { it >= 0 },
            isCompleted = o.optBoolean("isCompleted", false),
            createdAt = Instant.ofEpochMilli(o.optLong("createdAt", 0L)),
            updatedAt = Instant.ofEpochMilli(o.optLong("updatedAt", 0L)),
            lastRenewedAt = o.longOrNull("lastRenewedAt")?.let(Instant::ofEpochMilli),
            renewalUrl = o.optString("renewalUrl", ""),
            phone = o.optString("phone", ""),
            extraReminderDays = o.optJSONArray("extraReminderDays")
                ?.let { a -> (0 until a.length()).map { a.getInt(it) } }.orEmpty().filter { it in 0..365 }
        )
    }

    private fun historyJson(r: RenewalRecord) = JSONObject()
        .put("id", r.id)
        .put("itemId", r.itemId)
        .put("renewedAt", r.renewedAt.toEpochMilli())
        .put("dueBefore", r.dueBefore.toString())
        .put("dueAfter", r.dueAfter?.toString() ?: JSONObject.NULL)
        .put("amountPaise", r.amountPaise ?: JSONObject.NULL)

    private fun historyFrom(o: JSONObject) = RenewalRecord(
        id = o.getLong("id"),
        itemId = o.getLong("itemId"),
        renewedAt = Instant.ofEpochMilli(o.getLong("renewedAt")),
        dueBefore = LocalDate.parse(o.getString("dueBefore")),
        dueAfter = if (o.isNull("dueAfter")) null else LocalDate.parse(o.getString("dueAfter")),
        amountPaise = o.longOrNull("amountPaise")
    )

    private fun attachmentJson(a: Attachment) = JSONObject()
        .put("id", a.id)
        .put("itemId", a.itemId)
        .put("displayName", a.displayName)
        .put("mimeType", a.mimeType)
        .put("fileName", a.fileName)
        .put("sizeBytes", a.sizeBytes)
        .put("createdAt", a.createdAt.toEpochMilli())

    private fun attachmentFrom(o: JSONObject) = Attachment(
        id = o.getLong("id"),
        itemId = o.getLong("itemId"),
        displayName = o.optString("displayName", "attachment"),
        mimeType = o.optString("mimeType", "application/octet-stream"),
        fileName = o.getString("fileName"),
        sizeBytes = o.optLong("sizeBytes", 0L),
        createdAt = Instant.ofEpochMilli(o.optLong("createdAt", 0L))
    )
}
