package com.apurvpandey.expiryticker.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.currentState
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import com.apurvpandey.expiryticker.ExpiryTickerApplication
import com.apurvpandey.expiryticker.MainActivity
import com.apurvpandey.expiryticker.domain.model.ExpiryStatus
import com.apurvpandey.expiryticker.core.util.ConfirmTap
import com.apurvpandey.expiryticker.core.util.WidgetRow
import com.apurvpandey.expiryticker.core.util.WidgetSnapshot
import com.apurvpandey.expiryticker.core.util.WidgetSnapshots
import com.apurvpandey.expiryticker.R
import com.apurvpandey.expiryticker.presentation.common.labelRes
import com.apurvpandey.expiryticker.presentation.components.countdown
import com.apurvpandey.expiryticker.presentation.components.shortLabel
import com.apurvpandey.expiryticker.presentation.theme.DarkColorScheme
import com.apurvpandey.expiryticker.presentation.theme.DarkExpiryColors
import com.apurvpandey.expiryticker.presentation.theme.LightColorScheme
import com.apurvpandey.expiryticker.presentation.theme.LightExpiryColors
import com.apurvpandey.expiryticker.presentation.theme.urgency
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val SMALL = DpSize(110.dp, 110.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 250.dp)

private val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

/** Home-screen widget listing the most urgent items; adapts to its size. */
class ExpiryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as ExpiryTickerApplication).container
        val items = container.expiryItemRepository.observeAll().first()
        val snapshot = WidgetSnapshots.build(items, LocalDate.now())
        provideContent {
            val state = currentState<Preferences>()
            // Only an unexpired arm counts, so a stale "Done?" never lingers in a re-render.
            val armedId = state[ArmedItemKey]?.takeIf {
                ConfirmTap.isArmed(it, state[ArmedAtKey], System.currentTimeMillis(), it)
            }
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(context, snapshot, LocalSize.current, armedId)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, snapshot: WidgetSnapshot, size: DpSize, armedId: Long?) {
    val openApp = launch(context)
    val compact = size.width < 180.dp
    val maxRows = when {
        size.height >= 230.dp -> 5
        size.height >= 150.dp -> 3
        else -> 2
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(openApp)
    ) {
        Header(context, snapshot, compact)
        Spacer(GlanceModifier.height(8.dp))
        when {
            snapshot.rows.isEmpty() -> AllClear()
            compact && size.height < 150.dp -> NextUp(context, snapshot.rows.first(), armedId)
            // Glance allows at most 10 children per Column: no spacer elements between rows,
            // spacing comes from each row's own bottom padding.
            else -> snapshot.rows.take(if (compact) maxRows.coerceAtMost(4) else maxRows).forEach { row ->
                ItemRow(row, context, compact, armedId)
            }
        }
    }
}

@Composable
private fun Header(context: Context, snapshot: WidgetSnapshot, compact: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(if (compact) R.string.widget_due_soon else R.string.widget_title),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        if (snapshot.overdueCount > 0) {
            Text(
                text = context.getString(R.string.widget_overdue_count, snapshot.overdueCount),
                style = TextStyle(
                    color = accent(snapshot.rows.firstOrNull()?.status ?: ExpiryStatus.Overdue(1)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        if (!compact) {
            Spacer(GlanceModifier.width(10.dp))
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clickable(launch(context, openAdd = true)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun NextUp(context: Context, row: WidgetRow, armedId: Long?) {
    val c = row.status.countdown(LocalContext.current.resources)
    Column(modifier = GlanceModifier.fillMaxWidth().clickable(launch(context, row.id))) {
        Text(
            text = c.value,
            style = TextStyle(color = accent(row.status), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = c.caption,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = row.title,
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(8.dp))
        DoneButton(row, armed = armedId == row.id)
    }
}

@Composable
private fun ItemRow(row: WidgetRow, context: Context, compact: Boolean, armedId: Long?) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(launch(context, row.id)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(32.dp)
                .background(accent(row.status))
                .cornerRadius(2.dp)
        ) {}
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = row.title,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            // In a narrow widget the countdown moves under the title instead of beside it.
            Text(
                text = if (compact) row.status.shortLabel(LocalContext.current.resources) else LocalContext.current.getString(row.category.labelRes()),
                maxLines = 1,
                style = TextStyle(
                    color = if (compact) accent(row.status) else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (compact) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
        if (!compact) {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = row.status.shortLabel(LocalContext.current.resources),
                style = TextStyle(color = accent(row.status), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        DoneButton(row, armed = armedId == row.id)
    }
}

/** Check button; after the first tap it grows into a "Done?" pill that waits for confirmation. */
@Composable
private fun DoneButton(row: WidgetRow, armed: Boolean) {
    val onTap = actionRunCallback<MarkDoneAction>(actionParametersOf(ItemIdParam to row.id))
    Box(
        modifier = GlanceModifier
            .height(32.dp)
            .then(if (armed) GlanceModifier.padding(horizontal = 12.dp) else GlanceModifier.width(32.dp))
            .background(if (armed) GlanceTheme.colors.primary else GlanceTheme.colors.primaryContainer)
            .cornerRadius(16.dp)
            .clickable(onTap),
        contentAlignment = Alignment.Center
    ) {
        if (armed) {
            Text(
                text = LocalContext.current.getString(R.string.widget_confirm),
                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_check),
                contentDescription = LocalContext.current.getString(R.string.widget_mark_done_desc, row.title),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                modifier = GlanceModifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AllClear() {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = LocalContext.current.getString(R.string.all_clear),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = LocalContext.current.getString(R.string.widget_all_clear_sub),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
        )
    }
}

private fun accent(status: ExpiryStatus): ColorProvider =
    androidx.glance.color.ColorProvider(
        day = status.urgency(LightExpiryColors).accent,
        night = status.urgency(DarkExpiryColors).accent
    )

/** Opens the app; with [itemId] on that item, with [openAdd] on the add screen. */
private fun launch(context: Context, itemId: Long? = null, openAdd: Boolean = false): Action {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        // A distinct data URI keeps each target's PendingIntent separate.
        data = Uri.parse("expiryticker://widget/${itemId ?: if (openAdd) "add" else "home"}")
        itemId?.let { putExtra(MainActivity.EXTRA_ITEM_ID, it) }
        if (openAdd) putExtra(MainActivity.EXTRA_OPEN_ADD, true)
    }
    return actionStartActivity(intent)
}
