package com.nzsk.pureskip.settings

object SettingsMigration {

    private const val OLD_PREFIX = "app_experimental_"
    private const val NEW_PREFIX = "app_enhanced_"

    fun enhancedValues(allValues: Map<String, *>): Map<String, Boolean> {
        return allValues.mapNotNull { (key, value) ->
            if (key.startsWith(OLD_PREFIX) && value is Boolean) {
                NEW_PREFIX + key.removePrefix(OLD_PREFIX) to value
            } else {
                null
            }
        }.toMap()
    }

    fun disableLegacyLearnedRules(
        rules: List<LearnedPopupRule>
    ): List<LearnedPopupRule> {
        return rules.map { rule ->
            rule.copy(enabled = false, autoEligible = false, coordinateVersion = 1)
        }
    }
}
