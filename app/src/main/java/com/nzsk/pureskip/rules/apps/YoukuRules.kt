package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for Youku (优酷).
 * Package: com.youku.phone
 */
object YoukuRules {
    private const val PKG = "com.youku.phone"
    fun getRules(): List<AdRule> = listOf(
        // Rule 1: Startup ad with View ID
        AdRule(
            ruleId = "${PKG}_startup_viewid",
            packageName = PKG,
            ruleVersion = 2,
            adType = AdType.STARTUP_AD,
            activationTimeWindowMs = 30_000L,
            cooldownMs = 1_500L,
            maxTriggersPerSession = 3,
            conditions = listOf(
                RuleCondition(ConditionType.VIEW_ID, "com.youku.phone:id/skip_ad", MatchType.EXACT),
                RuleCondition(ConditionType.VIEW_ID, "com.youku.phone:id/ad_skip_btn", MatchType.EXACT),
                RuleCondition(ConditionType.VIEW_ID, "com.youku.phone:id/btn_skip", MatchType.EXACT),
                RuleCondition(ConditionType.TEXT, "跳过", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "关闭广告", MatchType.CONTAINS)
            ),
            action = RuleAction(type = ActionType.CLICK, delayMs = 100, requireClickable = false, requireVisible = true)
        ),
        // Rule 2: Content description fallback
        AdRule(
            ruleId = "${PKG}_startup_desc",
            packageName = PKG,
            ruleVersion = 2,
            adType = AdType.STARTUP_AD,
            activationTimeWindowMs = 30_000L,
            cooldownMs = 1_500L,
            maxTriggersPerSession = 3,
            conditions = listOf(
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "跳过", MatchType.CONTAINS),
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "关闭", MatchType.CONTAINS)
            ),
            action = RuleAction(type = ActionType.CLICK, delayMs = 100, requireClickable = false, requireVisible = true)
        )
    )
}
