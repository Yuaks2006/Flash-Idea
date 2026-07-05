package com.flashidea.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.flashidea.app.MainActivity

class FlashGestureService : AccessibilityService() {

    private var api33Controller: Api33ThreeFingerController? = null
    private var lastLaunchElapsedRealtime = 0L

    override fun onServiceConnected() {
        logRomInfo()
        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                AccessibilityServiceInfo.FLAG_REQUEST_MULTI_FINGER_GESTURES
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            api33Controller?.close()
            api33Controller = Api33ThreeFingerController(this, ::launchCapture)
            Log.i(TAG, "API33 ThreeFingerController 已启用（放宽阈值 + 全状态委托兜底）")
        } else {
            Log.w(TAG, "API < 33，仅走 deprecated onGesture(Int) 路径，三指手势可能被系统优先消费")
        }
    }

    override fun onGesture(gestureEvent: AccessibilityGestureEvent): Boolean {
        return handleGesture(gestureEvent.gestureId) || super.onGesture(gestureEvent)
    }

    @Suppress("DEPRECATION")
    override fun onGesture(gestureId: Int): Boolean {
        return handleGesture(gestureId) || super.onGesture(gestureId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            api33Controller?.close()
        }
        api33Controller = null
        super.onDestroy()
    }

    private fun handleGesture(gestureId: Int): Boolean {
        if (gestureId != AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP) {
            return false
        }
        Log.d(TAG, "收到 GESTURE_3_FINGER_DOUBLE_TAP（API<33 路径）")
        launchCapture()
        return true
    }

    private fun launchCapture() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunchElapsedRealtime < LAUNCH_COOLDOWN_MILLIS) {
            Log.d(TAG, "launchCapture 冷却中，忽略重复触发")
            return
        }
        lastLaunchElapsedRealtime = now
        Log.i(TAG, "launchCapture 触发快速记录入口")
        getSystemService(VibratorManager::class.java)
            ?.defaultVibrator
            ?.vibrate(
                VibrationEffect.createOneShot(
                    CONFIRMATION_VIBRATION_MILLIS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        sendNewNoteIntent(this)
    }

    /** 输出 ROM 信息，便于真机排查三指手势兼容性。 */
    private fun logRomInfo() {
        Log.i(
            TAG,
            "ROM 信息: manufacturer=${Build.MANUFACTURER}, " +
                "brand=${Build.BRAND}, " +
                "model=${Build.MODEL}, " +
                "sdk=${Build.VERSION.SDK_INT}, " +
                "release=${Build.VERSION.RELEASE}"
        )
    }

    companion object {
        private const val TAG = "FlashGesture"
        private const val LAUNCH_COOLDOWN_MILLIS = 800L
        private const val CONFIRMATION_VIBRATION_MILLIS = 35L

        fun sendNewNoteIntent(context: Context) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_NEW_NOTE
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
            )
        }

        fun openSettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val componentName = context.packageName + "/" + FlashGestureService::class.java.name
            return enabledServices.contains(componentName)
        }
    }
}
