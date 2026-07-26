package com.nzsk.pureskip.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnedRuleMatcherTest {

    private val rule = LearnedPopupRule(
        ruleId = "learned-1",
        packageName = "com.example.indie",
        normalizedX = 0.9f,
        normalizedY = 0.1f,
        orientation = 1,
        activityName = "MainActivity",
        windowTitle = "",
        hierarchyFingerprint = "stable",
        autoEligible = true
    )

    @Test
    fun automaticRuleRequiresMatchingContext() {
        assertTrue(LearnedRuleMatcher.matchesAutomatic(rule, 1, "MainActivity", "", "stable"))
        assertFalse(LearnedRuleMatcher.matchesAutomatic(rule, 2, "MainActivity", "", "stable"))
        assertFalse(LearnedRuleMatcher.matchesAutomatic(rule, 1, "OtherActivity", "", "stable"))
        assertFalse(LearnedRuleMatcher.matchesAutomatic(rule, 1, "MainActivity", "", "changed"))
    }

    @Test
    fun manualOnlyRuleIsNeverAutomatic() {
        assertFalse(
            LearnedRuleMatcher.matchesAutomatic(
                rule.copy(autoEligible = false),
                1,
                "MainActivity",
                "",
                "stable"
            )
        )
    }
}
