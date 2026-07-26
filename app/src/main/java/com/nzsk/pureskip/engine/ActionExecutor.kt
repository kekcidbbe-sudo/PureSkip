package com.nzsk.pureskip.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.nzsk.pureskip.rules.ActionType

class ActionExecutor(private val service: AccessibilityService) {

    data class ActionResult(
        val success: Boolean,
        val actionType: ActionType,
        val reason: String
    )

    fun execute(
        node: AccessibilityNodeInfo,
        actionType: ActionType,
        callback: (ActionResult) -> Unit
    ) {
        try {
            when (actionType) {
                ActionType.CLICK -> performClick(node, callback)
                ActionType.LONG_CLICK -> {
                    val success = node.isLongClickable &&
                        node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                    callback(ActionResult(success, actionType, if (success) "节点长按完成" else "节点长按失败"))
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Action execution failed", error)
            callback(ActionResult(false, actionType, "执行异常: ${error.message}"))
        }
    }

    fun executeAt(x: Float, y: Float, callback: (ActionResult) -> Unit) {
        dispatchTap(x, y, callback)
    }

    private fun performClick(
        node: AccessibilityNodeInfo,
        callback: (ActionResult) -> Unit
    ) {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            callback(ActionResult(true, ActionType.CLICK, "节点点击完成"))
            return
        }
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        dispatchTap(bounds.exactCenterX(), bounds.exactCenterY(), callback)
    }

    private fun dispatchTap(
        x: Float,
        y: Float,
        callback: (ActionResult) -> Unit
    ) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        val accepted = service.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback(ActionResult(true, ActionType.CLICK, "手势点击完成"))
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback(ActionResult(false, ActionType.CLICK, "手势点击被取消"))
                }
            },
            null
        )
        if (!accepted) callback(ActionResult(false, ActionType.CLICK, "系统拒绝手势"))
    }

    companion object {
        private const val TAG = "ActionExecutor"
    }
}
