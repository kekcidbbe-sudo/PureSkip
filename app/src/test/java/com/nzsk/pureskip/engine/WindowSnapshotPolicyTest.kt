package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowSnapshotPolicyTest {

    @Test
    fun `ordinary no-match scan does not build a fingerprint`() {
        assertFalse(
            WindowSnapshotPolicy.requiresSnapshot(
                actionNeedsVerification = false,
                hasLearnedRules = false,
                diagnosticsEnabled = false
            )
        )
    }

    @Test
    fun `verification learned rules and diagnostics still request a fingerprint`() {
        assertTrue(WindowSnapshotPolicy.requiresSnapshot(true, false, false))
        assertTrue(WindowSnapshotPolicy.requiresSnapshot(false, true, false))
        assertTrue(WindowSnapshotPolicy.requiresSnapshot(false, false, true))
    }
}
