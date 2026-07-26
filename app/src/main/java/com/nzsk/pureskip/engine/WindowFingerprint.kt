package com.nzsk.pureskip.engine

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

object WindowFingerprint {

    data class Snapshot(
        val value: String,
        val nodeCount: Int,
        val viewIdCount: Int,
        val distinctClassCount: Int
    ) {
        val isStableTrigger: Boolean
            get() = nodeCount >= 3 && (viewIdCount > 0 || distinctClassCount >= 3)
    }

    fun capture(root: AccessibilityNodeInfo): Snapshot {
        val parts = ArrayList<String>()
        val classes = linkedSetOf<String>()
        var viewIdCount = 0
        var nodeCount = 0

        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_DEPTH || nodeCount >= MAX_NODES) return
            nodeCount++
            val className = node.className?.toString().orEmpty()
            val viewId = node.viewIdResourceName.orEmpty()
            val bounds = Rect().also { node.getBoundsInScreen(it) }
            if (className.isNotBlank()) classes += className
            if (viewId.isNotBlank()) viewIdCount++
            parts += "$depth|$className|$viewId|${node.childCount}|${bounds.width()}x${bounds.height()}"
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                walk(child, depth + 1)
            }
        }

        walk(root, 0)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("\n").toByteArray())
            .joinToString("") { "%02x".format(it) }
        return Snapshot(digest, nodeCount, viewIdCount, classes.size)
    }

    private const val MAX_DEPTH = 35
    private const val MAX_NODES = 400
}
