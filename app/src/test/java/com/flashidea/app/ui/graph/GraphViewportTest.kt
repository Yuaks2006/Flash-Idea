package com.flashidea.app.ui.graph

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphViewportTest {

    @Test
    fun screenToWorldAppliesPanAndScale() {
        val viewport = GraphViewport(scale = 2f, offsetX = 80f, offsetY = 40f)

        val point = viewport.screenToWorld(screenX = 180f, screenY = 140f)

        assertEquals(50f, point.x, 0.001f)
        assertEquals(50f, point.y, 0.001f)
    }

    @Test
    fun zoomAroundKeepsGestureCentroidAnchored() {
        val viewport = GraphViewport(scale = 1f, offsetX = 0f, offsetY = 0f)

        val zoomed = viewport.zoomAround(
            centroidX = 240f,
            centroidY = 360f,
            zoom = 2f,
            minScale = 0.4f,
            maxScale = 4f
        )

        val anchoredWorldPoint = zoomed.screenToWorld(240f, 360f)
        assertEquals(240f, anchoredWorldPoint.x, 0.001f)
        assertEquals(360f, anchoredWorldPoint.y, 0.001f)
    }

    @Test
    fun panByMovesOffsetWithoutChangingScale() {
        val viewport = GraphViewport(scale = 1.6f, offsetX = 10f, offsetY = 20f)

        val moved = viewport.panBy(deltaX = -12f, deltaY = 18f)

        assertEquals(1.6f, moved.scale, 0.001f)
        assertEquals(-2f, moved.offsetX, 0.001f)
        assertEquals(38f, moved.offsetY, 0.001f)
    }
}
