package com.flashidea.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ThreeFingerDoubleTapDetectorTest {

    private val detector = ThreeFingerDoubleTapDetector(
        tapTimeoutMillis = 250,
        doubleTapTimeoutMillis = 350,
        chordTimeoutMillis = 120,
        touchSlopPx = 24f,
        doubleTapSlopPx = 80f
    )

    @Test
    fun `two valid three finger taps trigger capture`() {
        val decisions = buildList {
            addAll(threeFingerTap(startTime = 0))
            addAll(threeFingerTap(startTime = 180))
        }.map(detector::onEvent)

        assertEquals(GestureDecision.TRIGGER, decisions.last())
    }

    @Test
    fun `ordinary one finger tap is delegated`() {
        detector.onEvent(event(TouchAction.DOWN, 0, point(0, 100f, 100f)))

        val decision = detector.onEvent(event(TouchAction.UP, 80, point(0, 100f, 100f)))

        assertEquals(GestureDecision.DELEGATE, decision)
    }

    @Test
    fun `three finger movement beyond slop is delegated`() {
        detector.onEvent(event(TouchAction.DOWN, 0, point(0, 100f, 100f)))
        detector.onEvent(
            event(
                TouchAction.POINTER_DOWN,
                30,
                point(0, 100f, 100f),
                point(1, 200f, 100f)
            )
        )
        detector.onEvent(
            event(
                TouchAction.POINTER_DOWN,
                60,
                point(0, 100f, 100f),
                point(1, 200f, 100f),
                point(2, 300f, 100f)
            )
        )

        val decision = detector.onEvent(
            event(
                TouchAction.MOVE,
                90,
                point(0, 150f, 100f),
                point(1, 250f, 100f),
                point(2, 350f, 100f)
            )
        )

        assertEquals(GestureDecision.DELEGATE, decision)
    }

    private fun threeFingerTap(startTime: Long): List<TouchSample> = listOf(
        event(TouchAction.DOWN, startTime, point(0, 100f, 100f)),
        event(
            TouchAction.POINTER_DOWN,
            startTime + 30,
            point(0, 100f, 100f),
            point(1, 200f, 100f)
        ),
        event(
            TouchAction.POINTER_DOWN,
            startTime + 60,
            point(0, 100f, 100f),
            point(1, 200f, 100f),
            point(2, 300f, 100f)
        ),
        event(
            TouchAction.POINTER_UP,
            startTime + 100,
            point(0, 100f, 100f),
            point(1, 200f, 100f),
            point(2, 300f, 100f)
        ),
        event(
            TouchAction.POINTER_UP,
            startTime + 115,
            point(0, 100f, 100f),
            point(1, 200f, 100f)
        ),
        event(TouchAction.UP, startTime + 130, point(0, 100f, 100f))
    )

    private fun event(
        action: TouchAction,
        time: Long,
        vararg points: TouchPoint
    ) = TouchSample(action, time, points.toList())

    private fun point(id: Int, x: Float, y: Float) = TouchPoint(id, x, y)
}
