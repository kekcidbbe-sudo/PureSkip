package com.nzsk.pureskip.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class DragFrameCoalescerTest {

    @Test
    fun `many move events schedule one frame and render the latest point`() {
        val postedFrames = mutableListOf<Runnable>()
        val rendered = mutableListOf<FloatPoint>()
        val coalescer = DragFrameCoalescer(
            scheduleFrame = { postedFrames += it },
            render = { x, y -> rendered += FloatPoint(x, y) }
        )

        coalescer.submit(10f, 20f)
        coalescer.submit(30f, 40f)
        coalescer.submit(50f, 60f)

        assertEquals(1, postedFrames.size)
        assertEquals(0, rendered.size)

        postedFrames.single().run()

        assertEquals(listOf(FloatPoint(50f, 60f)), rendered)
    }

    @Test
    fun `flush renders final finger position without waiting for another frame`() {
        val postedFrames = mutableListOf<Runnable>()
        val rendered = mutableListOf<FloatPoint>()
        val coalescer = DragFrameCoalescer(
            scheduleFrame = { postedFrames += it },
            render = { x, y -> rendered += FloatPoint(x, y) }
        )

        coalescer.submit(100f, 200f)
        coalescer.flush()
        postedFrames.single().run()

        assertEquals(listOf(FloatPoint(100f, 200f)), rendered)
    }
}
