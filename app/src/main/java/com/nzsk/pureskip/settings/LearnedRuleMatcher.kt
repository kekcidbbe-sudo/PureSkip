package com.nzsk.pureskip.settings

object LearnedRuleMatcher {

    fun matchesAutomatic(
        rule: LearnedPopupRule,
        orientation: Int,
        activityName: String,
        windowTitle: String,
        hierarchyFingerprint: String
    ): Boolean {
        return rule.enabled &&
            rule.autoEligible &&
            rule.orientation == orientation &&
            rule.hierarchyFingerprint == hierarchyFingerprint &&
            (rule.activityName.isBlank() || rule.activityName == activityName) &&
            (rule.windowTitle.isBlank() || rule.windowTitle == windowTitle)
    }
}
