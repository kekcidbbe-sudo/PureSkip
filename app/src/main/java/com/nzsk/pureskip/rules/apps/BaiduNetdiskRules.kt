package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Baidu Netdisk (百度网盘) ad-skip rules.
 * Package: com.baidu.netdisk
 */
object BaiduNetdiskRules {

    private const val PKG = "com.baidu.netdisk"

    fun getRules(): List<AdRule> = listOf(
        // Rule 1: Startup ad with countdown - match "跳过 Xs" or "跳过广告 Xs"
        AdRule(
            ruleId = "${PKG}_startup_countdown",
            packageName = PKG,
            ruleVersion = 2,
            adType = AdType.STARTUP_AD,
            activationTimeWindowMs = 25_000L,
            cooldownMs = 1_500L,
            maxTriggersPerSession = 3,
            conditions = listOf(
                // Match "跳过" followed by optional countdown
                RuleCondition(ConditionType.TEXT, "跳过", MatchType.CONTAINS),
                // Match "跳过广告"
                RuleCondition(ConditionType.TEXT, "跳过广告", MatchType.CONTAINS),
                // Match countdown format "Xs" or "X秒"
                RuleCondition(ConditionType.TEXT, "\\d+\\s*[s秒]", MatchType.REGEX),
                // Content description fallback
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "跳过", MatchType.CONTAINS),
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "skip", MatchType.CONTAINS)
            ),
            action = RuleAction(
                type = ActionType.CLICK,
                delayMs = 200,
                requireClickable = false,
                requireVisible = true
            )
        ),

        // Rule 2: Close button on ad popup
        AdRule(
            ruleId = "${PKG}_close_popup",
            packageName = PKG,
            ruleVersion = 2,
            adType = AdType.FULLSCREEN_POPUP,
            activationTimeWindowMs = 0L,
            cooldownMs = 1_500L,
            maxTriggersPerSession = 5,
            conditions = listOf(
                RuleCondition(ConditionType.TEXT, "关闭广告", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "关闭", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "^[×xX✕]\\s*$", MatchType.REGEX),
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "关闭", MatchType.CONTAINS),
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "close", MatchType.CONTAINS)
            ),
            action = RuleAction(
                type = ActionType.CLICK,
                delayMs = 200,
                requireClickable = false,
                requireVisible = true
            )
        )
    )
}
