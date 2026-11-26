package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.data.backup.BackupCodec
import com.apurvpandey.expiryticker.data.backup.BackupData
import com.apurvpandey.expiryticker.data.backup.BackupFormatException
import com.apurvpandey.expiryticker.domain.model.Attachment
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.domain.model.RenewalRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackupCodecTest {
    private val item = ExpiryItem(
        id = 7, title = "Car insurance", category = RenewalCategory.INSURANCE,
        dueDate = LocalDate.of(2027, 3, 1), reminderDaysBefore = 14, recurrence = RecurrenceType.YEARLY,
        notes = "line1\nline2 \"quoted\"", amountPaise = 1_450_050, isCompleted = false,
        createdAt = Instant.ofEpochMilli(1_000), updatedAt = Instant.ofEpochMilli(2_000),
        lastRenewedAt = Instant.ofEpochMilli(3_000), renewalUrl = "insurer.com", phone = "+911234567890",
        extraReminderDays = listOf(30, 3)
    )
    private val record = RenewalRecord(
        id = 1, itemId = 7, renewedAt = Instant.ofEpochMilli(5_000),
        dueBefore = LocalDate.of(2026, 3, 1), dueAfter = LocalDate.of(2027, 3, 1), amountPaise = 1_300_000
    )
    private val attachment = Attachment(
        id = 2, itemId = 7, displayName = "policy.pdf", mimeType = "application/pdf",
        fileName = "abc.pdf", sizeBytes = 123, createdAt = Instant.ofEpochMilli(6_000)
    )
    private val data = BackupData(listOf(item), listOf(record), listOf(attachment))

    private fun failure(json: String): String =
        try { BackupCodec.decode(json); fail("expected failure"); "" } catch (e: BackupFormatException) { e.message.orEmpty() }

    @Test fun roundTripKeepsEverything() {
        val back = BackupCodec.decode(BackupCodec.encode(data))
        assertEquals(data, back)
    }

    @Test fun nullableFieldsRoundTrip() {
        val bare = item.copy(amountPaise = null, lastRenewedAt = null, extraReminderDays = emptyList())
        val bareRecord = record.copy(dueAfter = null, amountPaise = null)
        val back = BackupCodec.decode(BackupCodec.encode(BackupData(listOf(bare), listOf(bareRecord), emptyList())))
        assertEquals(bare, back.items.single())
        assertEquals(bareRecord, back.history.single())
    }

    @Test fun rejectsForeignJson() {
        assertTrue(failure("""{"hello":"world"}""").contains("not an ExpiryTicker backup"))
    }

    @Test fun rejectsGarbage() {
        assertTrue(failure("not json at all").contains("damaged"))
    }

    @Test fun rejectsNewerVersions() {
        val newer = BackupCodec.encode(data).replace("\"version\":1", "\"version\":99")
        assertTrue(failure(newer).contains("newer version"))
    }

    @Test fun rejectsInvalidDates() {
        val bad = BackupCodec.encode(data).replace("2027-03-01", "2027-13-45")
        assertTrue(failure(bad).contains("invalid date"))
    }

    @Test fun rejectsDuplicateItemIds() {
        val dup = BackupCodec.encode(BackupData(listOf(item, item), emptyList(), emptyList()))
        assertTrue(failure(dup).contains("duplicate"))
    }

    @Test fun unknownCategoryAndRecurrenceFallBack() {
        val json = BackupCodec.encode(data).replace("INSURANCE", "FUTURE_CATEGORY").replace("YEARLY", "EVERY_DECADE")
        val restored = BackupCodec.decode(json).items.single()
        assertEquals(RenewalCategory.OTHER, restored.category)
        assertEquals(RecurrenceType.NONE, restored.recurrence)
    }

    @Test fun orphanedHistoryAndAttachmentsAreDropped() {
        val orphans = BackupData(listOf(item), listOf(record.copy(itemId = 99)), listOf(attachment.copy(itemId = 99)))
        val back = BackupCodec.decode(BackupCodec.encode(orphans))
        assertTrue(back.history.isEmpty())
        assertTrue(back.attachments.isEmpty())
    }

    @Test fun unsafeAttachmentFileNamesAreDropped() {
        val evil = BackupData(
            listOf(item), emptyList(),
            listOf(attachment.copy(id = 1, fileName = "../../databases/expiry_ticker.db"), attachment.copy(id = 2, fileName = "ok.pdf"))
        )
        assertEquals(listOf("ok.pdf"), BackupCodec.decode(BackupCodec.encode(evil)).attachments.map { it.fileName })
    }

    @Test fun fileNameSafetyRules() {
        assertTrue(BackupCodec.isSafeFileName("5f2c.pdf"))
        assertFalse(BackupCodec.isSafeFileName(""))
        assertFalse(BackupCodec.isSafeFileName("a/b.pdf"))
        assertFalse(BackupCodec.isSafeFileName("a\\b.pdf"))
        assertFalse(BackupCodec.isSafeFileName(".hidden"))
        assertFalse(BackupCodec.isSafeFileName("x".repeat(200)))
    }

    @Test fun outOfRangeReminderOffsetsAreClamped() {
        val json = BackupCodec.encode(data).replace("\"reminderDaysBefore\":14", "\"reminderDaysBefore\":99999")
        assertEquals(365, BackupCodec.decode(json).items.single().reminderDaysBefore)
    }

    @Test fun titlesMustNotBeBlank() {
        val json = BackupCodec.encode(data).replace("Car insurance", "   ")
        assertTrue(failure(json).contains("without a title"))
    }
}
