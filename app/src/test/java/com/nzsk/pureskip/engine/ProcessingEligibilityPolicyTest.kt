package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingEligibilityPolicyTest {

    @Test
    fun `restricted system package is never scanned`() {
        assertFalse(
            ProcessingEligibilityPolicy.shouldProcess(
                masterEnabled = true,
                paused = false,
                appEnabled = true,
                appBlocked = false,
                systemRestricted = true
            )
        )
    }

    @Test
    fun `enabled ordinary app remains eligible`() {
        assertTrue(ProcessingEligibilityPolicy.shouldProcess(true, false, true, false, false))
    }
}
