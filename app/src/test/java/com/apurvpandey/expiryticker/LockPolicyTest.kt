package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.security.LockPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {
    @Test fun disabledNeverLocks() {
        assertFalse(LockPolicy.shouldLock(false, true, null))
        assertFalse(LockPolicy.shouldLock(false, false, 10 * 60_000))
    }

    @Test fun coldStartLocks() = assertTrue(LockPolicy.shouldLock(true, true, null))

    @Test fun quickAppSwitchStaysUnlocked() = assertFalse(LockPolicy.shouldLock(true, false, 5_000))

    @Test fun longAbsenceLocksAtTheThreshold() {
        assertFalse(LockPolicy.shouldLock(true, false, LockPolicy.RELOCK_AFTER_MS - 1))
        assertTrue(LockPolicy.shouldLock(true, false, LockPolicy.RELOCK_AFTER_MS))
    }

    @Test fun neverBackgroundedStaysUnlocked() = assertFalse(LockPolicy.shouldLock(true, false, null))

    @Test fun clockGoingBackwardsLocks() = assertTrue(LockPolicy.shouldLock(true, false, -1))
}
