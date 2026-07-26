package com.nzsk.pureskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Point
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.engine.ActionExecutor
import com.nzsk.pureskip.engine.WindowFingerprint
import com.nzsk.pureskip.settings.LearnedPopupRule
import java.util.UUID

class TeachingOverlayController(
    private val service: AccessibilityService,
    private val actionExecutor: ActionExecutor
) {

    private data class TeachingDraft(
        val packageName: String,
        val normalizedX: Float,
        val normalizedY: Float,
        val orientation: Int,
        val activityName: String,
        val windowTitle: String,
        val snapshot: WindowFingerprint.Snapshot
    )

    private val settings = PureSkipApplication.getInstance().settingsManager
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var currentTeachingPackage: String? = null
    private var manualButtonRuleId: String? = null

    fun isTeaching(): Boolean = currentTeachingPackage != null

    fun showTeaching(
        packageName: String,
        activityName: String,
        windowTitle: String,
        snapshot: WindowFingerprint.Snapshot
    ) {
        if (overlayView != null || currentTeachingPackage == packageName) return
        currentTeachingPackage = packageName

        val root = FrameLayout(service).apply {
            setBackgroundColor(Color.argb(35, 0, 0, 0))
            clipChildren = false
            clipToPadding = false
        }
        var selectedScreenPoint: FloatPoint? = null
        val crosshair = TextView(service).apply {
            text = "⊕"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(Color.RED)
            background = roundedBackground(Color.argb(210, 255, 255, 255), 24f)
        }
        root.addView(crosshair, FrameLayout.LayoutParams(dp(88), dp(88), Gravity.CENTER))
        installDrag(crosshair, root) { selectedScreenPoint = it }

        val panel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundedBackground(Color.argb(240, 30, 30, 30), 18f)
        }
        panel.addView(TextView(service).apply {
            text = "手动教学：拖动红色准星到弹窗关闭按钮，然后点“测试一次”。"
            setTextColor(Color.WHITE)
            textSize = 15f
        })
        val actions = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        var panelAtTop = false
        actions.addView(actionButton("换边") {
            panelAtTop = !panelAtTop
            val layoutParams = panel.layoutParams as? FrameLayout.LayoutParams
                ?: return@actionButton
            layoutParams.gravity = if (panelAtTop) Gravity.TOP else Gravity.BOTTOM
            panel.layoutParams = layoutParams
        })
        actions.addView(actionButton("取消") {
            settings.setPendingTeachingPackage(null)
            removeOverlay()
        })
        actions.addView(actionButton("测试一次") {
            val screenSize = realScreenSize()
            val overlayLocation = IntArray(2).also(root::getLocationOnScreen)
            val point = selectedScreenPoint ?: TeachingDragGeometry.toScreenPoint(
                localCenterX = crosshair.x + crosshair.width / 2f,
                localCenterY = crosshair.y + crosshair.height / 2f,
                overlayScreenX = overlayLocation[0],
                overlayScreenY = overlayLocation[1],
                screenWidth = screenSize.x,
                screenHeight = screenSize.y
            )
            val x = point.x
            val y = point.y
            val draft = TeachingDraft(
                packageName = packageName,
                normalizedX = (x / screenSize.x.coerceAtLeast(1)).coerceIn(0f, 1f),
                normalizedY = (y / screenSize.y.coerceAtLeast(1)).coerceIn(0f, 1f),
                orientation = service.resources.configuration.orientation,
                activityName = activityName,
                windowTitle = windowTitle,
                snapshot = snapshot
            )
            removeOverlay(clearTeaching = false)
            handler.postDelayed({
                actionExecutor.executeAt(x, y) { result ->
                    if (result.success) {
                        handler.postDelayed({ showSaveConfirmation(draft) }, 500L)
                    } else {
                        Toast.makeText(service, "测试点击失败，请重新教学", Toast.LENGTH_LONG).show()
                        currentTeachingPackage = null
                    }
                }
            }, 150L)
        })
        panel.addView(actions)
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply { setMargins(dp(12), dp(24), dp(12), dp(24)) }
        )
        crosshair.bringToFront()

        addOverlay(root, matchScreen = true)
    }

    fun showManualCloseButton(
        rule: LearnedPopupRule,
        onConfirm: (LearnedPopupRule) -> Unit
    ) {
        if (overlayView != null || isTeaching() || manualButtonRuleId == rule.ruleId) return
        manualButtonRuleId = rule.ruleId
        val row = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(8), dp(6))
            background = roundedBackground(Color.argb(238, 25, 90, 125), 28f)
        }
        row.addView(actionButton("关闭此弹窗") {
            removeOverlay()
            onConfirm(rule)
        })
        row.addView(actionButton("×") { removeOverlay() })
        addOverlay(row, matchScreen = false)
        handler.postDelayed({
            if (manualButtonRuleId == rule.ruleId) removeOverlay()
        }, MANUAL_BUTTON_TIMEOUT_MS)
    }

    private fun showSaveConfirmation(draft: TeachingDraft) {
        val panel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundedBackground(Color.argb(245, 30, 30, 30), 18f)
        }
        panel.addView(TextView(service).apply {
            text = "刚才的测试是否成功关闭了弹窗？"
            setTextColor(Color.WHITE)
            textSize = 16f
        })
        val actions = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        actions.addView(actionButton("未关闭，不保存") {
            settings.setPendingTeachingPackage(null)
            removeOverlay()
        })
        actions.addView(actionButton("已关闭，保存") {
            val rule = LearnedPopupRule(
                ruleId = "learned-${UUID.randomUUID()}",
                packageName = draft.packageName,
                normalizedX = draft.normalizedX,
                normalizedY = draft.normalizedY,
                orientation = draft.orientation,
                activityName = draft.activityName,
                windowTitle = draft.windowTitle,
                hierarchyFingerprint = draft.snapshot.value,
                autoEligible = draft.snapshot.isStableTrigger && draft.activityName.isNotBlank()
            )
            settings.saveLearnedRule(rule)
            settings.setEnhancementMasterEnabled(true)
            settings.setAppEnhancedEnabled(draft.packageName, true)
            settings.setPendingTeachingPackage(null)
            removeOverlay()
            val message = if (rule.autoEligible) {
                "学习规则已保存，可自动识别"
            } else {
                "规则已保存；无稳定特征时将显示确认按钮"
            }
            Toast.makeText(service, message, Toast.LENGTH_LONG).show()
        })
        panel.addView(actions)
        addOverlay(panel, matchScreen = false)
    }

    private fun installDrag(
        target: View,
        parent: View,
        onScreenPointChanged: (FloatPoint) -> Unit
    ) {
        var grabOffsetX = 0f
        var grabOffsetY = 0f
        var screenWidth = 1
        var screenHeight = 1
        var parentScreenX = 0
        var parentScreenY = 0
        var parentWidth = 1
        var parentHeight = 1

        val frameCoalescer = DragFrameCoalescer(
            scheduleFrame = target::postOnAnimation,
            render = { centerScreenX, centerScreenY ->
                val position = TeachingDragGeometry.clampTopLeftForCenter(
                    desiredCenterX = centerScreenX - parentScreenX,
                    desiredCenterY = centerScreenY - parentScreenY,
                    viewWidth = target.width,
                    viewHeight = target.height,
                    parentWidth = parentWidth,
                    parentHeight = parentHeight
                )
                // Translation avoids layout passes; coalescing guarantees at most one visual
                // update per display frame even when the device emits many MOVE events.
                target.translationX = position.x - target.left
                target.translationY = position.y - target.top
                onScreenPointChanged(FloatPoint(centerScreenX, centerScreenY))
            }
        )

        fun submitPointer(rawX: Float, rawY: Float, flush: Boolean = false) {
            val centerScreenX = (rawX - grabOffsetX).coerceIn(0f, screenWidth.toFloat())
            val centerScreenY = (rawY - grabOffsetY).coerceIn(0f, screenHeight.toFloat())
            frameCoalescer.submit(centerScreenX, centerScreenY)
            if (flush) frameCoalescer.flush()
        }

        target.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val screenSize = realScreenSize()
                    screenWidth = screenSize.x.coerceAtLeast(1)
                    screenHeight = screenSize.y.coerceAtLeast(1)
                    val parentLocation = IntArray(2).also(parent::getLocationOnScreen)
                    parentScreenX = parentLocation[0]
                    parentScreenY = parentLocation[1]
                    parentWidth = parent.width.coerceAtLeast(1)
                    parentHeight = parent.height.coerceAtLeast(1)

                    val centerScreenX = parentScreenX + view.x + view.width / 2f
                    val centerScreenY = parentScreenY + view.y + view.height / 2f
                    grabOffsetX = event.rawX - centerScreenX
                    grabOffsetY = event.rawY - centerScreenY
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        parent.requestUnbufferedDispatch(event)
                    }
                    frameCoalescer.submit(centerScreenX, centerScreenY)
                    frameCoalescer.flush()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    submitPointer(event.rawX, event.rawY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    submitPointer(event.rawX, event.rawY, flush = true)
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    frameCoalescer.cancel()
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    true
                }
                else -> true
            }
        }
    }

    private fun addOverlay(view: View, matchScreen: Boolean) {
        removeOverlay(clearTeaching = false)
        val params = WindowManager.LayoutParams(
            if (matchScreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            if (matchScreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (matchScreen) Gravity.TOP or Gravity.START else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            if (!matchScreen) y = dp(80)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        runCatching {
            windowManager.addView(view, params)
            overlayView = view
        }.onFailure {
            Toast.makeText(service, "无法显示教学悬浮层", Toast.LENGTH_LONG).show()
            currentTeachingPackage = null
        }
    }

    fun removeOverlay(clearTeaching: Boolean = true) {
        overlayView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        overlayView = null
        manualButtonRuleId = null
        if (clearTeaching) currentTeachingPackage = null
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
    }

    private fun actionButton(label: String, action: () -> Unit): Button {
        return Button(service).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun roundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun dp(value: Int): Int {
        return (value * service.resources.displayMetrics.density).toInt()
    }

    @Suppress("DEPRECATION")
    private fun realScreenSize(): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.let { Point(it.width(), it.height()) }
        } else {
            Point().also(windowManager.defaultDisplay::getRealSize)
        }
    }

    companion object {
        private const val MANUAL_BUTTON_TIMEOUT_MS = 10_000L
    }
}
