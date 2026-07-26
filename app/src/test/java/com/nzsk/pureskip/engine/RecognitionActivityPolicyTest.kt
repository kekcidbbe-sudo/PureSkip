package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionActivityPolicyTest {

    @Test
    fun `ordinary app stops content-change scanning after startup window`() {
        assertFalse(
            RecognitionActivityPolicy.shouldHandleContentChange(
                withinStartupWindow = false,
                enhancedRecognitionActive = false
            )
        )
    }

    @Test
    fun `startup and explicitly enhanced app still handle content changes`() {
        assertTrue(RecognitionActivityPolicy.shouldHandleContentChange(true, false))
        assertTrue(RecognitionActivityPolicy.shouldHandleContentChange(false, true))
    }
}
