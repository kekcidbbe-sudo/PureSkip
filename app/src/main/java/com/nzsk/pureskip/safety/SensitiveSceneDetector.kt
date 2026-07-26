package com.nzsk.pureskip.safety

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects sensitive scenes where ad-clicking should never occur.
 * Includes password fields, payment pages, system dialogs, etc.
 */
class SensitiveSceneDetector {

    // Known sensitive package names (system/security apps)
    private val sensitivePackages = setOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.certinstaller",
        "com.android.credentials",
        "com.android.keyguard",
        "com.android.systemui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.mms",
        "com.google.android.apps.messaging",
        // Password managers and authenticators
        "com.google.android.apps.authenticator2",
        "com.authy.authy",
        "com.lastpass.lpandroid",
        "com.agilebits.onepassword",
        // Launchers (home screens) - never click anything here
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.h.launcher",
        // File managers and downloads
        "com.android.documentsui",
        "com.android.providers.downloads.ui",
        // Our own app (extra safety)
        "com.nzsk.pureskip"
    )

    // Known sensitive activity keywords
    private val sensitiveActivityKeywords = listOf(
        "login", "signin", "signup", "register", "password", "payment",
        "checkout", "purchase", "billing", "pay", "verify", "verification",
        "captcha", "install", "uninstall", "permission", "confirm",
        "security", "authenticate", "otp", "sms"
    )

    // Password-related view IDs
    private val passwordViewIds = listOf(
        "password", "passwd", "pwd", "pin", "passcode",
        "验证码", "密码", "支付", "确认"
    )

    private val sensitiveContentKeywords = listOf(
        "支付", "付款", "购买", "订单", "验证码", "授权", "安装", "卸载", "登录",
        "payment", "purchase", "checkout", "billing", "otp", "captcha", "permission",
        "install", "uninstall", "password", "verify"
    )

    /**
     * Checks if the current scene is sensitive and should not be acted upon.
     */
    fun isSensitiveScene(rootNode: AccessibilityNodeInfo?, packageName: String): Boolean {
        if (rootNode == null) {
            Log.d("SensitiveScene", "Root node is null for $packageName")
            return true
        }

        // Check if it's a known sensitive package
        if (sensitivePackages.contains(packageName)) {
            Log.d("SensitiveScene", "Sensitive package detected: $packageName")
            return true
        }

        // Broad launcher detection (covers most Android launchers)
        if (packageName.contains("launcher", ignoreCase = true) ||
            packageName.contains("home", ignoreCase = true)) {
            Log.d("SensitiveScene", "Launcher/home detected: $packageName")
            return true
        }

        // Check for password fields in the view hierarchy
        if (containsPasswordField(rootNode)) {
            Log.d("SensitiveScene", "Password field detected in $packageName")
            return true
        }

        if (containsFocusedEditableField(rootNode)) {
            Log.d("SensitiveScene", "Focused text input detected in $packageName")
            return true
        }

        if (containsSensitiveContent(rootNode)) {
            Log.d("SensitiveScene", "Sensitive content detected in $packageName")
            return true
        }

        return false
    }

    /**
     * Recursively checks if any node contains a password input field.
     */
    private fun containsPasswordField(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > MAX_DEPTH) return false

        // Check if this is a password field
        if (node.isPassword) return true

        // Check view ID for password-related keywords
        val viewId = node.viewIdResourceName ?: ""
        if (passwordViewIds.any { viewId.contains(it, ignoreCase = true) }) return true

        // Check class name for password input
        val className = node.className?.toString() ?: ""
        if (className.contains("EditText", ignoreCase = true)) {
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            if (passwordViewIds.any { text.contains(it, ignoreCase = true) ||
                        contentDesc.contains(it, ignoreCase = true) }) {
                return true
            }
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsPasswordField(child, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    private fun containsFocusedEditableField(
        node: AccessibilityNodeInfo,
        depth: Int = 0
    ): Boolean {
        if (depth > MAX_DEPTH) return false
        val isEditable = node.isEditable ||
            node.className?.toString().orEmpty().contains("EditText", ignoreCase = true)
        if (isEditable && node.isFocused) return true

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            if (containsFocusedEditableField(child, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    private fun containsSensitiveContent(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > MAX_DEPTH) return false
        val values = listOf(
            node.viewIdResourceName.orEmpty(),
            node.text?.toString().orEmpty(),
            node.contentDescription?.toString().orEmpty()
        )
        if (values.any { value ->
                sensitiveContentKeywords.any { keyword -> value.contains(keyword, ignoreCase = true) }
            }) {
            return true
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            if (containsSensitiveContent(child, depth + 1)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    companion object {
        private const val MAX_DEPTH = 10
    }
}
