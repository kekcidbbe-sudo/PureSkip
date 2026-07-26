package com.nzsk.pureskip.safety

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSafetyPolicyTest {

    @Test
    fun blocksSystemPermissionLauncherAndKeyboardPackages() {
        assertTrue(AppSafetyPolicy.isRestricted("com.android.settings"))
        assertTrue(AppSafetyPolicy.isRestricted("com.miui.home.launcher"))
        assertTrue(AppSafetyPolicy.isRestricted("com.example.keyboard"))
        assertTrue(AppSafetyPolicy.isRestricted("com.google.android.permissioncontroller"))
    }

    @Test
    fun allowsOrdinaryThirdPartyPackages() {
        assertFalse(AppSafetyPolicy.isRestricted("com.example.indie.video"))
    }

    @Test
    fun keyboardWindowDoesNotResetTheForegroundAppSession() {
        assertTrue(
            AppSafetyPolicy.preservesForegroundSession(
                "com.google.android.inputmethod.latin"
            )
        )
        assertTrue(
            AppSafetyPolicy.preservesForegroundSession(
                "com.example.keyboard"
            )
        )
        assertFalse(
            AppSafetyPolicy.preservesForegroundSession(
                "com.miui.home.launcher"
            )
        )
    }
}
