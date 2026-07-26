package com.nzsk.pureskip.rules

import com.nzsk.pureskip.rules.apps.*

/**
 * Provides access to all built-in ad-skip rules.
 * Rules are loaded at service initialization and never fetched from network.
 */
object RuleProvider {

    private val allRules = mutableListOf<AdRule>()
    private val packageRules = mutableMapOf<String, MutableList<AdRule>>()
    private const val WILDCARD_KEY = "*"

    /**
     * Initializes all built-in rules.
     */
    fun initialize() {
        allRules.clear()
        packageRules.clear()

        // Universal rules: cover ALL common skip/close text variants
        // Conditions use OR logic (any match is sufficient)
        val universalRules = listOf(
            // Standard: startup ad skip (SAFE - only matches ad-specific text)
            AdRule(
                ruleId = "universal_ad_skip",
                packageName = "*",
                ruleVersion = 5,
                adType = AdType.STARTUP_AD,
                // Generic "跳过" text is only safe near app startup. Keeping a generous
                // 30-second window preserves slow splash ads without scanning normal pages.
                activationTimeWindowMs = 30_000L,
                cooldownMs = 2_000L,
                maxTriggersPerSession = 3,
                conditions = listOf(
                    // Core: "跳过" variants (most common ad skip button text)
                    RuleCondition(ConditionType.TEXT, "跳过", MatchType.CONTAINS),
                    RuleCondition(ConditionType.TEXT, "关闭广告", MatchType.CONTAINS),
                    RuleCondition(ConditionType.TEXT, "跳过广告", MatchType.CONTAINS),
                    // Countdown formats: "跳过 3s", "3s后跳过", "跳过(5秒)"
                    RuleCondition(ConditionType.TEXT, "\\d+\\s*[s秒].*跳过", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "跳过.*\\d+\\s*[s秒)）]", MatchType.REGEX),
                    // Content description for icon-only skip buttons
                    RuleCondition(ConditionType.CONTENT_DESCRIPTION, "跳过", MatchType.CONTAINS),
                    RuleCondition(ConditionType.CONTENT_DESCRIPTION, "跳过广告", MatchType.CONTAINS),
                    RuleCondition(ConditionType.CONTENT_DESCRIPTION, "skip", MatchType.CONTAINS)
                ),
                action = RuleAction(
                    type = ActionType.CLICK,
                    delayMs = 100,
                    requireClickable = false,
                    requireVisible = true
                )
            ),
            // Experimental: in-app popup dismissal (conservative matching)
            // Only matches SHORT text on SMALL elements to avoid false positives
            AdRule(
                ruleId = "universal_popup_dismiss",
                packageName = "*",
                ruleVersion = 2,
                adType = AdType.FULLSCREEN_POPUP,
                activationTimeWindowMs = 0L,
                cooldownMs = 2_000L,
                maxTriggersPerSession = 5,
                isExperimental = true,
                conditions = listOf(
                    // Single character close buttons (most reliable)
                    RuleCondition(ConditionType.TEXT, "^[×xX✕]\\s*$", MatchType.REGEX),
                    // Short dismiss phrases only (2-6 chars, very specific to ads/popups)
                    RuleCondition(ConditionType.TEXT, "^关闭广告$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^跳过广告$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^跳过$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^以后再说$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^暂不$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^不了$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^稍后再说$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^下次再说$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^不感兴趣$", MatchType.REGEX),
                    RuleCondition(ConditionType.TEXT, "^我知道了$", MatchType.REGEX),
                    // Delayed/countdown variants used by WebView and ad SDK controls
                    RuleCondition(
                        ConditionType.TEXT,
                        "^(跳过|跳过广告|关闭广告)\\s*[0-9０-９]*\\s*(s|秒)?$",
                        MatchType.REGEX
                    ),
                    // Content description (for icon-only buttons)
                    RuleCondition(ConditionType.CONTENT_DESCRIPTION, "^close$", MatchType.REGEX),
                    RuleCondition(ConditionType.CONTENT_DESCRIPTION, "^关闭$", MatchType.REGEX),
                    RuleCondition(ConditionType.CONTENT_DESCRIPTION, "^跳过$", MatchType.REGEX),
                    RuleCondition(
                        ConditionType.CONTENT_DESCRIPTION,
                        "^(close button|dismiss|关闭按钮|关闭广告|skip|skip ad|跳过按钮|跳过广告)$",
                        MatchType.REGEX
                    ),
                    RuleCondition(ConditionType.CONTENT_DESCRIPTION, "^[×xX✕]$", MatchType.REGEX),
                    // Many ad SDKs expose no text but keep stable resource IDs.
                    // Two matching ID signals are intentional: an ad-specific ID can be trusted
                    // even when its accessibility node itself is not clickable.
                    RuleCondition(
                        ConditionType.VIEW_ID,
                        "(?:^|[:/._-])(?:iv|img|image|btn|button|tv|text|view|layout)?[_-]*(?:ad|ads|advert|splash|interstitial|popup)[_-]*(?:close|skip|dismiss)(?:$|[_-])",
                        MatchType.REGEX
                    ),
                    RuleCondition(
                        ConditionType.VIEW_ID,
                        "(?:^|[:/._-])(?:close|skip|dismiss)[_-]*(?:ad|ads|advert|splash|interstitial|popup)(?:$|[_-])",
                        MatchType.REGEX
                    ),
                    RuleCondition(
                        ConditionType.VIEW_ID,
                        "(?:^|[:/._-])(?:iv|img|image|btn|button|tv|text|view|layout)?[_-]*(?:close|skip|dismiss)(?:$|[_-])",
                        MatchType.REGEX
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

        // Register universal rules under wildcard key
        for (rule in universalRules) {
            allRules.add(rule)
            packageRules.getOrPut(WILDCARD_KEY) { mutableListOf() }.add(rule)
        }

        // Register all app-specific rules
        val appRuleSets = listOf(
            TencentVideoRules.getRules(),
            IqiyiRules.getRules(),
            YoukuRules.getRules(),
            MangoTvRules.getRules(),
            SohuVideoRules.getRules(),
            TomatoNovelRules.getRules(),
            QimaoNovelRules.getRules(),
            QidianRules.getRules(),
            QqReadingRules.getRules(),
            IreaderRules.getRules(),
            BaiduNetdiskRules.getRules(),
            ChinaMobileRules.getRules(),
            DouyinRules.getRules()
        )

        for (ruleSet in appRuleSets) {
            for (rule in ruleSet) {
                allRules.add(rule)
                packageRules.getOrPut(rule.packageName) { mutableListOf() }.add(rule)
            }
        }
    }

    /**
     * Gets all rules for a specific package.
     * Always includes universal wildcard rules.
     */
    fun getRulesForPackage(packageName: String): List<AdRule> {
        val specific = packageRules[packageName] ?: emptyList()
        val universal = packageRules[WILDCARD_KEY] ?: emptyList()
        return specific + universal
    }

    /**
     * Gets all registered rules.
     */
    fun getAllRules(): List<AdRule> = allRules.toList()

    /**
     * Gets the total number of registered rules.
     */
    fun getRuleCount(): Int = allRules.size

    /**
     * Gets all package names that have rules.
     */
    fun getRegisteredPackages(): Set<String> = packageRules.keys.toSet()

    /**
     * Checks if a package has any rules.
     */
    fun hasRulesForPackage(packageName: String): Boolean = packageRules.containsKey(packageName)
}
