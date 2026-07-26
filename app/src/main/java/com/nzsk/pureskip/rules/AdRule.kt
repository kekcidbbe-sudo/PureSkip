package com.nzsk.pureskip.rules

/**
 * Represents a single ad-skip rule for a specific application.
 * Rules define when and how to identify and click skip/close buttons.
 */
data class AdRule(
    /** Unique rule identifier */
    val ruleId: String,

    /** Target application package name */
    val packageName: String,

    /** Rule version for tracking updates */
    val ruleVersion: Int,

    /** Minimum app version this rule applies to (inclusive) */
    val minAppVersionCode: Long = 0,

    /** Maximum app version this rule applies to (inclusive, 0 = no limit) */
    val maxAppVersionCode: Long = 0,

    /** Type of ad this rule handles */
    val adType: AdType,

    /** Time window in milliseconds after app launch to activate (0 = always) */
    val activationTimeWindowMs: Long = 10_000L,

    /** Minimum cooldown between actions in milliseconds */
    val cooldownMs: Long = 1_000L,

    /** Maximum number of times this rule can trigger per app session */
    val maxTriggersPerSession: Int = 1,

    /** Whether this rule is for experimental popup blocking */
    val isExperimental: Boolean = false,

    /** Test status of this rule */
    val testStatus: RuleTestStatus = RuleTestStatus.TESTED,

    /** Conditions that must ALL be met for the rule to match */
    val conditions: List<RuleCondition>,

    /** The action to perform when conditions match */
    val action: RuleAction
)

/**
 * Types of ads that rules can target.
 */
enum class AdType {
    /** Startup/splash screen ad with skip button */
    STARTUP_AD,

    /** Full-screen popup ad with close button */
    FULLSCREEN_POPUP,

    /** Video pre-roll ad with skip button */
    VIDEO_PREROLL
}

/**
 * Test status of a rule.
 */
enum class RuleTestStatus {
    TESTED,
    UNTESTED,
    NEEDS_UPDATE
}

/**
 * A condition that must be met for a rule to match.
 * All conditions in a rule must be satisfied simultaneously.
 */
data class RuleCondition(
    /** Type of condition to evaluate */
    val type: ConditionType,

    /** The expected value (interpretation depends on type) */
    val value: String,

    /** How to match the value */
    val matchType: MatchType = MatchType.EXACT
)

/**
 * Types of conditions for rule matching.
 */
enum class ConditionType {
    /** Match by View ID (e.g., "com.example:id/skip_btn") */
    VIEW_ID,

    /** Match by visible text content */
    TEXT,

    /** Match by content description (accessibility) */
    CONTENT_DESCRIPTION,

    /** Match by view class name */
    VIEW_CLASS,

    /** Match current activity/fragment name */
    ACTIVITY_NAME,

    /** Match window title */
    WINDOW_TITLE
}

/**
 * How to match a condition value.
 */
enum class MatchType {
    EXACT,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    REGEX
}

/**
 * Action to perform when a rule matches.
 */
data class RuleAction(
    /** Type of action to perform */
    val type: ActionType,

    /** Delay before performing action in milliseconds */
    val delayMs: Long = 0,

    /** Whether to wait for the element to be clickable before acting */
    val requireClickable: Boolean = true,

    /** Whether the element must be visible on screen */
    val requireVisible: Boolean = true
)

/**
 * Types of actions that can be performed.
 */
enum class ActionType {
    /** Click the matched element */
    CLICK,

    /** Long press the matched element */
    LONG_CLICK
}
