package com.nzsk.pureskip.safety

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyLimiterTest {

    @Test
    fun `honors each rules trigger limit`() {
        val limiter = FrequencyLimiter()
        val pkg = "com.example.indie"
        val rule = "rule-1"

        assertTrue(limiter.canExecute(pkg, rule, maxTriggers = 2))
        limiter.recordExecution(pkg, rule)
        assertTrue(limiter.canExecute(pkg, rule, maxTriggers = 2))
        limiter.recordExecution(pkg, rule)
        assertFalse(limiter.canExecute(pkg, rule, maxTriggers = 2))
    }
}
