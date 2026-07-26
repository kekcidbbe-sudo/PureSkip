package com.nzsk.pureskip.engine

data class RetryPlan(
    val cancelPending: Boolean,
    val delaysMs: LongArray
)

/**
 * Retry schedules tuned for ad-skip coverage vs. thermal balance.
 *
 * v1.9.x reduced retry counts to lower CPU usage (see 1.9.0-1.9.3 doc).
 * The schedules below keep enough retry depth for slow ads while staying
 * within thermal budgets proven by 1.8.3 field tests.
 */
object EventRetryPolicy {

    fun forWindowState(enhanced: Boolean): RetryPlan {
        return RetryPlan(
            cancelPending = true,
            delaysMs = if (enhanced) {
                // Enhanced: balanced 3-point retry (was [100, 400, 1000] in v1.9.0)
                longArrayOf(100L, 400L, 1_000L)
            } else {
                // Standard: 3-point retry covering first ~1s after launch window opens
                // Restored from v1.8.1 spec (50/300/800) with slight rounding
                longArrayOf(100L, 300L, 800L)
            }
        )
    }

    fun forWindowsChanged(enhanced: Boolean): RetryPlan {
        return RetryPlan(
            cancelPending = false,
            delaysMs = if (enhanced) longArrayOf(100L, 500L) else longArrayOf(0L)
        )
    }

    fun forContentChanged(enhanced: Boolean): RetryPlan {
        return RetryPlan(
            cancelPending = false,
            delaysMs = if (enhanced) {
                // Enhanced: 4-point retry spanning 2s for slow-loading ads
                // First scan at 0ms, then progressively delayed
                longArrayOf(0L, 500L, 1_000L, 2_000L)
            } else {
                longArrayOf(0L)
            }
        )
    }

    fun forInteraction(enhanced: Boolean): RetryPlan {
        return RetryPlan(
            cancelPending = false,
            delaysMs = if (enhanced) {
                // Enhanced: post-click retry; cover 1.8s to catch ads opened after click
                longArrayOf(100L, 600L, 1_800L)
            } else {
                longArrayOf(100L, 500L)
            }
        )
    }
}
