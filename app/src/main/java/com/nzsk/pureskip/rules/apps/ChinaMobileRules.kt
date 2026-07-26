package com.nzsk.pureskip.rules.apps

import com.nzsk.pureskip.rules.*

/**
 * China Mobile (中国移动) ad-skip rules.
 * Package: com.chinamobile.mcloud or com.greenpoint.android.mc10086
 */
object ChinaMobileRules {

    private const val PACKAGE_NAME_MCCLOUD = "com.chinamobile.mcloud"
    private const val PACKAGE_NAME_MC10086 = "com.greenpoint.android.mc10086"

    fun getRules(): List<AdRule> {
        val rules = mutableListOf<AdRule>()

        // Rules for com.chinamobile.mcloud
        rules.addAll(createRulesForPackage(PACKAGE_NAME_MCCLOUD))

        // Rules for com.greenpoint.android.mc10086
        rules.addAll(createRulesForPackage(PACKAGE_NAME_MC10086))

        return rules
    }

    private fun createRulesForPackage(packageName: String): List<AdRule> = listOf(
        // Startup ad skip
        CommonSkipRules.createStartupSkipRule(packageName),

        // Close popup ad
        CommonSkipRules.createCloseAdRule(packageName),

        // China Mobile-specific: "跳过" with countdown
        AdRule(
            ruleId = "${packageName}_skip_countdown",
            packageName = packageName,
            ruleVersion = 1,
            adType = AdType.STARTUP_AD,
            activationTimeWindowMs = 15_000L,
            cooldownMs = 2_000L,
            maxTriggersPerSession = 2,
            conditions = listOf(
                RuleCondition(
                    type = ConditionType.TEXT,
                    value = "跳过",
                    matchType = MatchType.CONTAINS
                ),
                RuleCondition(
                    type = ConditionType.TEXT,
                    value = "\\d+\\s*s",
                    matchType = MatchType.REGEX
                ),
                RuleCondition(
                    type = ConditionType.CONTENT_DESCRIPTION,
                    value = "跳过",
                    matchType = MatchType.CONTAINS
                )
            ),
            action = RuleAction(
                type = ActionType.CLICK,
                delayMs = 300,
                requireClickable = false,
                requireVisible = true
            )
        ),

        // China Mobile-specific: close button
        AdRule(
            ruleId = "${packageName}_close_popup",
            packageName = packageName,
            ruleVersion = 1,
            adType = AdType.FULLSCREEN_POPUP,
            activationTimeWindowMs = 0L,
            cooldownMs = 1_500L,
            maxTriggersPerSession = 10,
            conditions = listOf(
                RuleCondition(
                    type = ConditionType.TEXT,
                    value = "^[×xX✕]\\s*$",
                    matchType = MatchType.REGEX
                ),
                RuleCondition(
                    type = ConditionType.CONTENT_DESCRIPTION,
                    value = "关闭",
                    matchType = MatchType.CONTAINS
                ),
                RuleCondition(
                    type = ConditionType.TEXT,
                    value = "关闭",
                    matchType = MatchType.CONTAINS
                ),
                RuleCondition(
                    type = ConditionType.TEXT,
                    value = "我知道了",
                    matchType = MatchType.CONTAINS
                ),
                RuleCondition(
                    type = ConditionType.TEXT,
                    value = "暂不",
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
    )
}
