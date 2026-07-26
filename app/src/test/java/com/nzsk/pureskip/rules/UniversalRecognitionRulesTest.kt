package com.nzsk.pureskip.rules

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UniversalRecognitionRulesTest {

    @Before
    fun initializeRules() {
        RuleProvider.initialize()
    }

    @Test
    fun `enhanced universal rules recognize common ad close resource ids`() {
        val conditions = RuleProvider.getRulesForPackage("com.example.unlisted")
            .filter { it.isExperimental }
            .flatMap { it.conditions }
            .filter { it.type == ConditionType.VIEW_ID }

        val commonResourceIds = listOf(
            "com.vendor.video:id/iv_ad_close",
            "com.vendor.reader:id/splash_skip_btn",
            "com.vendor.smallapp:id/tt_video_ad_close_layout"
        )

        commonResourceIds.forEach { resourceId ->
            assertTrue(
                "增强规则应识别控件 ID：$resourceId",
                conditions.any { it.matches(resourceId) }
            )
        }
    }

    @Test
    fun `generic skip text is limited to the startup stage`() {
        val rule = RuleProvider.getRulesForPackage("com.example.unlisted")
            .first { it.ruleId == "universal_ad_skip" }

        assertTrue(
            "通用“跳过”规则不能在应用全程生效",
            rule.activationTimeWindowMs in 10_000L..30_000L
        )
        assertTrue(
            "一次应用会话内不能反复点击通用“跳过”控件",
            rule.maxTriggersPerSession <= 5
        )
    }

    private fun RuleCondition.matches(actual: String): Boolean {
        return when (matchType) {
            MatchType.EXACT -> actual.equals(value, ignoreCase = true)
            MatchType.CONTAINS -> actual.contains(value, ignoreCase = true)
            MatchType.STARTS_WITH -> actual.startsWith(value, ignoreCase = true)
            MatchType.ENDS_WITH -> actual.endsWith(value, ignoreCase = true)
            MatchType.REGEX -> Regex(value, RegexOption.IGNORE_CASE).containsMatchIn(actual)
        }
    }
}
