package com.nzsk.pureskip.settings

enum class RecognitionMode {
    STANDARD,
    SMART_ENHANCED,
    LEARNED
}

enum class EnhancementScope {
    STARTUP_ONLY,
    FULL_TIME
}

data class AppPolicy(
    val packageName: String,
    val standardEnabled: Boolean,
    val enhancementEnabled: Boolean,
    val recognitionMode: RecognitionMode,
    val scope: EnhancementScope
) {
    fun shouldUseEnhancedRules(): Boolean {
        return standardEnabled && enhancementEnabled && recognitionMode != RecognitionMode.STANDARD
    }

    companion object {
        fun default(packageName: String) = AppPolicy(
            packageName = packageName,
            standardEnabled = true,
            enhancementEnabled = false,
            recognitionMode = RecognitionMode.STANDARD,
            scope = EnhancementScope.FULL_TIME
        )
    }
}

data class LearnedPopupRule(
    val ruleId: String,
    val packageName: String,
    val normalizedX: Float,
    val normalizedY: Float,
    val orientation: Int,
    val activityName: String,
    val windowTitle: String,
    val hierarchyFingerprint: String,
    val autoEligible: Boolean,
    val enabled: Boolean = true,
    val coordinateVersion: Int = CURRENT_COORDINATE_VERSION,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun screenX(screenWidth: Int): Float = normalizedX.coerceIn(0f, 1f) * screenWidth
    fun screenY(screenHeight: Int): Float = normalizedY.coerceIn(0f, 1f) * screenHeight

    fun hasCompatibleCoordinates(): Boolean = coordinateVersion >= CURRENT_COORDINATE_VERSION

    companion object {
        const val CURRENT_COORDINATE_VERSION = 2
    }
}

data class ObservedApp(
    val packageName: String,
    val lastSeenAt: Long
)

enum class RecognitionOutcomeCode {
    SUCCESS,
    ROOT_UNAVAILABLE,
    NO_RULE,
    NO_CANDIDATE,
    LOW_CONFIDENCE,
    SAFETY_BLOCKED,
    ACTION_FAILED,
    USER_CONFIRMATION_REQUIRED
}

data class DiagnosticEntry(
    val timestamp: Long,
    val packageName: String,
    val eventType: Int,
    val nodeCount: Int,
    val ruleId: String,
    val confidence: Double,
    val outcome: RecognitionOutcomeCode
)
