package com.nzsk.pureskip.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanExecutionLimiterTest {

    @Test
    fun `event storm cannot execute more than one tree scan per interval`() {
        val limiter = ScanExecutionLimiter(minIntervalMs = 400L)
        val accepted = (0L until 2_000L step 25L)
            .count(limiter::tryAcquire)

        assertEquals(5, accepted)
    }

    @Test
    fun `default limiter keeps sustained scanning below two times per second`() {
        val limiter = ScanExecutionLimiter()
        val accepted = (0L until 2_000L step 25L)
            .count(limiter::tryAcquire)

        assertTrue(accepted <= 3)
    }

    @Test
    fun `reset permits an immediate scan for a newly opened app`() {
        val limiter = ScanExecutionLimiter(minIntervalMs = 400L)
        assertTrue(limiter.tryAcquire(1_000L))

        limiter.reset()

        assertTrue(limiter.tryAcquire(1_050L))
    }
}
