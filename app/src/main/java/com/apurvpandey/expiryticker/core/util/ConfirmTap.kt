package com.apurvpandey.expiryticker.core.util

/**
 * Two-step confirmation for actions that cannot be undone from where they are triggered
 * (the widget has no undo): the first tap arms an item, a second tap on the same item soon
 * after confirms. Arming is a timestamp, so nothing has to wait around for it to expire.
 */
object ConfirmTap {
    enum class Decision { ARM, CONFIRM }

    /** How long an armed button waits for the confirming tap. */
    const val ARM_TIMEOUT_MS = 5_000L

    fun isArmed(armedItemId: Long?, armedAtMs: Long?, nowMs: Long, itemId: Long): Boolean =
        armedItemId == itemId && armedAtMs != null && nowMs - armedAtMs in 0..ARM_TIMEOUT_MS

    fun decide(armedItemId: Long?, armedAtMs: Long?, nowMs: Long, tappedItemId: Long): Decision =
        if (isArmed(armedItemId, armedAtMs, nowMs, tappedItemId)) Decision.CONFIRM else Decision.ARM
}
