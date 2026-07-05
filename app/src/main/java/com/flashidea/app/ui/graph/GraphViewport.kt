package com.flashidea.app.ui.graph

data class GraphPoint(val x: Float, val y: Float)

data class GraphViewport(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun screenToWorld(screenX: Float, screenY: Float): GraphPoint =
        GraphPoint(
            x = (screenX - offsetX) / scale,
            y = (screenY - offsetY) / scale
        )

    fun panBy(deltaX: Float, deltaY: Float): GraphViewport =
        copy(offsetX = offsetX + deltaX, offsetY = offsetY + deltaY)

    fun zoomAround(
        centroidX: Float,
        centroidY: Float,
        zoom: Float,
        minScale: Float,
        maxScale: Float
    ): GraphViewport {
        val oldWorld = screenToWorld(centroidX, centroidY)
        val newScale = (scale * zoom).coerceIn(minScale, maxScale)
        return copy(
            scale = newScale,
            offsetX = centroidX - oldWorld.x * newScale,
            offsetY = centroidY - oldWorld.y * newScale
        )
    }
}
