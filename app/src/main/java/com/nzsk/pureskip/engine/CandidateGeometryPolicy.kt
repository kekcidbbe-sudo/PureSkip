package com.nzsk.pureskip.engine

data class IntBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val area: Long get() = width.toLong() * height.toLong()
    val centerX: Double get() = (left + right) / 2.0
    val centerY: Double get() = (top + bottom) / 2.0
}

object CandidateGeometryPolicy {

    fun isSafeClickableParent(
        child: IntBounds,
        parent: IntBounds,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        if (!hasValidGeometry(child, parent, screenWidth, screenHeight)) return false
        if (parent.area > child.area * MAX_PARENT_AREA_MULTIPLIER) return false
        if (parent.width > screenWidth * MAX_BUTTON_WIDTH_RATIO) return false
        if (parent.height > screenHeight * MAX_BUTTON_HEIGHT_RATIO) return false

        val maxCenterOffsetX = maxOf(child.width * 2.5, parent.width * 0.45)
        val maxCenterOffsetY = maxOf(child.height * 2.5, parent.height * 0.45)
        return kotlin.math.abs(child.centerX - parent.centerX) <= maxCenterOffsetX &&
            kotlin.math.abs(child.centerY - parent.centerY) <= maxCenterOffsetY
    }

    fun isNearPopupCloseCorner(
        child: IntBounds,
        ancestor: IntBounds,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        if (!hasValidGeometry(child, ancestor, screenWidth, screenHeight)) return false
        val screenArea = screenWidth.toLong() * screenHeight.toLong()
        if (screenArea <= 0L) return false
        val ancestorAreaRatio = ancestor.area.toDouble() / screenArea
        if (ancestorAreaRatio !in MIN_POPUP_AREA_RATIO..MAX_POPUP_AREA_RATIO) return false
        if (ancestor.width < child.width * MIN_POPUP_SIZE_MULTIPLIER) return false
        if (ancestor.height < child.height * MIN_POPUP_SIZE_MULTIPLIER) return false

        val relativeX = (child.centerX - ancestor.left) / ancestor.width
        val relativeY = (child.centerY - ancestor.top) / ancestor.height
        return relativeX >= POPUP_RIGHT_ZONE && relativeY <= POPUP_TOP_ZONE
    }

    private fun hasValidGeometry(
        child: IntBounds,
        parent: IntBounds,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        return child.area > 0L && parent.area > 0L && screenWidth > 0 && screenHeight > 0
    }

    private const val MAX_PARENT_AREA_MULTIPLIER = 16L
    private const val MAX_BUTTON_WIDTH_RATIO = 0.32
    private const val MAX_BUTTON_HEIGHT_RATIO = 0.22
    private const val MIN_POPUP_AREA_RATIO = 0.06
    private const val MAX_POPUP_AREA_RATIO = 0.82
    private const val MIN_POPUP_SIZE_MULTIPLIER = 3
    private const val POPUP_RIGHT_ZONE = 0.72
    private const val POPUP_TOP_ZONE = 0.28
}
