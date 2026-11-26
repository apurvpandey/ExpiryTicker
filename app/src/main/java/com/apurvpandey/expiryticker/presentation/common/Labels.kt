package com.apurvpandey.expiryticker.presentation.common

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.apurvpandey.expiryticker.R
import com.apurvpandey.expiryticker.data.backup.BackupError
import com.apurvpandey.expiryticker.core.util.DueBucket
import com.apurvpandey.expiryticker.domain.model.ExpiryItem
import com.apurvpandey.expiryticker.domain.model.RecurrenceType
import com.apurvpandey.expiryticker.domain.model.RenewalCategory
import com.apurvpandey.expiryticker.core.util.ReminderText
import com.apurvpandey.expiryticker.presentation.dashboard.DashboardFilter
import com.apurvpandey.expiryticker.presentation.dashboard.DashboardSort

// Display names live in string resources so they follow the user's language. The English
// `displayName` properties on the domain enums are kept for data files (CSV) and search.

@StringRes
fun RenewalCategory.labelRes(): Int = when (this) {
    RenewalCategory.VEHICLE -> R.string.cat_vehicle
    RenewalCategory.INSURANCE -> R.string.cat_insurance
    RenewalCategory.TRAVEL -> R.string.cat_travel
    RenewalCategory.LICENCE -> R.string.cat_licence
    RenewalCategory.WARRANTY -> R.string.cat_warranty
    RenewalCategory.SUBSCRIPTION -> R.string.cat_subscription
    RenewalCategory.DOMAIN -> R.string.cat_domain
    RenewalCategory.CERTIFICATION -> R.string.cat_certification
    RenewalCategory.MEMBERSHIP -> R.string.cat_membership
    RenewalCategory.AMC -> R.string.cat_amc
    RenewalCategory.FINANCE -> R.string.cat_finance
    RenewalCategory.HEALTH -> R.string.cat_health
    RenewalCategory.HOME -> R.string.cat_home
    RenewalCategory.OTHER -> R.string.cat_other
}

@StringRes
fun RecurrenceType.labelRes(): Int = when (this) {
    RecurrenceType.NONE -> R.string.rec_none
    RecurrenceType.MONTHLY -> R.string.rec_monthly
    RecurrenceType.QUARTERLY -> R.string.rec_quarterly
    RecurrenceType.HALF_YEARLY -> R.string.rec_half_yearly
    RecurrenceType.YEARLY -> R.string.rec_yearly
}

@StringRes
fun DashboardFilter.labelRes(): Int = when (this) {
    DashboardFilter.ALL -> R.string.filter_all
    DashboardFilter.OVERDUE -> R.string.overdue
    DashboardFilter.UPCOMING -> R.string.filter_upcoming
    DashboardFilter.COMPLETED -> R.string.completed
}

@StringRes
fun DashboardSort.labelRes(): Int = when (this) {
    DashboardSort.DUE_DATE -> R.string.sort_due_date
    DashboardSort.TITLE -> R.string.sort_name
    DashboardSort.AMOUNT -> R.string.sort_amount
    DashboardSort.RECENT -> R.string.sort_recent
}

@StringRes
fun DueBucket.labelRes(): Int = when (this) {
    DueBucket.OVERDUE -> R.string.bucket_overdue
    DueBucket.TODAY -> R.string.bucket_today
    DueBucket.THIS_WEEK -> R.string.bucket_week
    DueBucket.THIS_MONTH -> R.string.bucket_month
    DueBucket.LATER -> R.string.bucket_later
}

@Composable
fun RenewalCategory.label(): String = stringResource(labelRes())

@Composable
fun RecurrenceType.label(): String = stringResource(labelRes())

/** "On the day", "1 day before", "14 days before". */
fun reminderLabel(res: Resources, daysBefore: Int): String =
    if (daysBefore == 0) res.getString(R.string.on_the_day)
    else res.getQuantityString(R.plurals.days_before, daysBefore, daysBefore)

/** Short chip form: "On date", "1 day", "14 days". */
fun reminderChipLabel(res: Resources, daysBefore: Int): String =
    if (daysBefore == 0) res.getString(R.string.on_date)
    else res.getQuantityString(R.plurals.days_short, daysBefore, daysBefore)

fun reminderSummary(res: Resources, item: ExpiryItem): String =
    ReminderText.allOffsets(item).joinToString(", ") { reminderLabel(res, it) }

@Composable
fun resources(): Resources = LocalContext.current.resources

@StringRes
fun BackupError.messageRes(): Int = when (this) {
    BackupError.NOT_A_BACKUP -> R.string.backup_err_not_backup
    BackupError.UNREADABLE_VERSION -> R.string.backup_err_version
    BackupError.NEWER_VERSION -> R.string.backup_err_newer
    BackupError.DUPLICATE_ITEMS -> R.string.backup_err_duplicates
    BackupError.DAMAGED -> R.string.backup_err_damaged
    BackupError.INVALID_DATE -> R.string.backup_err_date
    BackupError.ITEM_WITHOUT_TITLE -> R.string.backup_err_no_title
    BackupError.TOO_LARGE -> R.string.backup_err_too_large
    BackupError.MISSING_DATA -> R.string.backup_err_missing_data
}
