package com.apurvpandey.expiryticker

import com.apurvpandey.expiryticker.core.util.ConfirmTap
import com.apurvpandey.expiryticker.core.util.ConfirmTap.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmTapTest {
    private val t0 = 1_000_000L

    @Test fun firstTapArms() = assertEquals(Decision.ARM, ConfirmTap.decide(null, null, t0, 7))

    @Test fun secondTapOnSameItemWithinTimeoutConfirms() =
        assertEquals(Decision.CONFIRM, ConfirmTap.decide(7, t0, t0 + 2_000, 7))

    @Test fun tapAtTheTimeoutBoundaryStillConfirms() =
        assertEquals(Decision.CONFIRM, ConfirmTap.decide(7, t0, t0 + ConfirmTap.ARM_TIMEOUT_MS, 7))

    @Test fun lateTapArmsAgainInsteadOfConfirming() =
        assertEquals(Decision.ARM, ConfirmTap.decide(7, t0, t0 + ConfirmTap.ARM_TIMEOUT_MS + 1, 7))

    @Test fun tappingAnotherItemArmsThatOneInstead() =
        assertEquals(Decision.ARM, ConfirmTap.decide(7, t0, t0 + 1_000, 8))

    @Test fun clockGoingBackwardsNeverConfirms() =
        assertEquals(Decision.ARM, ConfirmTap.decide(7, t0, t0 - 1, 7))

    @Test fun isArmedMatchesDecision() {
        assertTrue(ConfirmTap.isArmed(7, t0, t0 + 1, 7))
        assertFalse(ConfirmTap.isArmed(7, null, t0, 7))
        assertFalse(ConfirmTap.isArmed(null, t0, t0, 7))
    }
}
