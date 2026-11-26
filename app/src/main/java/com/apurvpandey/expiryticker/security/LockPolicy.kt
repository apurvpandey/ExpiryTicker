package com.apurvpandey.expiryticker.security

/** When the app locks itself. Kept free of Android types so it can be unit tested. */
object LockPolicy {
    /** Leaving the app for less than this keeps it unlocked, so a quick app switch is not annoying. */
    const val RELOCK_AFTER_MS = 30_000L

    /**
     * @param coldStart the process has just started (nothing has been unlocked yet)
     * @param msSinceBackgrounded how long the app was out of sight; null if it was never backgrounded
     */
    fun shouldLock(enabled: Boolean, coldStart: Boolean, msSinceBackgrounded: Long?): Boolean = when {
        !enabled -> false
        coldStart -> true
        msSinceBackgrounded == null -> false
        // A clock that went backwards must not unlock anything.
        msSinceBackgrounded < 0 -> true
        else -> msSinceBackgrounded >= RELOCK_AFTER_MS
    }
}
