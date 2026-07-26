package com.nzsk.pureskip.engine

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEventPolicyTest {

    @Test
    fun `click can trigger a delayed recognition scan`() {
        assertTrue(
            AccessibilityEventPolicy.shouldScheduleInteraction(
                AccessibilityEvent.TYPE_VIEW_CLICKED
            )
        )
    }

    @Test
    fun `scroll does not trigger recognition scans`() {
        assertFalse(
            AccessibilityEventPolicy.shouldScheduleInteraction(
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            )
        )
    }
}
