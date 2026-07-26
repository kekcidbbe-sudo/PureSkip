package com.nzsk.pureskip.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateScorerTest {

    private val scorer = CandidateScorer()

    @Test
    fun `uses relative bounds instead of fixed screen size`() {
        val candidate = CandidateFeatures(
            signalCount = 2,
            widthPx = 60,
            heightPx = 60,
            screenWidthPx = 720,
            screenHeightPx = 1280,
            leftPx = 640,
            topPx = 30,
            textLength = 1,
            clickable = true
        )

        assertTrue(scorer.score(candidate) >= CandidateScorer.MIN_CONFIDENCE)
    }

    @Test
    fun `rejects ambiguous top candidates`() {
        assertFalse(scorer.isConfident(bestScore = 210.0, secondScore = 205.0))
        assertTrue(scorer.isConfident(bestScore = 230.0, secondScore = 180.0))
    }
}
