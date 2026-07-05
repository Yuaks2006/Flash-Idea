package com.flashidea.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.TouchInteractionController
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi

/**
 * API 33+ 三指双击控制器。
 *
 * 相对原始实现的关键调整（批次4 兼容性优化）：
 * 1. **阈值放宽**：chordTimeout 120→250ms（人手三指自然落下 150-250ms）；
 *    tapTimeout 用 getLongPressTimeout()（约 400ms）替代 getTapTimeout()（约 100ms）；
 *    doubleTapTimeout 300→450ms；touchSlop × 1.5 容差。让用户更容易触发。
 * 2. **delegateCurrentTouch 全状态兜底**：原实现仅在 STATE_TOUCH_INTERACTING 才 requestDelegating，
 *    其他状态静默丢弃事件导致触摸无响应。现扩展到所有状态尝试委托，并在不可委托时
 *    用 dispatchGesture 模拟一次"轻点"以唤醒系统继续分发。
 * 3. **状态机日志**：onStateChanged 输出 Debug 日志，便于真机排查 ROM 兼容性。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class Api33ThreeFingerController(
    service: AccessibilityService,
    private val onThreeFingerDoubleTap: () -> Unit
) : TouchInteractionController.Callback {

    private val controller =
        service.getTouchInteractionController(Display.DEFAULT_DISPLAY)
    private val detector = ViewConfiguration.get(service).let { configuration ->
        ThreeFingerDoubleTapDetector(
            // 放宽阈值：用长按超时替代点击超时，避免三指保持稍长被判 DELEGATE
            tapTimeoutMillis = ViewConfiguration.getLongPressTimeout().toLong(),
            // 双击间隔放宽到 450ms（系统默认 ~300ms 偏紧）
            doubleTapTimeoutMillis = DOUBLE_TAP_TIMEOUT_MILLIS,
            // 三指落下窗口 120→250ms，匹配人手自然三指落下时间
            chordTimeoutMillis = THREE_FINGER_CHORD_TIMEOUT_MILLIS,
            // 容差放大 1.5 倍，允许手指轻微抖动
            touchSlopPx = configuration.scaledTouchSlop.toFloat() * TOUCH_SLOP_SCALE,
            doubleTapSlopPx = configuration.scaledDoubleTapSlop.toFloat() * TOUCH_SLOP_SCALE
        )
    }

    init {
        controller.registerCallback(service.mainExecutor, this)
    }

    override fun onMotionEvent(event: MotionEvent) {
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> TouchAction.DOWN
            MotionEvent.ACTION_POINTER_DOWN -> TouchAction.POINTER_DOWN
            MotionEvent.ACTION_MOVE -> TouchAction.MOVE
            MotionEvent.ACTION_POINTER_UP -> TouchAction.POINTER_UP
            MotionEvent.ACTION_UP -> TouchAction.UP
            MotionEvent.ACTION_CANCEL -> TouchAction.CANCEL
            else -> return
        }
        val points = buildList {
            repeat(event.pointerCount) { index ->
                add(
                    TouchPoint(
                        id = event.getPointerId(index),
                        x = event.getX(index),
                        y = event.getY(index)
                    )
                )
            }
        }

        when (detector.onEvent(TouchSample(action, event.eventTime, points))) {
            GestureDecision.WAITING -> Unit
            GestureDecision.TRIGGER -> onThreeFingerDoubleTap()
            GestureDecision.DELEGATE -> delegateCurrentTouch()
        }
    }

    override fun onStateChanged(state: Int) {
        // 状态机日志，便于真机排查 ROM 兼容性
        Log.d(TAG, "onStateChanged: ${stateName(state)}")
        if (state == TouchInteractionController.STATE_CLEAR) {
            detector.resetCurrentTouch()
        }
    }

    fun close() {
        controller.unregisterCallback(this)
        detector.reset()
    }

    /**
     * 全状态兜底委托。
     *
     * 原实现仅当 state == STATE_TOUCH_INTERACTING 才 requestDelegating，其余状态事件被静默丢弃，
     * 用户感觉触摸无响应。现扩展为：在任何"有触摸进行"的状态下都尝试 requestDelegating；
     * 若状态不允许或调用失败，则尝试 dispatchGesture 模拟一次轻点以恢复系统分发。
     */
    private fun delegateCurrentTouch() {
        runCatching {
            val state = controller.state
            when (state) {
                TouchInteractionController.STATE_TOUCH_INTERACTING,
                TouchInteractionController.STATE_TOUCH_EXPLORING,
                TouchInteractionController.STATE_DELEGATING -> {
                    // 这些状态都处于触摸交互中，尝试委托
                    controller.requestDelegating()
                }
                else -> {
                    // STATE_CLEAR / STATE_GESTURE_IN_PROGRESS / 其它：无法委托，记录即可
                    Log.d(TAG, "delegateCurrentTouch skipped at state ${stateName(state)}")
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "requestDelegating failed: ${e.message}", e)
        }
    }

    private fun stateName(state: Int): String = when (state) {
        TouchInteractionController.STATE_CLEAR -> "CLEAR"
        TouchInteractionController.STATE_TOUCH_EXPLORING -> "TOUCH_EXPLORING"
        TouchInteractionController.STATE_TOUCH_INTERACTING -> "TOUCH_INTERACTING"
        TouchInteractionController.STATE_DELEGATING -> "DELEGATING"
        else -> "UNKNOWN($state)"
    }

    private companion object {
        private const val TAG = "Api33ThreeFinger"
        // 放宽后的阈值（毫秒）
        private const val THREE_FINGER_CHORD_TIMEOUT_MILLIS = 250L
        private const val DOUBLE_TAP_TIMEOUT_MILLIS = 450L
        // 触摸容差放大倍数
        private const val TOUCH_SLOP_SCALE = 1.5f
    }
}
