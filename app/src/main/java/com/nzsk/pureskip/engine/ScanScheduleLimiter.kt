package com.nzsk.pureskip.engine

/** Prevents repeated accessibility events from scheduling duplicate tree scans. */
class ScanScheduleLimiter(
    private val slotSizeMs: Long = DEFAULT_SLOT_SIZE_MS
) {
    private val reservedSlots = mutableSetOf<Long>()

    init {
        require(slotSizeMs > 0L)
    }

    fun reserve(nowMs: Long, delayMs: Long): Long? {
        val safeNow = nowMs.coerceAtLeast(0L)
        val safeDelay = delayMs.coerceAtLeast(0L)
        val deadline = if (safeDelay > Long.MAX_VALUE - safeNow) {
            Long.MAX_VALUE
        } else {
            safeNow + safeDelay
        }
        val slot = deadline / slotSizeMs
        return slot.takeIf(reservedSlots::add)
    }

    fun release(slot: Long) {
        reservedSlots.remove(slot)
    }

    fun clear() {
        reservedSlots.clear()
    }

    companion object {
        private const val DEFAULT_SLOT_SIZE_MS = 200L
    }
}
