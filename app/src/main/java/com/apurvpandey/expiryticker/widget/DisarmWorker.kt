package com.apurvpandey.expiryticker.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apurvpandey.expiryticker.core.util.ConfirmTap

/** Clears any expired "Done?" buttons so the widget goes back to its resting look. */
class DisarmWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val widget = ExpiryWidget()
        GlanceAppWidgetManager(applicationContext).getGlanceIds(ExpiryWidget::class.java).forEach { id ->
            val state = getAppWidgetState<Preferences>(applicationContext, PreferencesGlanceStateDefinition, id)
            val armedAt = state[ArmedAtKey]
            if (state[ArmedItemKey] != null &&
                (armedAt == null || System.currentTimeMillis() - armedAt > ConfirmTap.ARM_TIMEOUT_MS)
            ) {
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs.remove(ArmedItemKey)
                    prefs.remove(ArmedAtKey)
                }
                widget.update(applicationContext, id)
            }
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "widget_disarm"
    }
}
