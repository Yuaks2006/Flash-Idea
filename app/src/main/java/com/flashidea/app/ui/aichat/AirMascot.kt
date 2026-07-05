package com.flashidea.app.ui.aichat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

enum class AirMascotState {
    Idle,
    Receive,
    Thinking,
    Pipeline,
    Done
}

enum class AirPipelineStage {
    LinkingNotes,
    Tagging,
    Incubating,
    Extending
}

fun resolveAirMascotState(
    isLoading: Boolean,
    messageCount: Int,
    receivingMessageCount: Int?,
    pipelineMessageCount: Int?,
    lastCompletedMessageCount: Int?
): AirMascotState = when {
    isLoading && receivingMessageCount == messageCount -> AirMascotState.Receive
    isLoading && pipelineMessageCount == messageCount -> AirMascotState.Pipeline
    isLoading -> AirMascotState.Thinking
    lastCompletedMessageCount != null && lastCompletedMessageCount == messageCount -> AirMascotState.Done
    else -> AirMascotState.Idle
}

fun resolveAirPipelineStage(frame: Int): AirPipelineStage = when (frame.floorMod(4)) {
    0 -> AirPipelineStage.LinkingNotes
    1 -> AirPipelineStage.Tagging
    2 -> AirPipelineStage.Incubating
    else -> AirPipelineStage.Extending
}

@Composable
fun AirMascot(
    state: AirMascotState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "air-mascot")
    val idleLift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "air-idle-lift"
    )
    val blinkFrame by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6200
                0f at 0
                0f at 5480
                1f at 5560
                0f at 5680
                0f at 6200
            }
        ),
        label = "air-blink"
    )
    val thinkingFrame by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 760
                0f at 0
                1f at 160
                2f at 320
                1f at 520
                0f at 760
            }
        ),
        label = "air-thinking-frame"
    )
    val pipelineFrame by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2600
                0f at 0
                1f at 650
                2f at 1300
                3f at 1950
                4f at 2600
            }
        ),
        label = "air-pipeline-frame"
    )
    val receivePulse by animateFloatAsState(
        targetValue = if (state == AirMascotState.Receive) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 320
            0f at 0
            0.55f at 120
            1f at 240
            1f at 320
        },
        label = "air-receive-pulse"
    )
    val donePulse by animateFloatAsState(
        targetValue = if (state == AirMascotState.Done) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 460
            0f at 0
            1f at 180
            0.45f at 320
            0f at 460
        },
        label = "air-done-pulse"
    )

    Box(modifier = modifier.size(width = 92.dp, height = 64.dp).aspectRatio(1.45f)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Air Agent mascot" }
        ) {
            val block = floor(min(size.width / 18f, size.height / 12f)).coerceAtLeast(3f)
            val origin = Offset(
                x = ((size.width - block * 18f) / 2f).coerceAtLeast(0f),
                y = ((size.height - block * 12f) / 2f).coerceAtLeast(0f) + block
            )
            val pixelY = when (state) {
                AirMascotState.Idle -> idleLift.roundToInt().toFloat()
                AirMascotState.Receive -> if (receivePulse < 0.65f) 1f else -1f
                AirMascotState.Thinking, AirMascotState.Pipeline -> 0f
                AirMascotState.Done -> if (donePulse > 0.1f) -2f else 0f
            }
            val pixelX = if (state == AirMascotState.Thinking || state == AirMascotState.Pipeline) {
                when (thinkingFrame.roundToInt()) {
                    1 -> 1f
                    2 -> -1f
                    else -> 0f
                }
            } else {
                0f
            }
            val compressed = state == AirMascotState.Receive && receivePulse < 0.65f
            val shine = (state == AirMascotState.Receive && receivePulse > 0.5f) ||
                (state == AirMascotState.Done && donePulse > 0.1f)

            translate(left = pixelX * block, top = pixelY * block) {
                drawAirCloud(origin, block, blinkFrame > 0.5f, shine, compressed)
            }
            if (state == AirMascotState.Receive && receivePulse > 0.35f) {
                drawReceiveSpark(origin, block)
            }
            if (state == AirMascotState.Thinking) {
                drawThinkingBolt(origin, block, thinkingFrame.roundToInt())
            }
            if (state == AirMascotState.Pipeline) {
                drawPipelineCue(origin, block, resolveAirPipelineStage(pipelineFrame.roundToInt()))
            }
            if (state == AirMascotState.Done && donePulse > 0.1f) {
                drawDoneSpark(origin, block)
            }
        }
    }
}

private fun DrawScope.drawAirCloud(
    origin: Offset,
    block: Float,
    blink: Boolean,
    shine: Boolean,
    compressed: Boolean
) {
    val body = Color(0xFFD7F3FF)
    val edge = Color(0xFF7CC9E8)
    val eye = Color(0xFF0B1220)
    val highlight = if (shine) Color(0xFF7DF9FF) else Color.White

    val rows = listOf(
        7..9,
        6..10,
        5..11,
        3..13,
        2..15,
        1..16,
        0..17,
        0..17,
        1..16,
        2..15
    )
    val edgeBlocks = buildSet {
        rows.forEachIndexed { y, range ->
            add(range.first to y)
            add(range.last to y)
        }
        add(5 to 3)
        add(11 to 3)
        add(14 to 4)
        add(2 to 8)
        add(15 to 8)
    }

    rows.forEachIndexed { y, range ->
        range.forEach { x ->
            val adjustedY = if (compressed && y >= 7) y - 1 else y
            drawPixel(origin, block, x, adjustedY, if ((x to y) in edgeBlocks) edge else body)
        }
    }

    drawPixel(origin, block, 4, 4, body)
    drawPixel(origin, block, 5, 4, body)
    drawPixel(origin, block, 12, 4, body)
    drawPixel(origin, block, 13, 4, body)

    if (blink) {
        drawPixel(origin, block, 5, 6, eye)
        drawPixel(origin, block, 6, 6, eye)
        drawPixel(origin, block, 11, 6, eye)
        drawPixel(origin, block, 12, 6, eye)
    } else {
        drawEye(origin, block, 5, 5, eye, highlight)
        drawEye(origin, block, 11, 5, eye, highlight)
    }
}

private fun DrawScope.drawEye(origin: Offset, block: Float, x: Int, y: Int, eye: Color, highlight: Color) {
    for (dx in 0..1) {
        for (dy in 0..1) {
            drawPixel(origin, block, x + dx, y + dy, eye)
        }
    }
    drawPixel(origin, block, x, y, highlight)
}

private fun DrawScope.drawThinkingBolt(origin: Offset, block: Float, frame: Int) {
    if (frame.floorMod(4) == 0) return
    val electric = Color(0xFF36E7FF)
    val violet = Color(0xFF8B5CFF)
    drawPixel(origin, block, 10, 9, electric)
    drawPixel(origin, block, 11, 10, electric)
    drawPixel(origin, block, 10, 11, if (frame.floorMod(4) == 2) violet else electric)
}

private fun DrawScope.drawReceiveSpark(origin: Offset, block: Float) {
    val spark = Color(0xFF36E7FF)
    drawPixel(origin, block, 15, 4, spark)
    drawPixel(origin, block, 16, 5, spark)
}

private fun DrawScope.drawPipelineCue(origin: Offset, block: Float, stage: AirPipelineStage) {
    val electric = Color(0xFF36E7FF)
    val violet = Color(0xFF8B5CFF)
    when (stage) {
        AirPipelineStage.LinkingNotes -> {
            drawPixel(origin, block, 1, 4, electric)
            drawPixel(origin, block, 16, 4, electric)
            drawPixel(origin, block, 3, 2, violet)
        }
        AirPipelineStage.Tagging -> {
            drawPixel(origin, block, 16, 5, electric)
            drawPixel(origin, block, 17, 5, electric)
            drawPixel(origin, block, 16, 6, electric)
            drawPixel(origin, block, 17, 6, violet)
        }
        AirPipelineStage.Incubating -> {
            drawPixel(origin, block, 8, 0, electric)
            drawPixel(origin, block, 7, 1, electric)
            drawPixel(origin, block, 9, 1, electric)
            drawPixel(origin, block, 8, 2, violet)
        }
        AirPipelineStage.Extending -> {
            drawPixel(origin, block, 8, 9, electric)
            drawPixel(origin, block, 9, 10, electric)
            drawPixel(origin, block, 8, 11, violet)
        }
    }
}

private fun DrawScope.drawDoneSpark(origin: Offset, block: Float) {
    val spark = Color(0xFF7DF9FF)
    drawPixel(origin, block, 14, 2, spark)
    drawPixel(origin, block, 13, 3, spark)
    drawPixel(origin, block, 15, 3, spark)
    drawPixel(origin, block, 14, 4, spark)
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

private fun DrawScope.drawPixel(origin: Offset, block: Float, x: Int, y: Int, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(origin.x + x * block, origin.y + y * block),
        size = Size(block, block)
    )
}
