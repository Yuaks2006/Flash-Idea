package com.flashidea.app.service

import kotlin.math.hypot

enum class TouchAction {
    DOWN,
    POINTER_DOWN,
    MOVE,
    POINTER_UP,
    UP,
    CANCEL
}

data class TouchPoint(
    val id: Int,
    val x: Float,
    val y: Float
)

data class TouchSample(
    val action: TouchAction,
    val eventTimeMillis: Long,
    val points: List<TouchPoint>
)

enum class GestureDecision {
    WAITING,
    DELEGATE,
    TRIGGER
}

class ThreeFingerDoubleTapDetector(
    private val tapTimeoutMillis: Long,
    private val doubleTapTimeoutMillis: Long,
    private val chordTimeoutMillis: Long,
    private val touchSlopPx: Float,
    private val doubleTapSlopPx: Float
) {
    private var tapStartMillis = 0L
    private var reachedThreeFingers = false
    private val initialPoints = mutableMapOf<Int, TouchPoint>()
    private var firstTapEndMillis: Long? = null
    private var firstTapCentroid: TouchPoint? = null

    fun onEvent(sample: TouchSample): GestureDecision {
        return when (sample.action) {
            TouchAction.DOWN -> beginTap(sample)
            TouchAction.POINTER_DOWN -> continueTap(sample)
            TouchAction.MOVE -> validateMovement(sample)
            TouchAction.POINTER_UP -> validateMovement(sample)
            TouchAction.UP -> finishTap(sample)
            TouchAction.CANCEL -> delegateAndReset()
        }
    }

    fun resetCurrentTouch() {
        tapStartMillis = 0L
        reachedThreeFingers = false
        initialPoints.clear()
    }

    fun reset() {
        resetCurrentTouch()
        firstTapEndMillis = null
        firstTapCentroid = null
    }

    private fun beginTap(sample: TouchSample): GestureDecision {
        val previousEnd = firstTapEndMillis
        if (previousEnd != null &&
            sample.eventTimeMillis - previousEnd > doubleTapTimeoutMillis
        ) {
            reset()
        } else {
            resetCurrentTouch()
        }
        tapStartMillis = sample.eventTimeMillis
        rememberNewPointers(sample.points)
        return GestureDecision.WAITING
    }

    private fun continueTap(sample: TouchSample): GestureDecision {
        rememberNewPointers(sample.points)
        if (sample.points.size > REQUIRED_FINGER_COUNT) return delegateAndReset()
        if (sample.eventTimeMillis - tapStartMillis > tapTimeoutMillis) {
            return delegateAndReset()
        }
        if (sample.points.size == REQUIRED_FINGER_COUNT) {
            if (sample.eventTimeMillis - tapStartMillis > chordTimeoutMillis) {
                return delegateAndReset()
            }
            reachedThreeFingers = true
        }
        return validateMovement(sample)
    }

    private fun validateMovement(sample: TouchSample): GestureDecision {
        if (sample.eventTimeMillis - tapStartMillis > tapTimeoutMillis) {
            return delegateAndReset()
        }
        val movedTooFar = sample.points.any { point ->
            val initial = initialPoints[point.id] ?: return@any true
            distance(initial, point) > touchSlopPx
        }
        return if (movedTooFar) delegateAndReset() else GestureDecision.WAITING
    }

    private fun finishTap(sample: TouchSample): GestureDecision {
        val duration = sample.eventTimeMillis - tapStartMillis
        if (!reachedThreeFingers || duration > tapTimeoutMillis) {
            return delegateAndReset()
        }

        val centroid = centroid(initialPoints.values.take(REQUIRED_FINGER_COUNT))
        val previousEnd = firstTapEndMillis
        val previousCentroid = firstTapCentroid
        resetCurrentTouch()

        if (previousEnd == null || previousCentroid == null) {
            firstTapEndMillis = sample.eventTimeMillis
            firstTapCentroid = centroid
            return GestureDecision.WAITING
        }

        val isWithinTime =
            sample.eventTimeMillis - previousEnd <= doubleTapTimeoutMillis
        val isWithinDistance =
            distance(previousCentroid, centroid) <= doubleTapSlopPx
        reset()
        return if (isWithinTime && isWithinDistance) {
            GestureDecision.TRIGGER
        } else {
            GestureDecision.DELEGATE
        }
    }

    private fun rememberNewPointers(points: List<TouchPoint>) {
        points.forEach { point -> initialPoints.putIfAbsent(point.id, point) }
    }

    private fun delegateAndReset(): GestureDecision {
        reset()
        return GestureDecision.DELEGATE
    }

    private fun centroid(points: Collection<TouchPoint>): TouchPoint {
        val count = points.size.coerceAtLeast(1)
        return TouchPoint(
            id = -1,
            x = points.sumOf { it.x.toDouble() }.toFloat() / count,
            y = points.sumOf { it.y.toDouble() }.toFloat() / count
        )
    }

    private fun distance(first: TouchPoint, second: TouchPoint): Float =
        hypot(first.x - second.x, first.y - second.y)

    private companion object {
        const val REQUIRED_FINGER_COUNT = 3
    }
}
