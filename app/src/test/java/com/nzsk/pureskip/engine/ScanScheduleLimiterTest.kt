package com.nzsk.pureskip.engine

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScanScheduleLimiterTest {

    @Test
    fun `duplicate scan deadlines in one frame window are coalesced`() {
        val limiter = ScanScheduleLimiter(slotSizeMs = 200L)

        val first = limiter.reserve(nowMs = 1_000L, delayMs = 500L)
        val duplicate = limiter.reserve(nowMs = 1_050L, delayMs = 430L)

        assertNotNull(first)
        assertNull(duplicate)
    }

    @Test
    fun `a time slot can be reused after its scan executes`() {
        val limiter = ScanScheduleLimiter(slotSizeMs = 200L)
        val slot = checkNotNull(limiter.reserve(nowMs = 1_000L, delayMs = 500L))

        limiter.release(slot)

        assertNotNull(limiter.reserve(nowMs = 1_020L, delayMs = 480L))
    }
}
