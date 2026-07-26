package com.nzsk.pureskip.safety

/**
 * Manages frequency limits and cooldowns for ad-skip actions.
 * Prevents excessive clicking and tracks per-session limits.
 */
class FrequencyLimiter {

    // Track per-app, per-rule execution counts per session
    private val sessionCounts = mutableMapOf<String, MutableMap<String, Int>>()

    // Track last execution time per app, per-rule
    private val lastExecutionTimes = mutableMapOf<String, MutableMap<String, Long>>()

    // Track app launch times for time window checks
    private val appLaunchTimes = mutableMapOf<String, Long>()

    /**
     * Checks if an execution is allowed based on session limits.
     */
    fun canExecute(
        packageName: String,
        ruleId: String,
        maxTriggers: Int = DEFAULT_MAX_TRIGGERS_PER_SESSION
    ): Boolean {
        val appCounts = sessionCounts[packageName] ?: return true
        val count = appCounts[ruleId] ?: 0
        return count < maxTriggers.coerceAtLeast(1)
    }

    /**
     * Checks if the current time is within the allowed activation window.
     */
    fun isWithinTimeWindow(packageName: String, timeWindowMs: Long): Boolean {
        if (timeWindowMs <= 0) return true // No time window restriction
        val launchTime = appLaunchTimes[packageName] ?: return false
        return System.currentTimeMillis() - launchTime <= timeWindowMs
    }

    /**
     * Checks if enough time has passed since the last execution (cooldown).
     */
    fun isCooldownMet(packageName: String, ruleId: String, cooldownMs: Long): Boolean {
        val appTimes = lastExecutionTimes[packageName] ?: return true
        val lastTime = appTimes[ruleId] ?: return true
        return System.currentTimeMillis() - lastTime >= cooldownMs
    }

    /**
     * Records that an execution occurred.
     */
    fun recordExecution(packageName: String, ruleId: String) {
        val now = System.currentTimeMillis()

        // Update count
        val appCounts = sessionCounts.getOrPut(packageName) { mutableMapOf() }
        appCounts[ruleId] = (appCounts[ruleId] ?: 0) + 1

        // Update last execution time
        val appTimes = lastExecutionTimes.getOrPut(packageName) { mutableMapOf() }
        appTimes[ruleId] = now
    }

    /**
     * Resets session tracking for a new app launch.
     */
    fun resetSession(packageName: String) {
        sessionCounts.remove(packageName)
        appLaunchTimes[packageName] = System.currentTimeMillis()
    }

    /**
     * Clears all tracking data.
     */
    fun clearAll() {
        sessionCounts.clear()
        lastExecutionTimes.clear()
        appLaunchTimes.clear()
    }

    companion object {
        private const val DEFAULT_MAX_TRIGGERS_PER_SESSION = 5
    }
}
