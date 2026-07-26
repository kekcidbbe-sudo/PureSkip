package com.nzsk.pureskip.engine

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.accessibility.TeachingOverlayController
import com.nzsk.pureskip.safety.AppSafetyPolicy
import com.nzsk.pureskip.safety.SafetyCheckResult
import com.nzsk.pureskip.safety.SafetyGuard
import com.nzsk.pureskip.settings.DiagnosticEntry
import com.nzsk.pureskip.settings.EnhancementScope
import com.nzsk.pureskip.settings.RecognitionOutcomeCode
import com.nzsk.pureskip.settings.LearnedPopupRule
import com.nzsk.pureskip.settings.LearnedRuleMatcher

class EngineOrchestrator(private val service: AccessibilityService) {

    private val ruleMatcher = RuleMatcher()
    private val actionExecutor = ActionExecutor(service)
    private val teachingOverlay = TeachingOverlayController(service, actionExecutor)
    private val safetyGuard = SafetyGuard.getInstance()
    private val settings = PureSkipApplication.getInstance().settingsManager
    private val handler = Handler(Looper.getMainLooper())
    private val pendingRunnables = mutableListOf<Runnable>()
    private val scanScheduleLimiter = ScanScheduleLimiter()
    private val scanExecutionLimiter = ScanExecutionLimiter()

    private var currentPackage = ""
    private var currentActivity = ""
    private var appSwitchTimestamp = 0L
    private var lastContentChangedTime = 0L
    private var lastInteractionTime = 0L
    private var latestEventType = 0
    private var actionInFlight = false
    private val blockedLearnedSignatures = mutableMapOf<String, String>()

    fun processEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        latestEventType = event.eventType

        if (packageName == service.packageName) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                cancelPending()
                currentPackage = ""
                actionInFlight = false
            }
            return
        }

        if (!shouldProcess(packageName)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                cancelPending()
                actionInFlight = false
                if (!AppSafetyPolicy.preservesForegroundSession(packageName)) {
                    currentPackage = ""
                }
            }
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.className?.toString()?.takeIf { it.isNotBlank() }?.let { currentActivity = it }
            onAppSwitched(packageName)
        } else if (packageName != currentPackage && shouldProcess(packageName)) {
            // A service reconnect or an SDK-hosted window can start with only content/window
            // events. Adopt the enabled foreground package instead of discarding every scan.
            onAppSwitched(packageName)
        }
        if (teachingOverlay.isTeaching()) return

        val policy = settings.getAppPolicy(packageName)
        val now = System.currentTimeMillis()
        val withinStartup = now - appSwitchTimestamp <= STARTUP_WINDOW_MS
        val enhanced = settings.isEnhancementMasterEnabled() &&
            policy.shouldUseEnhancedRules() &&
            (policy.scope == EnhancementScope.FULL_TIME || withinStartup)
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                applyRetryPlan(packageName, EventRetryPolicy.forWindowState(enhanced))
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                applyRetryPlan(packageName, EventRetryPolicy.forWindowsChanged(enhanced))
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (RecognitionActivityPolicy.shouldHandleContentChange(withinStartup, enhanced) &&
                    now - lastContentChangedTime >= CONTENT_CHANGE_THROTTLE_MS
                ) {
                    lastContentChangedTime = now
                    applyRetryPlan(packageName, EventRetryPolicy.forContentChanged(enhanced))
                }
            }
            else -> {
                if (AccessibilityEventPolicy.shouldScheduleInteraction(event.eventType)) {
                    if (now - lastInteractionTime >= INTERACTION_THROTTLE_MS) {
                        lastInteractionTime = now
                        applyRetryPlan(packageName, EventRetryPolicy.forInteraction(enhanced))
                    }
                }
            }
        }
    }

    private fun applyRetryPlan(packageName: String, plan: RetryPlan) {
        if (plan.cancelPending) cancelPending()
        plan.delaysMs.forEach { scheduleProcess(packageName, it) }
    }

    private fun scheduleProcess(packageName: String, delayMs: Long) {
        val slot = scanScheduleLimiter.reserve(SystemClock.uptimeMillis(), delayMs) ?: return
        lateinit var task: Runnable
        task = Runnable {
            pendingRunnables.remove(task)
            scanScheduleLimiter.release(slot)
            if (scanExecutionLimiter.tryAcquire(SystemClock.uptimeMillis())) {
                processCurrentWindow(packageName)
            }
        }
        pendingRunnables += task
        if (!handler.postDelayed(task, delayMs)) {
            pendingRunnables.remove(task)
            scanScheduleLimiter.release(slot)
        }
    }

    private fun processCurrentWindow(packageName: String) {
        if (packageName != currentPackage || actionInFlight || teachingOverlay.isTeaching()) return
        val root = runCatching { service.rootInActiveWindow }.getOrNull()
        if (root == null) {
            recordDiagnostic(packageName, 0, "", 0.0, RecognitionOutcomeCode.ROOT_UNAVAILABLE)
            return
        }
        try {
            if (root.packageName?.toString() != packageName) {
                recordDiagnostic(
                    packageName,
                    0,
                    "",
                    0.0,
                    RecognitionOutcomeCode.ROOT_UNAVAILABLE
                )
                return
            }
            val pendingTeaching = settings.getPendingTeachingPackage()
            if (pendingTeaching == packageName && !teachingOverlay.isTeaching()) {
                val snapshot = WindowFingerprint.capture(root)
                val windowTitle = runCatching {
                    root.window?.title?.toString().orEmpty()
                }.getOrDefault("")
                teachingOverlay.showTeaching(
                    packageName = packageName,
                    activityName = currentActivity,
                    windowTitle = windowTitle,
                    snapshot = snapshot
                )
                return
            }
            processWindow(root, packageName)
        } finally {
            root.recycle()
        }
    }

    private fun processWindow(root: AccessibilityNodeInfo, packageName: String) {
        val policy = settings.getAppPolicy(packageName)
        val withinStartup = System.currentTimeMillis() - appSwitchTimestamp <= STARTUP_WINDOW_MS
        val enhanced = settings.isEnhancementMasterEnabled() &&
            policy.shouldUseEnhancedRules() &&
            (policy.scope == EnhancementScope.FULL_TIME || withinStartup)
        val metrics = service.resources.displayMetrics
        val windowTitle = runCatching { root.window?.title?.toString().orEmpty() }.getOrDefault("")
        val match = ruleMatcher.match(
            packageName = packageName,
            rootNode = root,
            enhancedEnabled = enhanced,
            activityName = currentActivity,
            windowTitle = windowTitle,
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels
        )
        val node = match.targetNode
        val rule = match.rule
        if (!match.matched || node == null || rule == null) {
            val hasLearnedRules = enhanced && settings.getLearnedRules(packageName)
                .any { it.enabled && it.hasCompatibleCoordinates() }
            val snapshot = captureSnapshotIfNeeded(
                root = root,
                actionNeedsVerification = false,
                hasLearnedRules = hasLearnedRules
            )
            if (enhanced && hasLearnedRules && snapshot != null && handleLearnedRules(
                    root = root,
                    packageName = packageName,
                    snapshot = snapshot,
                    windowTitle = windowTitle,
                    screenWidth = metrics.widthPixels,
                    screenHeight = metrics.heightPixels
                )
            ) {
                return
            }
            recordDiagnostic(
                packageName,
                snapshot?.nodeCount ?: 0,
                "",
                match.confidence,
                RecognitionOutcomeCode.NO_CANDIDATE
            )
            return
        }

        val safety = safetyGuard.check(packageName, root, rule)
        if (safety is SafetyCheckResult.Blocked) {
            val snapshot = captureSnapshotIfNeeded(
                root = root,
                actionNeedsVerification = false,
                hasLearnedRules = false
            )
            recordDiagnostic(
                packageName,
                snapshot?.nodeCount ?: 0,
                rule.ruleId,
                match.confidence,
                RecognitionOutcomeCode.SAFETY_BLOCKED
            )
            return
        }

        val snapshot = checkNotNull(
            captureSnapshotIfNeeded(
                root = root,
                actionNeedsVerification = true,
                hasLearnedRules = false
            )
        )
        @Suppress("DEPRECATION")
        val nodeCopy = AccessibilityNodeInfo.obtain(node)
        cancelPending()
        actionInFlight = true
        handler.postDelayed({
            val activeRoot = runCatching { service.rootInActiveWindow }.getOrNull()
            val activeRootPackage = try {
                activeRoot?.packageName?.toString()
            } finally {
                activeRoot?.recycle()
            }
            @Suppress("DEPRECATION")
            val refreshed = runCatching { nodeCopy.refresh() }.getOrDefault(false)
            val canExecute = shouldProcess(packageName) &&
                PendingActionPolicy.canExecute(
                    expectedPackage = packageName,
                    currentPackage = currentPackage,
                    activeRootPackage = activeRootPackage,
                    targetPackage = nodeCopy.packageName?.toString(),
                    targetRefreshed = refreshed,
                    targetVisible = nodeCopy.isVisibleToUser
                )
            if (!canExecute) {
                nodeCopy.recycle()
                actionInFlight = false
                recordDiagnostic(
                    packageName,
                    snapshot.nodeCount,
                    rule.ruleId,
                    match.confidence,
                    RecognitionOutcomeCode.ACTION_FAILED
                )
                return@postDelayed
            }

            actionExecutor.execute(nodeCopy, rule.action.type) { result ->
                nodeCopy.recycle()
                if (!result.success) {
                    actionInFlight = false
                    recordDiagnostic(
                        packageName,
                        snapshot.nodeCount,
                        rule.ruleId,
                        match.confidence,
                        RecognitionOutcomeCode.ACTION_FAILED
                    )
                    return@execute
                }

                safetyGuard.recordAction(packageName, rule.ruleId)
                handler.postDelayed({
                    verifyWindowChanged(
                        packageName = packageName,
                        before = snapshot,
                        ruleId = rule.ruleId,
                        confidence = match.confidence
                    )
                }, VERIFY_DELAY_MS)
            }
        }, rule.action.delayMs.coerceAtLeast(0L))
    }

    private fun captureSnapshotIfNeeded(
        root: AccessibilityNodeInfo,
        actionNeedsVerification: Boolean,
        hasLearnedRules: Boolean
    ): WindowFingerprint.Snapshot? {
        return if (WindowSnapshotPolicy.requiresSnapshot(
                actionNeedsVerification = actionNeedsVerification,
                hasLearnedRules = hasLearnedRules,
                diagnosticsEnabled = settings.isDiagnosticsEnabled()
            )
        ) {
            WindowFingerprint.capture(root)
        } else {
            null
        }
    }

    private fun handleLearnedRules(
        root: AccessibilityNodeInfo,
        packageName: String,
        snapshot: WindowFingerprint.Snapshot,
        windowTitle: String,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        val orientation = service.resources.configuration.orientation
        val rules = settings.getLearnedRules(packageName)
            .filter {
                it.enabled && it.hasCompatibleCoordinates() && it.orientation == orientation
            }
        if (rules.isEmpty()) return false

        blockedLearnedSignatures.entries.removeAll { (ruleId, signature) ->
            rules.any { it.ruleId == ruleId } && signature != snapshot.value
        }

        val automatic = rules.firstOrNull { rule ->
            LearnedRuleMatcher.matchesAutomatic(
                rule = rule,
                orientation = orientation,
                activityName = currentActivity,
                windowTitle = windowTitle,
                hierarchyFingerprint = snapshot.value
            ) && blockedLearnedSignatures[rule.ruleId] != snapshot.value
        }
        if (automatic != null) {
            val safety = safetyGuard.checkLearnedAction(packageName, root, automatic.ruleId)
            if (safety is SafetyCheckResult.Blocked) {
                recordDiagnostic(
                    packageName,
                    snapshot.nodeCount,
                    automatic.ruleId,
                    100.0,
                    RecognitionOutcomeCode.SAFETY_BLOCKED
                )
                return true
            }
            blockedLearnedSignatures[automatic.ruleId] = snapshot.value
            executeLearnedRule(automatic, snapshot, screenWidth, screenHeight)
            return true
        }

        val manual = rules.firstOrNull { !it.autoEligible } ?: return false
        val safety = safetyGuard.checkLearnedAction(packageName, root, manual.ruleId)
        if (safety is SafetyCheckResult.Allowed) {
            teachingOverlay.showManualCloseButton(manual) { confirmedRule ->
                val metrics = service.resources.displayMetrics
                val currentRoot = runCatching { service.rootInActiveWindow }.getOrNull()
                if (currentRoot == null) return@showManualCloseButton
                try {
                    val currentSafety = safetyGuard.checkLearnedAction(
                        packageName,
                        currentRoot,
                        confirmedRule.ruleId
                    )
                    if (currentSafety is SafetyCheckResult.Allowed) {
                        executeLearnedRule(
                            confirmedRule,
                            WindowFingerprint.capture(currentRoot),
                            metrics.widthPixels,
                            metrics.heightPixels
                        )
                    }
                } finally {
                    currentRoot.recycle()
                }
            }
            recordDiagnostic(
                packageName,
                snapshot.nodeCount,
                manual.ruleId,
                0.0,
                RecognitionOutcomeCode.USER_CONFIRMATION_REQUIRED
            )
            return true
        }
        return false
    }

    private fun executeLearnedRule(
        rule: LearnedPopupRule,
        before: WindowFingerprint.Snapshot,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (actionInFlight) return
        actionInFlight = true
        actionExecutor.executeAt(rule.screenX(screenWidth), rule.screenY(screenHeight)) { result ->
            if (!result.success) {
                actionInFlight = false
                recordDiagnostic(
                    rule.packageName,
                    before.nodeCount,
                    rule.ruleId,
                    100.0,
                    RecognitionOutcomeCode.ACTION_FAILED
                )
                return@executeAt
            }
            safetyGuard.recordAction(rule.packageName, rule.ruleId)
            handler.postDelayed({
                verifyWindowChanged(
                    packageName = rule.packageName,
                    before = before,
                    ruleId = rule.ruleId,
                    confidence = 100.0
                )
            }, VERIFY_DELAY_MS)
        }
    }

    private fun verifyWindowChanged(
        packageName: String,
        before: WindowFingerprint.Snapshot,
        ruleId: String,
        confidence: Double
    ) {
        val root = runCatching { service.rootInActiveWindow }.getOrNull()
        val changed = if (root != null) {
            try {
                WindowFingerprint.capture(root).value != before.value
            } finally {
                root.recycle()
            }
        } else {
            false
        }
        actionInFlight = false
        if (changed && packageName == currentPackage) {
            settings.incrementSkipCount()
            recordDiagnostic(
                packageName,
                before.nodeCount,
                ruleId,
                confidence,
                RecognitionOutcomeCode.SUCCESS
            )
        } else {
            recordDiagnostic(
                packageName,
                before.nodeCount,
                ruleId,
                confidence,
                RecognitionOutcomeCode.ACTION_FAILED
            )
        }
    }

    private fun onAppSwitched(packageName: String) {
        if (packageName == currentPackage) return
        cancelPending()
        currentPackage = packageName
        scanExecutionLimiter.reset()
        appSwitchTimestamp = System.currentTimeMillis()
        settings.recordObservedApp(packageName, appSwitchTimestamp)
        safetyGuard.onNewSession(packageName)
        actionInFlight = false
        blockedLearnedSignatures.clear()
        teachingOverlay.removeOverlay()
        Log.d(TAG, "App switched to: $packageName")
    }

    private fun shouldProcess(packageName: String): Boolean {
        return ProcessingEligibilityPolicy.shouldProcess(
            masterEnabled = settings.isMasterEnabled(),
            paused = settings.isPaused(),
            appEnabled = settings.isAppEnabled(packageName),
            appBlocked = settings.isAppBlocked(packageName),
            systemRestricted = AppSafetyPolicy.isRestricted(packageName)
        )
    }

    private fun recordDiagnostic(
        packageName: String,
        nodeCount: Int,
        ruleId: String,
        confidence: Double,
        outcome: RecognitionOutcomeCode
    ) {
        settings.addDiagnostic(
            DiagnosticEntry(
                timestamp = System.currentTimeMillis(),
                packageName = packageName,
                eventType = latestEventType,
                nodeCount = nodeCount,
                ruleId = ruleId,
                confidence = confidence,
                outcome = outcome
            )
        )
    }

    private fun cancelPending() {
        pendingRunnables.forEach(handler::removeCallbacks)
        pendingRunnables.clear()
        scanScheduleLimiter.clear()
    }

    fun destroy() {
        cancelPending()
        handler.removeCallbacksAndMessages(null)
        safetyGuard.clearAll()
        teachingOverlay.destroy()
    }

    companion object {
        private const val TAG = "EngineOrchestrator"
        private const val CONTENT_CHANGE_THROTTLE_MS = 500L
        private const val INTERACTION_THROTTLE_MS = 250L
        private const val STARTUP_WINDOW_MS = 10_000L
        private const val VERIFY_DELAY_MS = 700L
    }
}
