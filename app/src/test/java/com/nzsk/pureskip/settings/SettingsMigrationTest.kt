package com.nzsk.pureskip.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMigrationTest {

    @Test
    fun migratesOnlyOldPerAppExperimentalSwitches() {
        val migrated = SettingsMigration.enhancedValues(
            mapOf(
                "app_experimental_com.example.one" to true,
                "app_experimental_com.example.two" to false,
                "master_enabled" to true,
                "unrelated" to "value"
            )
        )

        assertEquals(
            mapOf(
                "app_enhanced_com.example.one" to true,
                "app_enhanced_com.example.two" to false
            ),
            migrated
        )
    }

    @Test
    fun disablesLegacyLearnedRulesWithOldCoordinateSystem() {
        val legacy = LearnedPopupRule(
            ruleId = "legacy",
            packageName = "com.example.app",
            normalizedX = 0.9f,
            normalizedY = 0.1f,
            orientation = 1,
            activityName = "ExampleActivity",
            windowTitle = "",
            hierarchyFingerprint = "old",
            autoEligible = true,
            enabled = true
        )

        val migrated = SettingsMigration.disableLegacyLearnedRules(listOf(legacy))

        assertEquals(false, migrated.single().enabled)
        assertEquals(false, migrated.single().autoEligible)
        assertEquals(1, migrated.single().coordinateVersion)
        assertEquals(false, migrated.single().hasCompatibleCoordinates())
    }
}
