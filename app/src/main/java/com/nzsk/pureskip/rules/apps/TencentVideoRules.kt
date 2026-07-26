package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Rules for Tencent Video (腾讯视频).
 * Package: com.tencent.qqlive
 */
object TencentVideoRules {
    private const val PKG = "com.tencent.qqlive"
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
                RuleCondition(ConditionType.VIEW_ID, "com.tencent.qqlive:id/skip_ad_btn", MatchType.EXACT),
                RuleCondition(ConditionType.VIEW_ID, "com.tencent.qqlive:id/skip_btn", MatchType.EXACT),
                RuleCondition(ConditionType.VIEW_ID, "com.tencent.qqlive:id/ad_skip", MatchType.EXACT),
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
        ),
        // Rule 3: In-app popup ad (experimental)
        AdRule(
            ruleId = "${PKG}_inapp_popup",
            packageName = PKG,
            ruleVersion = 1,
            adType = AdType.FULLSCREEN_POPUP,
            activationTimeWindowMs = 0L,
            cooldownMs = 2_000L,
            maxTriggersPerSession = 10,
            isExperimental = true,
            conditions = listOf(
                RuleCondition(ConditionType.TEXT, "^[×xX✕]\\s*$", MatchType.REGEX),
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "关闭", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "关闭", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "跳过", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "不感兴趣", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "以后再说", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "暂不开通", MatchType.CONTAINS)
            ),
            action = RuleAction(type = ActionType.CLICK, delayMs = 200, requireClickable = false, requireVisible = true)
        )
    )
}
