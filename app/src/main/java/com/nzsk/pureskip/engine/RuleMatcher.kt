package com.nzsk.pureskip.engine

import android.view.accessibility.AccessibilityNodeInfo
import com.nzsk.pureskip.rules.AdRule
import com.nzsk.pureskip.rules.RuleProvider

class RuleMatcher {

    private val candidateIdentifier = CandidateIdentifier()

    data class MatchResult(
        val matched: Boolean,
        val rule: AdRule?,
        val targetNode: AccessibilityNodeInfo?,
        val matchedSignals: List<String>,
        val reason: String,
        val confidence: Double = 0.0
    )

    fun match(
        packageName: String,
        rootNode: AccessibilityNodeInfo?,
        enhancedEnabled: Boolean,
        activityName: String,
        windowTitle: String,
        screenWidthPx: Int,
        screenHeightPx: Int
    ): MatchResult {
        if (rootNode == null) return noMatch("根节点为空")

        val rules = RuleProvider.getRulesForPackage(packageName)
            .filter { !it.isExperimental || enhancedEnabled }
        if (rules.isEmpty()) return noMatch("无适用规则")

        val context = CandidateIdentifier.WindowContext(
            activityName = activityName,
            windowTitle = windowTitle,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx
        )

        for (rule in rules) {
            val candidate = candidateIdentifier.findCandidates(
                rootNode = rootNode,
                conditions = rule.conditions,
                windowContext = context,
                requireConfidence = rule.isExperimental
            )
            val node = candidate.node ?: continue
            if (rule.action.requireClickable && !node.isClickable) continue
            return MatchResult(
                matched = true,
                rule = rule,
                targetNode = node,
                matchedSignals = candidate.matchedSignals,
                reason = "匹配成功",
                confidence = candidate.confidence
            )
        }
        return noMatch("未匹配到高置信度控件")
    }

    private fun noMatch(reason: String) = MatchResult(
        matched = false,
        rule = null,
        targetNode = null,
        matchedSignals = emptyList(),
        reason = reason
    )
}
