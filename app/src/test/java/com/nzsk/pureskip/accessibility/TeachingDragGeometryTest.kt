package com.nzsk.pureskip.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class TeachingDragGeometryTest {

    @Test
    fun `crosshair center can reach every screen edge`() {
        val topLeft = TeachingDragGeometry.clampTopLeftForCenter(
            desiredCenterX = 0f,
            desiredCenterY = 0f,
            viewWidth = 120,
            viewHeight = 120,
            parentWidth = 1080,
            parentHeight = 2400
        )
        val bottomRight = TeachingDragGeometry.clampTopLeftForCenter(
            desiredCenterX = 1080f,
            desiredCenterY = 2400f,
            viewWidth = 120,
            viewHeight = 120,
            parentWidth = 1080,
            parentHeight = 2400
        )

        assertEquals(-60f, topLeft.x, 0.01f)
        assertEquals(-60f, topLeft.y, 0.01f)
        assertEquals(1020f, bottomRight.x, 0.01f)
        assertEquals(2340f, bottomRight.y, 0.01f)
    }

    @Test
    fun `local pointer is converted to real screen coordinates`() {
        val point = TeachingDragGeometry.toScreenPoint(
            localCenterX = 50f,
            localCenterY = -20f,
            overlayScreenX = 0,
            overlayScreenY = 80,
            screenWidth = 1080,
            screenHeight = 2400
        )

        assertEquals(50f, point.x, 0.01f)
        assertEquals(60f, point.y, 0.01f)
    }
}
