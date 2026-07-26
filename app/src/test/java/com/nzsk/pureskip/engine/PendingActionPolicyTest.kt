package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingActionPolicyTest {

    @Test
    fun `delayed click is rejected after the active window changes`() {
        assertFalse(
            PendingActionPolicy.canExecute(
                expectedPackage = "com.ss.android.ugc.aweme",
                currentPackage = "com.ss.android.ugc.aweme",
                activeRootPackage = "com.google.android.inputmethod.latin",
                targetPackage = "com.ss.android.ugc.aweme",
                targetRefreshed = false,
                targetVisible = true
            )
        )
    }

    @Test
    fun `fresh visible target in the same app can execute`() {
        assertTrue(
            PendingActionPolicy.canExecute(
                expectedPackage = "com.ss.android.ugc.aweme",
                currentPackage = "com.ss.android.ugc.aweme",
                activeRootPackage = "com.ss.android.ugc.aweme",
                targetPackage = "com.ss.android.ugc.aweme",
                targetRefreshed = true,
                targetVisible = true
            )
        )
    }
}
