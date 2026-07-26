package com.nzsk.pureskip.rules

import com.nzsk.pureskip.settings.EnhancementScope

/**
 * Information about an application that has been detected by the service.
 */
data class AppInfo(
    /** Application package name */
    val packageName: String,

    /** User-friendly application name */
    val appName: String,

    /** Whether ad-skip is enabled for this app */
    val isEnabled: Boolean = true,

    /** Whether this app has been blocked by the user */
    val isBlocked: Boolean = false,

    /** Whether experimental popup blocking is enabled for this app */
    val isExperimentalEnabled: Boolean = false,

    val enhancementScope: EnhancementScope = EnhancementScope.FULL_TIME,

    val learnedRuleCount: Int = 0,

    val isSafetyRestricted: Boolean = false,

    /** Number of times ads have been skipped for this app */
    val skipCount: Int = 0,

    /** Available rules for this app */
    val ruleCount: Int = 0,

    /** Last time this app was encountered */
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
