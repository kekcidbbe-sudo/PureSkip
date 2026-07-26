package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Common skip/close rules shared across apps.
 * Uses text and content description matching with CONTAINS for maximum compatibility.
 */
object CommonSkipRules {

    /**
     * Creates a standard startup ad skip rule for any app.
     * Matches nodes containing "跳过" in text or content description.
     */
    fun createStartupSkipRule(
        packageName: String,
        ruleIdSuffix: String = "startup_skip"
    ): AdRule = AdRule(
        ruleId = "${packageName}_$ruleIdSuffix",
        packageName = packageName,
        ruleVersion = 1,
        adType = AdType.STARTUP_AD,
        activationTimeWindowMs = 10_000L,
        cooldownMs = 2_000L,
        maxTriggersPerSession = 1,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.TEXT,
                value = "跳过",
                matchType = MatchType.CONTAINS
            )
        ),
        action = RuleAction(
            type = ActionType.CLICK,
            delayMs = 200,
            requireClickable = false,
            requireVisible = true
        )
    )

    /**
     * Creates a close-ad rule for popup ads.
     * Matches nodes containing close/skip related text or descriptions.
     */
    fun createCloseAdRule(
        packageName: String,
        ruleIdSuffix: String = "close_ad"
    ): AdRule = AdRule(
        ruleId = "${packageName}_$ruleIdSuffix",
        packageName = packageName,
        ruleVersion = 1,
        adType = AdType.FULLSCREEN_POPUP,
        activationTimeWindowMs = 0L,
        cooldownMs = 2_000L,
        maxTriggersPerSession = 10,
        isExperimental = false,
        conditions = listOf(
            RuleCondition(
                type = ConditionType.CONTENT_DESCRIPTION,
                value = "关闭",
                matchType = MatchType.CONTAINS
            ),
            RuleCondition(
                type = ConditionType.TEXT,
                value = "×",
                matchType = MatchType.CONTAINS
            ),
            RuleCondition(
                type = ConditionType.TEXT,
                value = "关闭",
                matchType = MatchType.CONTAINS
            ),
            RuleCondition(
                type = ConditionType.TEXT,
                value = "跳过广告",
                matchType = MatchType.CONTAINS
            ),
            RuleCondition(
                type = ConditionType.TEXT,
                value = "跳过",
                matchType = MatchType.CONTAINS
            ),
            RuleCondition(
                type = ConditionType.TEXT,
                value = "^[×xX✕]$",
                matchType = MatchType.REGEX
            )
        ),
        action = RuleAction(
            type = ActionType.CLICK,
            delayMs = 200,
            requireClickable = false,
            requireVisible = true
        )
    )
}
