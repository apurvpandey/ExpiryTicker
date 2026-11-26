package com.apurvpandey.expiryticker.presentation.details

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.apurvpandey.expiryticker.core.util.LinkUtils
import com.apurvpandey.expiryticker.data.files.AttachmentStore
import com.apurvpandey.expiryticker.domain.model.Attachment
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import java.time.ZoneOffset

/** Hands work to other apps; each returns false when nothing could handle it. */
object ExternalActions {

    fun openUrl(context: Context, raw: String): Boolean {
        val url = LinkUtils.normalizeUrl(raw) ?: return false
        return start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun dial(context: Context, raw: String): Boolean {
        val number = LinkUtils.normalizePhone(raw) ?: return false
        return start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
    }

    /** Opens the calendar app with a prefilled all-day event on the due date. */
    fun addToCalendar(context: Context, item: ExpiryItem): Boolean {
        val dayStart = item.dueDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, "${item.title} (${item.category.displayName})")
            .putExtra(CalendarContract.Events.DESCRIPTION, item.notes)
            .putExtra(CalendarContract.Events.ALL_DAY, true)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dayStart)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dayStart + 24L * 60 * 60 * 1000)
        return start(context, intent)
    }

    fun openAttachment(context: Context, store: AttachmentStore, attachment: Attachment): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(store.uriFor(attachment), attachment.mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
