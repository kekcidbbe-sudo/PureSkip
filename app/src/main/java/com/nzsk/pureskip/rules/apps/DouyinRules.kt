package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * Douyin (抖音) ad-skip rules.
 * Package: com.ss.android.ugc.aweme
 *
 * IMPORTANT: These rules must NOT match the comment section close button (×)
 * or other normal UI elements. Only match ad-specific text.
 */
object DouyinRules {

    private const val PKG = "com.ss.android.ugc.aweme"

    fun getRules(): List<AdRule> = listOf(
        // Startup ad skip - match countdown format like "跳过 3s"
        AdRule(
            ruleId = "${PKG}_startup_skip",
            packageName = PKG,
            ruleVersion = 1,
            adType = AdType.STARTUP_AD,
            activationTimeWindowMs = 15_000L,
            cooldownMs = 2_000L,
            maxTriggersPerSession = 3,
            conditions = listOf(
                // Match "跳过 Xs" countdown pattern (very specific to ads)
                RuleCondition(ConditionType.TEXT, "跳过 \\d+", MatchType.REGEX),
                RuleCondition(ConditionType.TEXT, "\\d+\\s*[s秒]", MatchType.REGEX),
                // Match ad-specific text
                RuleCondition(ConditionType.TEXT, "跳过广告", MatchType.CONTAINS),
                RuleCondition(ConditionType.TEXT, "关闭广告", MatchType.CONTAINS),
                // Content description
                RuleCondition(ConditionType.CONTENT_DESCRIPTION, "跳过广告", MatchType.CONTAINS)
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
