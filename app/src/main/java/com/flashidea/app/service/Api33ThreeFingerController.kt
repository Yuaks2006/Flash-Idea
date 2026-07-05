package com.flashidea.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.TouchInteractionController
import android.os.Build
import android.view.Display
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class Api33ThreeFingerController(
    service: AccessibilityService,
    private val onThreeFingerDoubleTap: () -> Unit
) : TouchInteractionController.Callback {

    private val controller =
        service.getTouchInteractionController(Display.DEFAULT_DISPLAY)
    private val detector = ViewConfiguration.get(service).let { configuration ->
        ThreeFingerDoubleTapDetector(
            tapTimeoutMillis = ViewConfiguration.getTapTimeout().toLong(),
            doubleTapTimeoutMillis = ViewConfiguration.getDoubleTapTimeout().toLong(),
            chordTimeoutMillis = THREE_FINGER_CHORD_TIMEOUT_MILLIS,
            touchSlopPx = configuration.scaledTouchSlop.toFloat(),
            doubleTapSlopPx = configuration.scaledDoubleTapSlop.toFloat()
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
        if (state == TouchInteractionController.STATE_CLEAR) {
            detector.resetCurrentTouch()
        }
    }

    fun close() {
        controller.unregisterCallback(this)
        detector.reset()
    }

    private fun delegateCurrentTouch() {
        runCatching {
            if (controller.state == TouchInteractionController.STATE_TOUCH_INTERACTING) {
                controller.requestDelegating()
            }
        }
    }

    private companion object {
        const val THREE_FINGER_CHORD_TIMEOUT_MILLIS = 120L
    }
}
