package com.flashidea.app.ui.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.local.LinkEntity
import com.flashidea.app.ui.common.QuietBackground
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    onNavigateToNoteDetail: (noteId: String) -> Unit,
    onNavigateToInsightFeed: () -> Unit,
    viewModel: GraphViewModel = hiltViewModel()
) {
    val ideas by viewModel.ideas.collectAsState()
    val links by viewModel.links.collectAsState()
    val hasUnreadInsights by viewModel.hasUnreadInsights.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedIdeaId by remember { mutableStateOf<String?>(null) }
    val selectedIdea = remember(selectedIdeaId, ideas) {
        ideas.find { it.id == selectedIdeaId }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Snackbar 响应 analysisState ──────────────────────────────────────────
    LaunchedEffect(analysisState) {
        when (val s = analysisState) {
            is AnalysisState.Done -> {
                scope.launch {
                    snackbarHostState.showSnackbar("已生成 ${s.linkCount} 条关联 / ${s.insightCount} 条洞察")
                }
                viewModel.resetAnalysisState()
            }
            is AnalysisState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar("分析失败：${s.message}")
                }
                viewModel.resetAnalysisState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                ),
                actions = {
                    val isRunning = analysisState is AnalysisState.Running
                    IconButton(
                        onClick = { viewModel.analyzeAllLinks() },
                        enabled = !isRunning && ideas.isNotEmpty()
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "分析关联",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = hasUnreadInsights,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = onNavigateToInsightFeed) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.tertiary)
                                }
                            ) {
                                Icon(
                                    Icons.Default.BubbleChart,
                                    contentDescription = "灵感孵化",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        QuietBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            if (ideas.isEmpty()) {
                EmptyGraphState()
            } else {
                GraphCanvas(
                    ideas = ideas,
                    links = links,
                    selectedId = selectedIdeaId,
                    onNodeSelected = { id -> selectedIdeaId = id }
                )
            }

            AnimatedVisibility(
                visible = analysisState is AnalysisState.Running,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            }
        }
    }

    // ── 节点详情 BottomSheet ────────────────────────────────────────────────
    if (selectedIdea != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedIdeaId = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                )
            }
        ) {
            NodeDetailCard(
                idea = selectedIdea,
                linkCount = links.count { it.sourceId == selectedIdea.id || it.targetId == selectedIdea.id },
                onViewNote = {
                    selectedIdeaId = null
                    onNavigateToNoteDetail(selectedIdea.id)
                }
            )
        }
    }
}

// ── 图谱 Canvas ──────────────────────────────────────────────────────────────

@Composable
private fun GraphCanvas(
    ideas: List<IdeaEntity>,
    links: List<LinkEntity>,
    selectedId: String?,
    onNodeSelected: (String?) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    var viewport by remember { mutableStateOf(GraphViewport(scale = 0.72f, offsetX = 0f, offsetY = 0f)) }
    var draggingNodeId by remember { mutableStateOf<String?>(null) }

    // 力导向布局坐标（世界坐标），key = id 变化时重算
    var positions by remember(ideas.map { it.id }, links.map { it.id }) {
        mutableStateOf(ForceLayout.compute(ideas, links))
    }

    // 预计算节点度数
    val degree: Map<String, Int> = remember(links) {
        buildMap {
            links.forEach { link ->
                merge(link.sourceId, 1, Int::plus)
                merge(link.targetId, 1, Int::plus)
            }
        }
    }
    val maxDeg = remember(degree) { degree.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f }

    val minRadiusPx = with(density) { 20.dp.toPx() }
    val maxRadiusPx = with(density) { 32.dp.toPx() }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    fun hitTestNode(screenPoint: Offset): IdeaEntity? {
        val world = viewport.screenToWorld(screenPoint.x, screenPoint.y)
        return ideas.firstOrNull { idea ->
            val pos = positions[idea.id] ?: return@firstOrNull false
            val d = degree[idea.id] ?: 0
            val ratio = if (maxDeg > 1f) d / maxDeg else 0f
            val radius = minRadiusPx + ratio * (maxRadiusPx - minRadiusPx)
            val dx = world.x - pos.x
            val dy = world.y - pos.y
            sqrt(dx * dx + dy * dy) <= radius + 12f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = viewport.scale
                    scaleY = viewport.scale
                    translationX = viewport.offsetX
                    translationY = viewport.offsetY
                }
        ) {
            val gridStep = 96f
            var gx = 0f
            while (gx <= 1200f) {
                drawLine(
                    color = outlineVariant.copy(alpha = 0.13f),
                    start = Offset(gx, 0f),
                    end = Offset(gx, 1200f),
                    strokeWidth = 1f
                )
                gx += gridStep
            }
            var gy = 0f
            while (gy <= 1200f) {
                drawLine(
                    color = outlineVariant.copy(alpha = 0.13f),
                    start = Offset(0f, gy),
                    end = Offset(1200f, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }

            // ── 先画边 ───────────────────────────────────────────────────────
            links.forEach { link ->
                val a = positions[link.sourceId] ?: return@forEach
                val b = positions[link.targetId] ?: return@forEach
                val lineColor = if (link.strength >= 0.7f) {
                    primary.copy(alpha = 0.5f)
                } else {
                    outlineVariant.copy(alpha = 0.34f)
                }
                val strokeWidth = 0.8f + link.strength * 2.0f
                drawLine(
                    color = lineColor,
                    start = a,
                    end = b,
                    strokeWidth = strokeWidth
                )
            }

            // ── 再画节点 + 标签 ───────────────────────────────────────────────
            ideas.forEach { idea ->
                val pos = positions[idea.id] ?: return@forEach
                val deg = degree[idea.id] ?: 0
                val ratio = if (maxDeg > 1f) deg / maxDeg else 0f
                val radius = minRadiusPx + ratio * (maxRadiusPx - minRadiusPx)

                val fillColor = when (idea.category) {
                    "项目火种" -> primary
                    "任务" -> tertiary
                    else -> secondary
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            fillColor.copy(alpha = 0.96f),
                            fillColor.copy(alpha = 0.62f),
                            surfaceVariant.copy(alpha = 0.54f)
                        ),
                        center = Offset(pos.x - radius * 0.38f, pos.y - radius * 0.42f),
                        radius = radius * 1.8f
                    ),
                    radius = radius,
                    center = pos
                )
                drawCircle(
                    color = onSurface.copy(alpha = 0.08f),
                    radius = radius,
                    center = pos,
                    style = Stroke(width = 1.2f)
                )

                // 选中描边
                if (idea.id == selectedId) {
                    drawCircle(
                        color = secondary.copy(alpha = 0.16f),
                        radius = radius + 18f,
                        center = pos
                    )
                    drawCircle(
                        color = secondary,
                        radius = radius + 5f,
                        center = pos,
                        style = Stroke(width = 3f)
                    )
                }

                if (idea.id == draggingNodeId) {
                    drawCircle(
                        color = primary.copy(alpha = 0.22f),
                        radius = radius + 24f,
                        center = pos
                    )
                }

                // 节点标签（11sp）
                val label = (idea.summary.takeIf { it.isNotBlank() } ?: idea.content)
                    .take(8)
                val textResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        color = labelColor,
                        fontSize = 11.sp
                    )
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(
                        x = pos.x - textResult.size.width / 2f,
                        y = pos.y + radius + 4f
                    )
                )
            }
        }

        // ── 手势层：拖动画布、双指缩放、点击节点、长按拖动节点 ───────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(ideas, positions, viewport) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (draggingNodeId == null) {
                            viewport = viewport
                                .zoomAround(
                                    centroidX = centroid.x,
                                    centroidY = centroid.y,
                                    zoom = zoom,
                                    minScale = 0.34f,
                                    maxScale = 4.8f
                                )
                                .panBy(pan.x, pan.y)
                        }
                    }
                }
                .pointerInput(ideas, positions, viewport, selectedId) {
                    detectTapGestures(
                        onDoubleTap = { tapScreen ->
                            val targetZoom = if (viewport.scale < 1.2f) 1.9f else 0.72f / viewport.scale
                            viewport = viewport.zoomAround(
                                centroidX = tapScreen.x,
                                centroidY = tapScreen.y,
                                zoom = targetZoom,
                                minScale = 0.34f,
                                maxScale = 4.8f
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onTap = { tapScreen ->
                            val hit = hitTestNode(tapScreen)
                            if (hit != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNodeSelected(if (hit?.id == null || hit.id == selectedId) null else hit.id)
                        }
                    )
                }
                .pointerInput(ideas, positions, viewport) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start ->
                            draggingNodeId = hitTestNode(start)?.id
                            if (draggingNodeId != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragEnd = { draggingNodeId = null },
                        onDragCancel = { draggingNodeId = null },
                        onDrag = { change, dragAmount ->
                            val nodeId = draggingNodeId
                            if (nodeId != null) {
                                val current = positions[nodeId]
                                if (current != null) {
                                    positions = positions + (
                                        nodeId to Offset(
                                            current.x + dragAmount.x / viewport.scale,
                                            current.y + dragAmount.y / viewport.scale
                                        )
                                    )
                                }
                                change.consume()
                            }
                        }
                    )
                }
        )

        TextButton(
            onClick = { viewport = GraphViewport(scale = 0.72f, offsetX = 0f, offsetY = 0f) },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Text("${(viewport.scale * 100).roundToInt()}%")
        }
    }
}

// ── 节点详情卡片 ──────────────────────────────────────────────────────────────

@Composable
private fun NodeDetailCard(
    idea: IdeaEntity,
    linkCount: Int,
    onViewNote: () -> Unit
) {
    val gson = remember { Gson() }
    val tags: List<String> = remember(idea.tags) {
        runCatching {
            gson.fromJson<List<String>>(idea.tags, object : TypeToken<List<String>>() {}.type)
        }.getOrDefault(emptyList())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category 芯片
        if (idea.category.isNotBlank()) {
            val chipColor = when (idea.category) {
                "项目火种" -> MaterialTheme.colorScheme.primary
                "任务" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.secondary
            }
            Surface(
                color = chipColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = idea.category,
                    color = chipColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // 摘要 / 内容
        val displayText = idea.summary.takeIf { it.isNotBlank() } ?: idea.content
        Text(
            text = displayText,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        // 标签行
        if (tags.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.take(5).forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // 关联数量提示
        if (linkCount > 0) {
            Text(
                text = "与 $linkCount 条笔记有关联",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // 查看笔记按钮
        Button(
            onClick = onViewNote,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("查看笔记")
        }
    }
}

// ── 空态 ──────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyGraphState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BubbleChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ── 力导向布局 ─────────────────────────────────────────────────────────────────

private object ForceLayout {

    private const val ITERATIONS = 80
    private const val DAMPING = 0.85f
    private const val CANVAS_SIZE = 1200f  // 世界坐标范围
    private const val PADDING = 80f

    fun compute(ideas: List<IdeaEntity>, links: List<LinkEntity>): Map<String, Offset> {
        if (ideas.isEmpty()) return emptyMap()
        if (ideas.size == 1) {
            return mapOf(ideas[0].id to Offset(CANVAS_SIZE / 2f, CANVAS_SIZE / 2f))
        }

        val n = ideas.size
        val area = CANVAS_SIZE * CANVAS_SIZE
        val k = sqrt(area / n.toFloat()) * 0.8f  // 理想间距

        // 初始位置：网格播种，避免全部堆在一起
        val pos = mutableMapOf<String, FloatArray>()
        val cols = maxOf(1, sqrt(n.toFloat()).toInt())
        val step = (CANVAS_SIZE - 2 * PADDING) / cols
        ideas.forEachIndexed { i, idea ->
            val col = i % cols
            val row = i / cols
            pos[idea.id] = floatArrayOf(
                PADDING + col * step + step * 0.5f + (Math.random() * 20 - 10).toFloat(),
                PADDING + row * step + step * 0.5f + (Math.random() * 20 - 10).toFloat()
            )
        }

        val disp = mutableMapOf<String, FloatArray>()

        repeat(ITERATIONS) { iter ->
            // 阻尼随迭代衰减
            val temp = k * (1f - iter.toFloat() / ITERATIONS) * DAMPING

            ideas.forEach { it -> disp[it.id] = floatArrayOf(0f, 0f) }

            // 斥力（所有节点对）
            for (i in ideas.indices) {
                for (j in i + 1 until ideas.size) {
                    val vi = pos[ideas[i].id] ?: continue
                    val vj = pos[ideas[j].id] ?: continue
                    val dx = vi[0] - vj[0]
                    val dy = vi[1] - vj[1]
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
                    val repForce = k * k / dist
                    val nx = dx / dist * repForce
                    val ny = dy / dist * repForce
                    disp[ideas[i].id]?.let { d -> d[0] += nx; d[1] += ny }
                    disp[ideas[j].id]?.let { d -> d[0] -= nx; d[1] -= ny }
                }
            }

            // 引力（沿边）
            links.forEach { link ->
                val vs = pos[link.sourceId] ?: return@forEach
                val vt = pos[link.targetId] ?: return@forEach
                val dx = vt[0] - vs[0]
                val dy = vt[1] - vs[1]
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
                val attForce = dist * dist / k
                val nx = dx / dist * attForce
                val ny = dy / dist * attForce
                disp[link.sourceId]?.let { d -> d[0] += nx; d[1] += ny }
                disp[link.targetId]?.let { d -> d[0] -= nx; d[1] -= ny }
            }

            // 应用位移（限幅 + 边界约束）
            ideas.forEach { idea ->
                val p = pos[idea.id] ?: return@forEach
                val d = disp[idea.id] ?: return@forEach
                val dispLen = sqrt(d[0] * d[0] + d[1] * d[1]).coerceAtLeast(0.01f)
                val capped = minOf(dispLen, temp)
                p[0] += d[0] / dispLen * capped
                p[1] += d[1] / dispLen * capped
                // 边界约束
                p[0] = p[0].coerceIn(PADDING, CANVAS_SIZE - PADDING)
                p[1] = p[1].coerceIn(PADDING, CANVAS_SIZE - PADDING)
            }
        }

        return buildMap {
            ideas.forEach { idea ->
                val p = pos[idea.id] ?: return@forEach
                put(idea.id, Offset(p[0], p[1]))
            }
        }
    }
}
