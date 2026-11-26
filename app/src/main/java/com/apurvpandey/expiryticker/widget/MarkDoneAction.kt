package com.apurvpandey.expiryticker.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.apurvpandey.expiryticker.ExpiryTickerApplication
import com.apurvpandey.expiryticker.core.util.ConfirmTap
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

internal val ArmedItemKey = longPreferencesKey("armed_item_id")
internal val ArmedAtKey = longPreferencesKey("armed_at_ms")
internal val ItemIdParam = ActionParameters.Key<Long>("item_id")

/** Done button on a widget row: the first tap arms it, a second tap shortly after completes the item. */
class MarkDoneAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ItemIdParam] ?: return
        val widget = ExpiryWidget()
        val state = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)

        val decision = ConfirmTap.decide(state[ArmedItemKey], state[ArmedAtKey], System.currentTimeMillis(), itemId)
        when (decision) {
            ConfirmTap.Decision.ARM -> {
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[ArmedItemKey] = itemId
                    prefs[ArmedAtKey] = System.currentTimeMillis()
                }
                widget.update(context, glanceId)
                // Reset the button visually once the confirmation window has passed.
                WorkManager.getInstance(context).enqueueUniqueWork(
                    DisarmWorker.UNIQUE_NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DisarmWorker>()
                        .setInitialDelay(ConfirmTap.ARM_TIMEOUT_MS + 250, TimeUnit.MILLISECONDS)
                        .build()
                )
            }
            ConfirmTap.Decision.CONFIRM -> {
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs.remove(ArmedItemKey)
                    prefs.remove(ArmedAtKey)
                }
                val container = (context.applicationContext as ExpiryTickerApplication).container
                container.expiryItemRepository.observeById(itemId).first()
                    ?.let { container.itemActions.markDone(it) }
                // The data observer also refreshes widgets; updating here is just faster.
                widget.update(context, glanceId)
            }
        }
    }
}
