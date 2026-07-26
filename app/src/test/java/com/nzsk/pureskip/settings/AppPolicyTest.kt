package com.nzsk.pureskip.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPolicyTest {

    @Test
    fun `enhancement is isolated to the selected package`() {
        val selected = AppPolicy(
            packageName = "com.example.indie",
            standardEnabled = true,
            enhancementEnabled = true,
            recognitionMode = RecognitionMode.SMART_ENHANCED,
            scope = EnhancementScope.FULL_TIME
        )
        val normal = AppPolicy.default("com.example.normal")

        assertTrue(selected.shouldUseEnhancedRules())
        assertFalse(normal.shouldUseEnhancedRules())
        assertEquals(EnhancementScope.FULL_TIME, selected.scope)
    }

    @Test
    fun `learned rule converts normalized coordinates to current screen`() {
        val rule = LearnedPopupRule(
            ruleId = "learned-1",
            packageName = "com.example.indie",
            normalizedX = 0.9f,
            normalizedY = 0.1f,
            orientation = 1,
            activityName = "MainActivity",
            windowTitle = "",
            hierarchyFingerprint = "abc",
            autoEligible = true
        )

        assertEquals(972f, rule.screenX(1080), 0.01f)
        assertEquals(234f, rule.screenY(2340), 0.01f)
    }
}
