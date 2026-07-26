package com.nzsk.pureskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.nzsk.pureskip.PureSkipApplication
import com.nzsk.pureskip.engine.EngineOrchestrator
import com.nzsk.pureskip.rules.RuleProvider

/**
 * Core accessibility service that receives system events and delegates to the engine.
 * This is the entry point for all ad-skip operations.
 *
 * Modified to run as a foreground service to prevent being killed by the system.
 */
class SkipAccessibilityService : AccessibilityService() {

    private lateinit var engine: EngineOrchestrator
    private var isInitialized = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")

        try {
            engine = EngineOrchestrator(this)
            RuleProvider.initialize()
            isInitialized = true

            // Start as foreground service to prevent being killed
            startForegroundCompat()

            // Notify settings about service state
            PureSkipApplication.getInstance().settingsManager.setServiceRunning(true)

            Log.i(TAG, "Service initialized successfully with ${RuleProvider.getRuleCount()} rules")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize service", e)
            isInitialized = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isInitialized || event == null) return

        try {
            engine.processEvent(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        Log.i(TAG, "Accessibility service destroying")
        if (isInitialized) {
            engine.destroy()
        }
        PureSkipApplication.getInstance().settingsManager.setServiceRunning(false)
        super.onDestroy()

        // Attempt to restart service if killed
        scheduleServiceRestart()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "Accessibility service unbound")
        PureSkipApplication.getInstance().settingsManager.setServiceRunning(false)
        return super.onUnbind(intent)
    }

    /**
     * Start as foreground service with a low-priority notification.
     * This tells the OS that the service is doing important work and should not be killed.
     */
    private fun startForegroundCompat() {
        try {
            val channelId = "pureskip_service_channel"
            val channelName = "纯净跳过服务"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "用于持续监控并自动跳过应用启动广告和弹窗"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("纯净跳过运行中")
                .setContentText("正在监控并自动跳过广告")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Started as foreground service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    /**
     * Schedule a service restart attempt after being killed.
     */
    private fun scheduleServiceRestart() {
        handler.postDelayed({
            try {
                val intent = Intent(this, SkipAccessibilityService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Log.d(TAG, "Service restart attempted")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart service", e)
            }
        }, RESTART_DELAY_MS)
    }

    companion object {
        private const val TAG = "SkipA11yService"
        private const val NOTIFICATION_ID = 1001
        private const val RESTART_DELAY_MS = 30_000L // 30 seconds
    }
}
