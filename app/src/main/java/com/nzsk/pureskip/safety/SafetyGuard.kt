package com.nzsk.pureskip.safety

import android.view.accessibility.AccessibilityNodeInfo
import com.nzsk.pureskip.rules.AdRule

/**
 * Central safety controller that gates all ad-skip actions.
 * Performs comprehensive checks before any action is executed.
 */
class SafetyGuard private constructor() {

    private val sensitiveSceneDetector = SensitiveSceneDetector()
    private val frequencyLimiter = FrequencyLimiter()

    /**
     * Checks if an action is safe to perform.
     * @return SafetyCheckResult indicating pass/fail with reason
     */
    fun check(
        packageName: String,
        rootNode: AccessibilityNodeInfo?,
        rule: AdRule
    ): SafetyCheckResult {
        // 1. Check if the service is globally enabled and not paused
        if (!isGloballyEnabled()) {
            return SafetyCheckResult.Blocked("服务已暂停")
        }

        // 2. Check for sensitive scenes (password, payment, system dialogs, etc.)
        rootNode?.let { node ->
            if (sensitiveSceneDetector.isSensitiveScene(node, packageName)) {
                return SafetyCheckResult.Blocked("敏感场景保护")
            }
        }

        // 3. Check frequency limits
        if (!frequencyLimiter.canExecute(packageName, rule.ruleId, rule.maxTriggersPerSession)) {
            return SafetyCheckResult.Blocked("频率限制")
        }

        // 4. Check activation time window
        if (!frequencyLimiter.isWithinTimeWindow(packageName, rule.activationTimeWindowMs)) {
            return SafetyCheckResult.Blocked("超出时间窗口")
        }

        // 5. Check cooldown
        if (!frequencyLimiter.isCooldownMet(packageName, rule.ruleId, rule.cooldownMs)) {
            return SafetyCheckResult.Blocked("冷却中")
        }

        return SafetyCheckResult.Allowed
    }

    fun checkLearnedAction(
        packageName: String,
        rootNode: AccessibilityNodeInfo?,
        ruleId: String
    ): SafetyCheckResult {
        if (!isGloballyEnabled()) return SafetyCheckResult.Blocked("服务已暂停")
        if (AppSafetyPolicy.isRestricted(packageName)) {
            return SafetyCheckResult.Blocked("受保护应用")
        }
        if (rootNode == null || sensitiveSceneDetector.isSensitiveScene(rootNode, packageName)) {
            return SafetyCheckResult.Blocked("敏感场景保护")
        }
        if (!frequencyLimiter.canExecute(packageName, ruleId, LEARNED_MAX_TRIGGERS)) {
            return SafetyCheckResult.Blocked("频率限制")
        }
        if (!frequencyLimiter.isCooldownMet(packageName, ruleId, LEARNED_COOLDOWN_MS)) {
            return SafetyCheckResult.Blocked("冷却中")
        }
        return SafetyCheckResult.Allowed
    }

    /**
     * Records that an action was executed for frequency tracking.
     */
    fun recordAction(packageName: String, ruleId: String) {
        frequencyLimiter.recordExecution(packageName, ruleId)
    }

    /**
     * Resets session tracking for a new app launch.
     */
    fun onNewSession(packageName: String) {
        frequencyLimiter.resetSession(packageName)
    }

    /**
     * Clears all tracking data.
     */
    fun clearAll() {
        frequencyLimiter.clearAll()
    }

    private fun isGloballyEnabled(): Boolean {
        val settings = com.nzsk.pureskip.PureSkipApplication.getInstance().settingsManager
        return settings.isMasterEnabled() && !settings.isPaused()
    }

    companion object {
        private const val LEARNED_MAX_TRIGGERS = 5
        private const val LEARNED_COOLDOWN_MS = 2_000L
        @Volatile
        private var instance: SafetyGuard? = null

        fun getInstance(): SafetyGuard {
            return instance ?: synchronized(this) {
                instance ?: SafetyGuard().also { instance = it }
            }
        }
    }
}

/**
 * Result of a safety check.
 */
sealed class SafetyCheckResult {
    data object Allowed : SafetyCheckResult()
    data class Blocked(val reason: String) : SafetyCheckResult()
}
