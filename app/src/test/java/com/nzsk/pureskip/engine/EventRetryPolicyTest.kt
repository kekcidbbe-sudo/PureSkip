package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventRetryPolicyTest {

    @Test
    fun `standard window state receives early and late retries`() {
        val plan = EventRetryPolicy.forWindowState(enhanced = false)

        assertTrue(plan.cancelPending)
        assertTrue(plan.delaysMs.size >= 3)
        assertTrue(plan.delaysMs.minOrNull()!! <= 100L)
        assertTrue(plan.delaysMs.maxOrNull()!! >= 700L)
    }

    @Test
    fun `windows changed never postpones existing work`() {
        val plan = EventRetryPolicy.forWindowsChanged(enhanced = false)

        assertFalse(plan.cancelPending)
        assertTrue(plan.delaysMs.first() <= 100L)
    }

    @Test
    fun `enhanced content changes scan immediately and cover slow ad loading`() {
        val plan = EventRetryPolicy.forContentChanged(enhanced = true)

        assertFalse(plan.cancelPending)
        assertTrue(plan.delaysMs.size >= 4)
        assertTrue(plan.delaysMs.first() == 0L)
        assertTrue(plan.delaysMs.last() >= 1_800L)
    }

    @Test
    fun `interaction retries cover ads opened after a user click`() {
        val plan = EventRetryPolicy.forInteraction(enhanced = true)

        assertFalse(plan.cancelPending)
        assertTrue(plan.delaysMs.first() <= 100L)
        assertTrue(plan.delaysMs.last() >= 1_500L)
    }
}
