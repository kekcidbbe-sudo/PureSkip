package com.nzsk.pureskip.engine

data class CandidateFeatures(
    val signalCount: Int,
    val widthPx: Int,
    val heightPx: Int,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val leftPx: Int,
    val topPx: Int,
    val textLength: Int?,
    val clickable: Boolean,
    val nearPopupCorner: Boolean = false
)

class CandidateScorer {

    fun score(features: CandidateFeatures): Double {
        if (features.screenWidthPx <= 0 || features.screenHeightPx <= 0) return 0.0
        if (features.widthPx <= 0 || features.heightPx <= 0) return 0.0

        var score = features.signalCount * 100.0
        val widthRatio = features.widthPx.toDouble() / features.screenWidthPx
        val heightRatio = features.heightPx.toDouble() / features.screenHeightPx
        val areaRatio = widthRatio * heightRatio

        score += when {
            areaRatio <= 0.008 -> 60.0
            areaRatio <= 0.025 -> 30.0
            areaRatio <= 0.06 -> 10.0
            else -> -40.0
        }

        score += when (features.textLength) {
            null -> 0.0
            in 0..4 -> 30.0
            in 5..8 -> 15.0
            else -> 0.0
        }

        val rightPx = features.leftPx + features.widthPx
        val bottomPx = features.topPx + features.heightPx
        val nearEdge = features.leftPx < features.screenWidthPx * 0.15 ||
            rightPx > features.screenWidthPx * 0.85 ||
            features.topPx < features.screenHeightPx * 0.12 ||
            bottomPx > features.screenHeightPx * 0.88
        if (nearEdge) score += 25.0
        if (features.nearPopupCorner) score += 25.0
        if (features.clickable) score += 15.0
        return score
    }

    fun isConfident(bestScore: Double, secondScore: Double?): Boolean {
        if (bestScore < MIN_CONFIDENCE) return false
        return secondScore == null || bestScore - secondScore >= MIN_SCORE_MARGIN
    }

    companion object {
        const val MIN_CONFIDENCE = 155.0
        const val MIN_SCORE_MARGIN = 25.0
    }
}
