package com.flashidea.app.ui.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
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
import kotlin.math.hypot
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
            val systemBottomPadding = WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = systemBottomPadding + 88.dp)
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    // ── 视图状态：MutableState 持有，pointerInput(k=Unit) 内部通过 .value 读写最新值，避免重建
    val viewportState = remember { mutableStateOf(GraphViewport(scale = 0.72f, offsetX = 0f, offsetY = 0f)) }
    val positionsState = remember(ideas.map { it.id }, links.map { it.id }) {
        mutableStateOf(ForceLayout.compute(ideas, links))
    }
    val draggingNodeId = remember { mutableStateOf<String?>(null) }
    val selectedIdRef = rememberUpdatedState(selectedId)

    // ── 力导向布局坐标（世界坐标），key = id 变化时重算 ──
    val degree: Map<String, Int> = remember(links) {
        buildMap {
            links.forEach { link ->
                merge(link.sourceId, 1, Int::plus)
                merge(link.targetId, 1, Int::plus)
            }
        }
    }
    val maxDeg = remember(degree) { degree.values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f }
    val degreeRef = rememberUpdatedState(degree)
    val maxDegRef = rememberUpdatedState(maxDeg)

    val minRadiusPx = with(density) { 20.dp.toPx() }
    val maxRadiusPx = with(density) { 32.dp.toPx() }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val shadowColor = Color.Black

    // 性能：category 颜色 & Brush 缓存（每种 category 复用同一 Brush）
    val brushCache: Map<String, Brush> = remember(primary, secondary, tertiary, surfaceVariant) {
        mapOf(
            "项目火种" to Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.96f), primary.copy(alpha = 0.62f), surfaceVariant.copy(alpha = 0.54f)),
                center = Offset(0.42f, 0.38f)
            ),
            "任务" to Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.96f), tertiary.copy(alpha = 0.62f), surfaceVariant.copy(alpha = 0.54f)),
                center = Offset(0.42f, 0.38f)
            )
        )
    }
    val defaultBrush = remember(secondary, surfaceVariant) {
        Brush.radialGradient(
            colors = listOf(secondary.copy(alpha = 0.96f), secondary.copy(alpha = 0.62f), surfaceVariant.copy(alpha = 0.54f)),
            center = Offset(0.42f, 0.38f)
        )
    }

    // 性能：节点图标预渲染为 ImageBitmap，按 category 复用
    val iconSizePx = with(density) { 18.dp.toPx() }

    // 性能：节点标签 TextLayoutResult 缓存（按 idea id+summary+category 指纹重置）
    val textFingerprint = ideas.map { it.id + "|" + it.summary + "|" + it.category }.hashCode()
    val textCache = remember(textFingerprint) { mutableMapOf<String, androidx.compose.ui.text.TextLayoutResult>() }

    fun hitTestNodeScreen(screenPoint: Offset, v: GraphViewport): IdeaEntity? {
        val world = v.screenToWorld(screenPoint.x, screenPoint.y)
        val hitTolerance = with(density) { NODE_HIT_RADIUS_DP.dp.toPx() }
        return ideas.firstOrNull { idea ->
            val pos = positionsState.value[idea.id] ?: return@firstOrNull false
            val d = degreeRef.value[idea.id] ?: 0
            val ratio = if (maxDegRef.value > 1f) d / maxDegRef.value else 0f
            val radius = minRadiusPx + ratio * (maxRadiusPx - minRadiusPx)
            val dx = world.x - pos.x
            val dy = world.y - pos.y
            sqrt(dx * dx + dy * dy) <= radius + hitTolerance
        }
    }
    val hitTestFun = ::hitTestNodeScreen

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = viewportState.value.scale
                    scaleY = viewportState.value.scale
                    translationX = viewportState.value.offsetX
                    translationY = viewportState.value.offsetY
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
                val a = positionsState.value[link.sourceId] ?: return@forEach
                val b = positionsState.value[link.targetId] ?: return@forEach
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

            // ── 再画节点 + 阴影 + 图标 + 标签 ───────────────────────────────────
            ideas.forEach { idea ->
                val pos = positionsState.value[idea.id] ?: return@forEach
                val deg = degree[idea.id] ?: 0
                val ratio = if (maxDeg > 1f) deg / maxDeg else 0f
                val radius = minRadiusPx + ratio * (maxRadiusPx - minRadiusPx)

                // 立体感：原生 ShadowLayer 通过 drawIntoCanvas + nativeCanvas 绘制
                val blurPx = 6.dp.toPx()
                val dyPx = 4.dp.toPx()
                drawIntoCanvas { canvas ->
                    val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = shadowColor.toArgb()
                        setShadowLayer(
                            blurPx,
                            0f,
                            dyPx,
                            0x33000000
                        )
                    }
                    canvas.nativeCanvas.drawCircle(pos.x, pos.y, radius - 1f, shadowPaint)
                }

                // 主填充：复用按 category 缓存的径向渐变 Brush
                drawCircle(
                    brush = brushCache[idea.category] ?: defaultBrush,
                    radius = radius,
                    center = pos
                )
                // 描边 alpha 0.08 → 0.25
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius,
                    center = pos,
                    style = Stroke(width = 1.4f)
                )

                // 选中态描边
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

                // 拖动节点局部高亮外圈（radius + 4dp 半透明环）
                if (idea.id == draggingNodeId.value) {
                    drawCircle(
                        color = primary.copy(alpha = 0.28f),
                        radius = radius + 4.dp.toPx(),
                        center = pos,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // 节点标签：take(16) + 最多 2 行 + 椭圆省略；按 idea id 缓存 measure 结果
                val label = (idea.summary.takeIf { it.isNotBlank() } ?: idea.content).take(16)
                val maxLabeWidthPx = (radius * 3f).toInt().coerceAtLeast(1)
                val result = textCache.getOrPut(idea.id) {
                    textMeasurer.measure(
                        text = label,
                        style = TextStyle(color = labelColor, fontSize = 11.sp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        softWrap = true,
                        constraints = androidx.compose.ui.unit.Constraints(
                            maxWidth = maxLabeWidthPx,
                            maxHeight = Int.MAX_VALUE
                        )
                    )
                }
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(
                        x = pos.x - result.size.width / 2f,
                        y = pos.y + radius + 4f
                    )
                )
            }
        }

        // ── 手势统一层：单 pointerInput(Unit)，合并 描述 pan/zoom/tap/drag ──────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitPointerEventScope {
                        var dragNode: String? = null
                        var downScreen: Offset? = null
                        var lastSinglePos: Offset? = null
                        var lastDownTimeMs = 0L
                        var dragged = false
                        var prevZoomDist: Float? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes.map { it.position }
                            val n = pointers.size
                            val v = viewportState.value
                            when (event.type) {
                                PointerEventType.Press -> {
                                    val hit = hitTestFun(pointers.first(), v)
                                    dragNode = hit?.id
                                    if (dragNode != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    downScreen = pointers.first()
                                    lastDownTimeMs = System.currentTimeMillis()
                                    lastSinglePos = pointers.first()
                                    dragged = false
                                    prevZoomDist = null
                                }
                                PointerEventType.Move -> {
                                    if (n >= 2) {
                                        // 双指缩放
                                        val dist = (pointers[0] - pointers[1]).getDistance()
                                        val centroid = (pointers[0] + pointers[1]) / 2f
                                        prevZoomDist?.let { pd ->
                                            if (pd > 0.01f) {
                                                val z = (dist / pd).coerceIn(0.7f, 1.5f)
                                                viewportState.value = v.zoomAround(
                                                    centroid.x, centroid.y, z, 0.34f, 4.8f
                                                )
                                            }
                                        }
                                        prevZoomDist = dist
                                        dragged = true
                                        dragNode = null
                                    } else if (n == 1) {
                                        // 单指：拖动节点 或 平移画布
                                        val now = pointers[0]
                                        val dx = now.x - (lastSinglePos ?: now).x
                                        val dy = now.y - (lastSinglePos ?: now).y
                                        val node = dragNode
                                        if (node != null) {
                                            val s = v.scale.coerceAtLeast(0.01f)
                                            val cur = positionsState.value[node]
                                            if (cur != null) {
                                                positionsState.value = positionsState.value + (node to Offset(cur.x + dx / s, cur.y + dy / s))
                                            }
                                            if (hypot(dx, dy) > touchSlop) dragged = true
                                        } else if (hypot(dx, dy) > touchSlop) {
                                            dragged = true
                                            viewportState.value = v.panBy(dx, dy)
                                        }
                                        lastSinglePos = now
                                    }
                                }
                                PointerEventType.Release, PointerEventType.Exit -> {
                                    val release = pointers.firstOrNull() ?: downScreen
                                    if (!dragged && release != null && downScreen != null) {
                                        val dt = System.currentTimeMillis() - lastDownTimeMs
                                        val moved = (release - downScreen).getDistance()
                                        if (dt < 250 && moved < touchSlop) {
                                            // tap：命中节点则切换选中，否则取消选中
                                            val hit = hitTestFun(release, viewportState.value)
                                            if (hit != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNodeSelected(
                                                if (hit?.id == null || hit.id == selectedIdRef.value) null else hit.id
                                            )
                                        }
                                    }
                                    dragNode = null
                                    prevZoomDist = null
                                    lastSinglePos = null
                                    downScreen = null
                                    dragged = false
                                }
                                else -> Unit
                            }
                        }
                    }
                }
        )

        TextButton(
            onClick = { viewportState.value = GraphViewport(scale = 0.72f, offsetX = 0f, offsetY = 0f) },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Text("${(viewportState.value.scale * 100).roundToInt()}%")
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
