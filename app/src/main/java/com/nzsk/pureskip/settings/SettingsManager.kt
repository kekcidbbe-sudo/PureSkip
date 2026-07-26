package com.nzsk.pureskip.settings

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages all application settings and local state.
 * Uses SharedPreferences for lightweight local storage.
 */
class SettingsManager private constructor(context: Context) {

    private val context: Context = context.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    init {
        migrateSettingsIfNeeded()
    }

    // Master switch
    fun isMasterEnabled(): Boolean = prefs.getBoolean(KEY_MASTER_ENABLED, true)
    fun setMasterEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()

    // Paused state
    fun isPaused(): Boolean = prefs.getBoolean(KEY_PAUSED, false)
    fun setPaused(paused: Boolean) = prefs.edit().putBoolean(KEY_PAUSED, paused).apply()

    // Service running state (runtime only, not persisted)
    private var _isServiceRunning = false
    fun isServiceRunning(): Boolean = _isServiceRunning || isAccessibilityServiceEnabled()
    fun setServiceRunning(running: Boolean) { _isServiceRunning = running }

    /**
     * Check if accessibility service is actually enabled in system settings.
     * This persists across app restarts, unlike the runtime flag.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )
        // Service class name is fixed and does NOT have the debug suffix
        // context.packageName may be "com.nzsk.pureskip.debug" but service is "com.nzsk.pureskip.accessibility..."
        val serviceClassName = "com.nzsk.pureskip.accessibility.SkipAccessibilityService"
        return enabledServices.any {
            it.resolveInfo.serviceInfo.name == serviceClassName
        }
    }

    // Enhanced recognition master switch. Old experimental APIs remain as compatibility aliases.
    fun isEnhancementMasterEnabled(): Boolean = prefs.getBoolean(KEY_EXPERIMENTAL_ENABLED, false)
    fun setEnhancementMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EXPERIMENTAL_ENABLED, enabled).apply()
    }
    fun isExperimentalEnabled(): Boolean = isEnhancementMasterEnabled()
    fun setExperimentalEnabled(enabled: Boolean) = setEnhancementMasterEnabled(enabled)

    // Per-app settings
    fun isAppEnabled(packageName: String): Boolean {
        return prefs.getBoolean("$KEY_APP_ENABLED_PREFIX$packageName", true)
    }
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putBoolean("$KEY_APP_ENABLED_PREFIX$packageName", enabled).apply()
    }

    fun isAppBlocked(packageName: String): Boolean {
        return prefs.getBoolean("$KEY_APP_BLOCKED_PREFIX$packageName", false)
    }
    fun setAppBlocked(packageName: String, blocked: Boolean) {
        prefs.edit().putBoolean("$KEY_APP_BLOCKED_PREFIX$packageName", blocked).apply()
    }

    fun isAppEnhancedEnabled(packageName: String): Boolean {
        val key = "$KEY_APP_ENHANCED_PREFIX$packageName"
        return if (prefs.contains(key)) {
            prefs.getBoolean(key, false)
        } else {
            prefs.getBoolean("$KEY_APP_EXPERIMENTAL_PREFIX$packageName", false)
        }
    }
    fun setAppEnhancedEnabled(packageName: String, enabled: Boolean) {
        prefs.edit()
            .putBoolean("$KEY_APP_ENHANCED_PREFIX$packageName", enabled)
            .putBoolean("$KEY_APP_EXPERIMENTAL_PREFIX$packageName", enabled)
            .apply()
    }
    fun isAppExperimentalEnabled(packageName: String): Boolean = isAppEnhancedEnabled(packageName)
    fun setAppExperimentalEnabled(packageName: String, enabled: Boolean) =
        setAppEnhancedEnabled(packageName, enabled)

    fun getEnhancementScope(packageName: String): EnhancementScope {
        val value = prefs.getString("$KEY_APP_SCOPE_PREFIX$packageName", EnhancementScope.FULL_TIME.name)
        return runCatching { EnhancementScope.valueOf(value ?: EnhancementScope.FULL_TIME.name) }
            .getOrDefault(EnhancementScope.FULL_TIME)
    }

    fun setEnhancementScope(packageName: String, scope: EnhancementScope) {
        prefs.edit().putString("$KEY_APP_SCOPE_PREFIX$packageName", scope.name).apply()
    }

    fun getAppPolicy(packageName: String): AppPolicy {
        val enhanced = isAppEnhancedEnabled(packageName)
        return AppPolicy(
            packageName = packageName,
            standardEnabled = isAppEnabled(packageName),
            enhancementEnabled = enhanced,
            recognitionMode = if (enhanced) RecognitionMode.SMART_ENHANCED else RecognitionMode.STANDARD,
            scope = getEnhancementScope(packageName)
        )
    }

    // Recently observed packages. Only package name and last-seen timestamp are stored.
    @Synchronized
    fun recordObservedApp(packageName: String, timestamp: Long = System.currentTimeMillis()) {
        if (packageName.isBlank() || packageName == context.packageName) return
        val current = readJsonObject(KEY_OBSERVED_APPS)
        current.put(packageName, timestamp)

        val entries = mutableListOf<ObservedApp>()
        val keys = current.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            entries += ObservedApp(key, current.optLong(key, 0L))
        }
        val trimmed = entries.sortedByDescending { it.lastSeenAt }.take(MAX_OBSERVED_APPS)
        val output = JSONObject()
        trimmed.forEach { output.put(it.packageName, it.lastSeenAt) }
        prefs.edit().putString(KEY_OBSERVED_APPS, output.toString()).apply()
    }

    fun getObservedApps(): List<ObservedApp> {
        val current = readJsonObject(KEY_OBSERVED_APPS)
        val result = mutableListOf<ObservedApp>()
        val keys = current.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result += ObservedApp(key, current.optLong(key, 0L))
        }
        return result.sortedByDescending { it.lastSeenAt }
    }

    fun clearObservedApps() {
        prefs.edit().remove(KEY_OBSERVED_APPS).apply()
    }

    // User consent gate for broad installed-app visibility.
    fun isAllAppsConsentGiven(): Boolean = prefs.getBoolean(KEY_ALL_APPS_CONSENT, false)
    fun setAllAppsConsentGiven(given: Boolean) {
        prefs.edit().putBoolean(KEY_ALL_APPS_CONSENT, given).apply()
    }

    // Manual teaching hand-off between the app UI and accessibility service.
    fun getPendingTeachingPackage(): String? = prefs.getString(KEY_PENDING_TEACHING_PACKAGE, null)
    fun setPendingTeachingPackage(packageName: String?) {
        val edit = prefs.edit()
        if (packageName == null) edit.remove(KEY_PENDING_TEACHING_PACKAGE)
        else edit.putString(KEY_PENDING_TEACHING_PACKAGE, packageName)
        edit.apply()
    }

    fun consumeLegacyTeachingMigrationNotice(): Boolean {
        val shouldShow = prefs.getBoolean(KEY_LEGACY_TEACHING_NOTICE, false)
        if (shouldShow) prefs.edit().remove(KEY_LEGACY_TEACHING_NOTICE).apply()
        return shouldShow
    }

    @Synchronized
    fun getLearnedRules(packageName: String? = null): List<LearnedPopupRule> {
        val array = readJsonArray(KEY_LEARNED_RULES)
        val result = mutableListOf<LearnedPopupRule>()
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            val rule = jsonToLearnedRule(json) ?: continue
            if (packageName == null || rule.packageName == packageName) result += rule
        }
        return result
    }

    @Synchronized
    fun saveLearnedRule(rule: LearnedPopupRule) {
        val rules = getLearnedRules().filterNot { it.ruleId == rule.ruleId } + rule
        writeLearnedRules(rules)
    }

    @Synchronized
    fun deleteLearnedRule(ruleId: String) {
        writeLearnedRules(getLearnedRules().filterNot { it.ruleId == ruleId })
    }

    @Synchronized
    fun setLearnedRuleEnabled(ruleId: String, enabled: Boolean) {
        writeLearnedRules(getLearnedRules().map {
            if (it.ruleId == ruleId) it.copy(enabled = enabled) else it
        })
    }

    // Privacy-preserving local diagnostics. No text, screenshots, account data, or input is stored.
    fun isDiagnosticsEnabled(): Boolean = prefs.getBoolean(KEY_DIAGNOSTICS_ENABLED, false)
    fun setDiagnosticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIAGNOSTICS_ENABLED, enabled).apply()
    }

    @Synchronized
    fun addDiagnostic(entry: DiagnosticEntry) {
        if (!isDiagnosticsEnabled()) return
        val entries = getDiagnostics().toMutableList()
        entries += entry
        writeDiagnostics(entries.takeLast(MAX_DIAGNOSTIC_ENTRIES))
    }

    fun getDiagnostics(): List<DiagnosticEntry> {
        val array = readJsonArray(KEY_DIAGNOSTICS)
        val result = mutableListOf<DiagnosticEntry>()
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            val outcome = runCatching {
                RecognitionOutcomeCode.valueOf(json.optString("outcome"))
            }.getOrNull() ?: continue
            result += DiagnosticEntry(
                timestamp = json.optLong("timestamp"),
                packageName = json.optString("packageName"),
                eventType = json.optInt("eventType"),
                nodeCount = json.optInt("nodeCount"),
                ruleId = json.optString("ruleId"),
                confidence = json.optDouble("confidence"),
                outcome = outcome
            )
        }
        return result
    }

    fun clearDiagnostics() {
        prefs.edit().remove(KEY_DIAGNOSTICS).apply()
    }

    fun exportDiagnostics(): String = getDiagnostics().joinToString("\n") {
        "${it.timestamp}\t${it.packageName}\tevent=${it.eventType}\tnodes=${it.nodeCount}" +
            "\trule=${it.ruleId}\tconfidence=${"%.1f".format(it.confidence)}\t${it.outcome.name}"
    }

    // Skip count tracking
    fun getSkipCount(): Int = prefs.getInt(KEY_SKIP_COUNT, 0)
    fun incrementSkipCount() {
        prefs.edit().putInt(KEY_SKIP_COUNT, getSkipCount() + 1).apply()
    }
    fun clearSkipCount() {
        prefs.edit().putInt(KEY_SKIP_COUNT, 0).apply()
    }

    // First launch tracking
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    // Show count on main screen
    fun isShowCountEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_COUNT, true)
    fun setShowCountEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_COUNT, enabled).apply()
    }

    // Registered apps tracking
    fun getRegisteredApps(): Set<String> {
        return prefs.getStringSet(KEY_REGISTERED_APPS, emptySet()) ?: emptySet()
    }
    fun addRegisteredApp(packageName: String) {
        val apps = getRegisteredApps().toMutableSet()
        apps.add(packageName)
        prefs.edit().putStringSet(KEY_REGISTERED_APPS, apps).apply()
    }

    // Clear all data
    fun clearAllData() {
        prefs.edit().clear().apply()
    }

    // Reset to defaults
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        setMasterEnabled(true)
        setPaused(false)
        setEnhancementMasterEnabled(false)
        setShowCountEnabled(true)
        prefs.edit().putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION).apply()
    }

    private fun readJsonObject(key: String): JSONObject {
        return runCatching { JSONObject(prefs.getString(key, "{}") ?: "{}") }
            .getOrDefault(JSONObject())
    }

    private fun readJsonArray(key: String): JSONArray {
        return runCatching { JSONArray(prefs.getString(key, "[]") ?: "[]") }
            .getOrDefault(JSONArray())
    }

    private fun jsonToLearnedRule(json: JSONObject): LearnedPopupRule? {
        val ruleId = json.optString("ruleId")
        val packageName = json.optString("packageName")
        if (ruleId.isBlank() || packageName.isBlank()) return null
        return LearnedPopupRule(
            ruleId = ruleId,
            packageName = packageName,
            normalizedX = json.optDouble("normalizedX", 0.5).toFloat(),
            normalizedY = json.optDouble("normalizedY", 0.5).toFloat(),
            orientation = json.optInt("orientation", 1),
            activityName = json.optString("activityName"),
            windowTitle = json.optString("windowTitle"),
            hierarchyFingerprint = json.optString("hierarchyFingerprint"),
            autoEligible = json.optBoolean("autoEligible", false),
            enabled = json.optBoolean("enabled", true),
            coordinateVersion = json.optInt("coordinateVersion", 1),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun writeLearnedRules(rules: List<LearnedPopupRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(JSONObject().apply {
                put("ruleId", rule.ruleId)
                put("packageName", rule.packageName)
                put("normalizedX", rule.normalizedX.toDouble())
                put("normalizedY", rule.normalizedY.toDouble())
                put("orientation", rule.orientation)
                put("activityName", rule.activityName)
                put("windowTitle", rule.windowTitle)
                put("hierarchyFingerprint", rule.hierarchyFingerprint)
                put("autoEligible", rule.autoEligible)
                put("enabled", rule.enabled)
                put("coordinateVersion", rule.coordinateVersion)
                put("createdAt", rule.createdAt)
            })
        }
        prefs.edit().putString(KEY_LEARNED_RULES, array.toString()).apply()
    }

    private fun writeDiagnostics(entries: List<DiagnosticEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("timestamp", entry.timestamp)
                put("packageName", entry.packageName)
                put("eventType", entry.eventType)
                put("nodeCount", entry.nodeCount)
                put("ruleId", entry.ruleId)
                put("confidence", entry.confidence)
                put("outcome", entry.outcome.name)
            })
        }
        prefs.edit().putString(KEY_DIAGNOSTICS, array.toString()).apply()
    }

    private fun migrateSettingsIfNeeded() {
        val currentVersion = prefs.getInt(KEY_SCHEMA_VERSION, 1)
        if (currentVersion >= CURRENT_SCHEMA_VERSION) return

        val edit = prefs.edit()
        if (currentVersion < 2) {
            SettingsMigration.enhancedValues(prefs.all).forEach { (key, value) ->
                edit.putBoolean(key, value)
            }
        }
        if (currentVersion < 3) {
            val legacyRules = getLearnedRules()
            if (legacyRules.any { it.enabled }) {
                writeLearnedRules(SettingsMigration.disableLegacyLearnedRules(legacyRules))
                edit.putBoolean(KEY_LEGACY_TEACHING_NOTICE, true)
            }
        }
        edit.putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION).apply()
    }

    companion object {
        private const val PREFS_NAME = "pureskip_settings"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_PAUSED = "paused"
        private const val KEY_EXPERIMENTAL_ENABLED = "experimental_enabled"
        private const val KEY_APP_ENABLED_PREFIX = "app_enabled_"
        private const val KEY_APP_BLOCKED_PREFIX = "app_blocked_"
        private const val KEY_APP_EXPERIMENTAL_PREFIX = "app_experimental_"
        private const val KEY_APP_ENHANCED_PREFIX = "app_enhanced_"
        private const val KEY_APP_SCOPE_PREFIX = "app_enhancement_scope_"
        private const val KEY_SKIP_COUNT = "skip_count"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SHOW_COUNT = "show_count"
        private const val KEY_REGISTERED_APPS = "registered_apps"
        private const val KEY_OBSERVED_APPS = "observed_apps_json"
        private const val KEY_ALL_APPS_CONSENT = "all_apps_consent"
        private const val KEY_PENDING_TEACHING_PACKAGE = "pending_teaching_package"
        private const val KEY_LEARNED_RULES = "learned_rules_json"
        private const val KEY_LEGACY_TEACHING_NOTICE = "legacy_teaching_reteach_notice"
        private const val KEY_DIAGNOSTICS_ENABLED = "diagnostics_enabled"
        private const val KEY_DIAGNOSTICS = "diagnostics_json"
        private const val KEY_SCHEMA_VERSION = "settings_schema_version"
        private const val CURRENT_SCHEMA_VERSION = 3
        private const val MAX_OBSERVED_APPS = 100
        private const val MAX_DIAGNOSTIC_ENTRIES = 100

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
