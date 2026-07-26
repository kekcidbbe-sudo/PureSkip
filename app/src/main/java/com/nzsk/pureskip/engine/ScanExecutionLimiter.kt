package com.nzsk.pureskip.engine

/** Caps completed accessibility-tree scans even when event callbacks arrive in bursts. */
class ScanExecutionLimiter(
    private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS
) {
    private var lastExecutionMs: Long? = null

    init {
        require(minIntervalMs > 0L)
    }

    fun tryAcquire(nowMs: Long): Boolean {
        val safeNow = nowMs.coerceAtLeast(0L)
        val last = lastExecutionMs
        if (last != null && safeNow - last < minIntervalMs) return false
        lastExecutionMs = safeNow
        return true
    }

    fun reset() {
        lastExecutionMs = null
    }

    companion object {
        // v1.8.3 field-proven: 700ms keeps background CPU to 0-2% (Pixel 7, Android 14)
        // v1.9.0 raised to 1000ms (overkill), v1.9.1 dropped to 400ms (too aggressive).
        // 700ms is the balanced middle ground.
        private const val DEFAULT_MIN_INTERVAL_MS = 700L
    }
}
