package com.nzsk.pureskip.accessibility

data class FloatPoint(val x: Float, val y: Float)

object TeachingDragGeometry {

    fun clampTopLeftForCenter(
        desiredCenterX: Float,
        desiredCenterY: Float,
        viewWidth: Int,
        viewHeight: Int,
        parentWidth: Int,
        parentHeight: Int
    ): FloatPoint {
        val halfWidth = viewWidth / 2f
        val halfHeight = viewHeight / 2f
        val centerX = desiredCenterX.coerceIn(0f, parentWidth.coerceAtLeast(0).toFloat())
        val centerY = desiredCenterY.coerceIn(0f, parentHeight.coerceAtLeast(0).toFloat())
        return FloatPoint(centerX - halfWidth, centerY - halfHeight)
    }

    fun toScreenPoint(
        localCenterX: Float,
        localCenterY: Float,
        overlayScreenX: Int,
        overlayScreenY: Int,
        screenWidth: Int,
        screenHeight: Int
    ): FloatPoint {
        return FloatPoint(
            x = (overlayScreenX + localCenterX).coerceIn(0f, screenWidth.toFloat()),
            y = (overlayScreenY + localCenterY).coerceIn(0f, screenHeight.toFloat())
        )
    }
}
