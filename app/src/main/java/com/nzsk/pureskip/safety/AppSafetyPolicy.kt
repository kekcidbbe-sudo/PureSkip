package com.nzsk.pureskip.safety

object AppSafetyPolicy {

    private val exactRestrictedPackages = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.vending",
        "com.android.keyguard",
        "com.android.credentials",
        "com.android.documentsui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.nzsk.pureskip",
        "com.nzsk.pureskip.debug"
    )

    fun isRestricted(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        return packageName in exactRestrictedPackages ||
            normalized.contains("launcher") ||
            normalized.contains("inputmethod") ||
            normalized.contains("keyboard") ||
            normalized.contains("packageinstaller") ||
            normalized.contains("permissioncontroller") ||
            normalized.contains("authenticator")
    }

    /**
     * Input methods are temporary windows over the current app and must not reset its
     * startup window, cooldown, or per-session click counters.
     */
    fun preservesForegroundSession(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        return normalized.contains("inputmethod") ||
            normalized.contains("keyboard") ||
            normalized == "com.android.systemui"
    }
}
