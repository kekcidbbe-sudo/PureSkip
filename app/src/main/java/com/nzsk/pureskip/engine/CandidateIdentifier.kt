package com.nzsk.pureskip.engine

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.nzsk.pureskip.rules.ConditionType
import com.nzsk.pureskip.rules.MatchType
import com.nzsk.pureskip.rules.RuleCondition

class CandidateIdentifier {

    data class WindowContext(
        val activityName: String = "",
        val windowTitle: String = "",
        val screenWidthPx: Int,
        val screenHeightPx: Int
    )

    data class CandidateResult(
        val node: AccessibilityNodeInfo?,
        val candidateCount: Int,
        val matchedSignals: List<String>,
        val confidence: Double,
        val ambiguous: Boolean
    )

    private data class NodeCandidate(
        val node: AccessibilityNodeInfo,
        val signals: Set<String>,
        val score: Double
    )

    private val scorer = CandidateScorer()

    fun findCandidates(
        rootNode: AccessibilityNodeInfo,
        conditions: List<RuleCondition>,
        windowContext: WindowContext,
        requireConfidence: Boolean
    ): CandidateResult {
        if (conditions.isEmpty()) return emptyResult()

        val windowConditions = conditions.filter {
            it.type == ConditionType.ACTIVITY_NAME || it.type == ConditionType.WINDOW_TITLE
        }
        if (windowConditions.any { !matchesWindowCondition(it, windowContext) }) {
            return emptyResult()
        }

        val nodeConditions = conditions - windowConditions.toSet()
        if (nodeConditions.isEmpty()) return emptyResult()

        val candidates = mutableListOf<NodeCandidate>()
        collectCandidates(rootNode, nodeConditions, windowContext, candidates, 0)
        if (candidates.isEmpty()) return emptyResult()

        val sorted = candidates.sortedByDescending { it.score }
        val best = sorted.first()
        val secondScore = sorted.getOrNull(1)?.score
        val confident = !requireConfidence || scorer.isConfident(best.score, secondScore)
        if (!confident) {
            return CandidateResult(
                node = null,
                candidateCount = sorted.size,
                matchedSignals = best.signals.toList(),
                confidence = best.score,
                ambiguous = secondScore != null &&
                    best.score - secondScore < CandidateScorer.MIN_SCORE_MARGIN
            )
        }

        return CandidateResult(
            node = findClickableTarget(best.node, windowContext),
            candidateCount = sorted.size,
            matchedSignals = best.signals.toList(),
            confidence = best.score,
            ambiguous = false
        )
    }

    private fun collectCandidates(
        node: AccessibilityNodeInfo,
        conditions: List<RuleCondition>,
        context: WindowContext,
        output: MutableList<NodeCandidate>,
        depth: Int
    ) {
        if (depth > MAX_DEPTH) return

        // Whitelist: skip node and its subtree if text contains own app name "纯净跳过"
        val nodeText = node.text?.toString().orEmpty()
        val nodeDesc = node.contentDescription?.toString().orEmpty()
        if (nodeText.contains("纯净跳过") || nodeDesc.contains("纯净跳过")) {
            return
        }
        if (InputTargetSafetyPolicy.isUnsafeInputTarget(
                editable = node.isEditable,
                className = node.className?.toString()
            )
        ) {
            return
        }

        val signals = conditions
            .filter { matchesNodeCondition(node, it) }
            .map { "${it.type.name}:${it.value}" }
            .toSet()

        if (signals.isNotEmpty() && node.isVisibleToUser) {
            val bounds = Rect().also { node.getBoundsInScreen(it) }
            val textLengths = listOfNotNull(
                node.text?.toString()?.takeIf { it.isNotBlank() }?.length,
                node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.length
            )
            val features = CandidateFeatures(
                signalCount = signals.size,
                widthPx = bounds.width(),
                heightPx = bounds.height(),
                screenWidthPx = context.screenWidthPx,
                screenHeightPx = context.screenHeightPx,
                leftPx = bounds.left,
                topPx = bounds.top,
                textLength = textLengths.minOrNull(),
                clickable = node.isClickable || hasSafeClickableParent(node, context),
                nearPopupCorner = isNearPopupCloseCorner(node, context)
            )
            output += NodeCandidate(node, signals, scorer.score(features))
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectCandidates(child, conditions, context, output, depth + 1)
        }
    }

    private fun findClickableTarget(
        node: AccessibilityNodeInfo,
        context: WindowContext
    ): AccessibilityNodeInfo {
        if (node.isClickable) return node
        val childBounds = node.boundsBox()
        var current = node
        repeat(MAX_PARENT_DEPTH) {
            val parent = current.parent ?: return node
            if (parent.isClickable && parent.isVisibleToUser &&
                CandidateGeometryPolicy.isSafeClickableParent(
                    child = childBounds,
                    parent = parent.boundsBox(),
                    screenWidth = context.screenWidthPx,
                    screenHeight = context.screenHeightPx
                )
            ) {
                return parent
            }
            current = parent
        }
        return node
    }

    private fun hasSafeClickableParent(
        node: AccessibilityNodeInfo,
        context: WindowContext
    ): Boolean {
        val childBounds = node.boundsBox()
        var current = node
        repeat(MAX_PARENT_DEPTH) {
            val parent = current.parent ?: return false
            if (parent.isClickable && parent.isVisibleToUser &&
                CandidateGeometryPolicy.isSafeClickableParent(
                    child = childBounds,
                    parent = parent.boundsBox(),
                    screenWidth = context.screenWidthPx,
                    screenHeight = context.screenHeightPx
                )
            ) {
                return true
            }
            current = parent
        }
        return false
    }

    private fun isNearPopupCloseCorner(
        node: AccessibilityNodeInfo,
        context: WindowContext
    ): Boolean {
        val childBounds = node.boundsBox()
        var current = node
        repeat(MAX_PARENT_DEPTH) {
            val parent = current.parent ?: return false
            if (CandidateGeometryPolicy.isNearPopupCloseCorner(
                    child = childBounds,
                    ancestor = parent.boundsBox(),
                    screenWidth = context.screenWidthPx,
                    screenHeight = context.screenHeightPx
                )
            ) {
                return true
            }
            current = parent
        }
        return false
    }

    private fun AccessibilityNodeInfo.boundsBox(): IntBounds {
        val bounds = Rect().also { getBoundsInScreen(it) }
        return IntBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    private fun matchesWindowCondition(
        condition: RuleCondition,
        context: WindowContext
    ): Boolean {
        val actual = when (condition.type) {
            ConditionType.ACTIVITY_NAME -> context.activityName
            ConditionType.WINDOW_TITLE -> context.windowTitle
            else -> return true
        }
        return matchValue(actual, condition)
    }

    private fun matchesNodeCondition(
        node: AccessibilityNodeInfo,
        condition: RuleCondition
    ): Boolean {
        val actual = when (condition.type) {
            ConditionType.VIEW_ID -> node.viewIdResourceName ?: ""
            ConditionType.TEXT -> node.text?.toString() ?: ""
            ConditionType.CONTENT_DESCRIPTION -> node.contentDescription?.toString() ?: ""
            ConditionType.VIEW_CLASS -> node.className?.toString() ?: ""
            ConditionType.ACTIVITY_NAME, ConditionType.WINDOW_TITLE -> return false
        }
        return matchValue(actual, condition)
    }

    private fun matchValue(actual: String, condition: RuleCondition): Boolean {
        if (actual.isBlank()) return false
        return when (condition.matchType) {
            MatchType.EXACT -> actual.equals(condition.value, ignoreCase = true)
            MatchType.CONTAINS -> actual.contains(condition.value, ignoreCase = true)
            MatchType.STARTS_WITH -> actual.startsWith(condition.value, ignoreCase = true)
            MatchType.ENDS_WITH -> actual.endsWith(condition.value, ignoreCase = true)
            MatchType.REGEX -> runCatching {
                Regex(condition.value, RegexOption.IGNORE_CASE).containsMatchIn(actual)
            }.getOrDefault(false)
        }
    }

    private fun emptyResult() = CandidateResult(
        node = null,
        candidateCount = 0,
        matchedSignals = emptyList(),
        confidence = 0.0,
        ambiguous = false
    )

    companion object {
        private const val MAX_DEPTH = 50
        private const val MAX_PARENT_DEPTH = 4
    }
}
