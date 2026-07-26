package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateGeometryPolicyTest {

    @Test
    fun `accepts compact clickable button parent`() {
        val child = IntBounds(900, 120, 960, 170)
        val parent = IntBounds(875, 95, 985, 195)

        assertTrue(
            CandidateGeometryPolicy.isSafeClickableParent(
                child = child,
                parent = parent,
                screenWidth = 1080,
                screenHeight = 2400
            )
        )
    }

    @Test
    fun `rejects whole advertisement card as clickable parent`() {
        val closeGlyph = IntBounds(760, 340, 820, 400)
        val advertisementCard = IntBounds(120, 300, 830, 1700)

        assertFalse(
            CandidateGeometryPolicy.isSafeClickableParent(
                child = closeGlyph,
                parent = advertisementCard,
                screenWidth = 1080,
                screenHeight = 2400
            )
        )
    }

    @Test
    fun `recognizes close glyph at top right of centered popup`() {
        val closeGlyph = IntBounds(760, 340, 820, 400)
        val popup = IntBounds(120, 300, 830, 1700)

        assertTrue(
            CandidateGeometryPolicy.isNearPopupCloseCorner(
                child = closeGlyph,
                ancestor = popup,
                screenWidth = 1080,
                screenHeight = 2400
            )
        )
    }

    @Test
    fun `does not treat full screen root as popup`() {
        val closeGlyph = IntBounds(1000, 40, 1060, 100)
        val fullScreen = IntBounds(0, 0, 1080, 2400)

        assertFalse(
            CandidateGeometryPolicy.isNearPopupCloseCorner(
                child = closeGlyph,
                ancestor = fullScreen,
                screenWidth = 1080,
                screenHeight = 2400
            )
        )
    }
}
