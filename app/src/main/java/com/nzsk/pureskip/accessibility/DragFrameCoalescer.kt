package com.nzsk.pureskip.accessibility

/**
 * Collapses high-frequency pointer events into at most one visual update per display frame.
 */
class DragFrameCoalescer(
    private val scheduleFrame: (Runnable) -> Unit,
    private val render: (Float, Float) -> Unit
) {
    private var latestX = 0f
    private var latestY = 0f
    private var pending = false
    private var frameScheduled = false

    private val frameCallback = Runnable {
        if (!frameScheduled) return@Runnable
        frameScheduled = false
        renderPending()
    }

    fun submit(x: Float, y: Float) {
        latestX = x
        latestY = y
        pending = true
        if (!frameScheduled) {
            frameScheduled = true
            scheduleFrame(frameCallback)
        }
    }

    fun flush() {
        frameScheduled = false
        renderPending()
    }

    fun cancel() {
        frameScheduled = false
        pending = false
    }

    private fun renderPending() {
        if (!pending) return
        pending = false
        render(latestX, latestY)
    }
}
