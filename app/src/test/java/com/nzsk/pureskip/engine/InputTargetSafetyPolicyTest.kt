package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputTargetSafetyPolicyTest {

    @Test
    fun `editable and edit text nodes are unsafe click candidates`() {
        assertTrue(
            InputTargetSafetyPolicy.isUnsafeInputTarget(
                editable = true,
                className = "android.view.View"
            )
        )
        assertTrue(
            InputTargetSafetyPolicy.isUnsafeInputTarget(
                editable = false,
                className = "android.widget.EditText"
            )
        )
    }

    @Test
    fun `ordinary ad close image is not treated as an input target`() {
        assertFalse(
            InputTargetSafetyPolicy.isUnsafeInputTarget(
                editable = false,
                className = "android.widget.ImageView"
            )
        )
    }
}
