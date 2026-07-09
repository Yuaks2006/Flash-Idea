package com.flashidea.app.ui.aichat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.ui.common.QuietBackground
import com.flashidea.app.ui.common.QuietPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(viewModel: AiChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val input by viewModel.input.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val modelConfig by viewModel.modelConfig.collectAsState()
    val contextChips by viewModel.contextChips.collectAsState()
    val contextNotes by viewModel.contextNotes.collectAsState()
    val selectedCount by viewModel.selectedCount.collectAsState()
    val showNotePicker by viewModel.showNotePicker.collectAsState()
    val allIdeas by viewModel.allIdeas.collectAsState()
    val aiPhase by viewModel.aiPhase.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showModelSheet by remember { mutableStateOf(false) }
    var prevSelectedCount by remember { mutableStateOf(0) }

    // 真实流式驱动的 mascot 状态：工具阶段 → Pipeline；生成阶段 → Thinking；空闲 → Idle
    val mascotState = if (isLoading) {
        if (aiPhase != null) AirMascotState.Pipeline else AirMascotState.Thinking
    } else {
        AirMascotState.Idle
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // 流式进行中持续滚动到底部（content 增长时）
    LaunchedEffect(messages.lastOrNull()?.content, isLoading) {
        if (isLoading && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // 引用笔记浮动提示：选中数量增加时弹出「已引用：{摘要}」
    LaunchedEffect(selectedCount) {
        if (selectedCount > prevSelectedCount) {
            val note = contextNotes.lastOrNull()
            val preview = if (note != null) {
                val p = note.content.trim().replace('\n', ' ')
                if (p.length > 16) p.take(16) + "…" else p
            } else "已引用 $selectedCount 篇笔记"
            scope.launch { snackbarHostState.showSnackbar("已引用：$preview") }
        }
        prevSelectedCount = selectedCount
    }

    if (showNotePicker) {
        NotePickerSheet(
            allIdeas = allIdeas,
            isSelected = viewModel::isNoteSelected,
            onSelect = { idea ->
                if (!viewModel.isNoteSelected(idea.id)) {
                    viewModel.toggleNote(idea)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            selectedCount = selectedCount,
            onDone = {
                scope.launch {
                    viewModel.closeNotePicker()
                }
            },
            onDismiss = viewModel::closeNotePicker
        )
    }

    if (showModelSheet) {
        AgentModelSheet(
            modelConfig = modelConfig,
            activeProvider = activeProvider,
            onSelectProvider = viewModel::selectProvider,
            onSaveCustomModel = viewModel::saveCustomModel,
            onDismiss = { showModelSheet = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        QuietBackground {
            // 让出悬浮导航栏 + 系统手势条空间
            val systemBottomPadding = WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .padding(bottom = systemBottomPadding + 88.dp)
            ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    AgentHeader(
                        modelConfig = modelConfig,
                        onOpenModelSheet = { showModelSheet = true },
                        mascotState = mascotState
                    )
                }
                if (aiPhase != null) {
                    item {
                        Text(
                            aiPhase ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                if (messages.isEmpty()) {
                    item {
                        AirEmptySpace(mascotState = mascotState)
                    }
                } else {
                    itemsIndexed(messages) { index, message ->
                        val isLastAssistant = index == messages.lastIndex &&
                            message.role == "assistant" &&
                            isLoading
                        ChatBubble(message, isStreaming = isLastAssistant)
                    }
                }
            }

            ComposerBar(
                input = input,
                isLoading = isLoading,
                contextNotes = contextNotes,
                contextChips = contextChips,
                selectedCount = selectedCount,
                onInputChange = viewModel::onInputChange,
                onOpenNotePicker = viewModel::openNotePicker,
                onRemoveContextNote = viewModel::removeSelectedNote,
                onSend = { viewModel.send() }
            )
            }
        }
    }
}

@Composable
private fun AgentHeader(
    modelConfig: ModelProviderConfig,
    onOpenModelSheet: () -> Unit,
    mascotState: AirMascotState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clickable(onClickLabel = "模型设置", onClick = onOpenModelSheet),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuietPill(
            label = modelConfig.providerType.agentModelOption().status,
            icon = Icons.Default.ExpandMore,
            onClick = onOpenModelSheet
        )
        AirMascot(state = mascotState, modifier = Modifier.size(width = 70.dp, height = 48.dp))
    }
}

@Composable
private fun AirEmptySpace(mascotState: AirMascotState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp),
        contentAlignment = Alignment.Center
    ) {
        AirMascot(state = mascotState, modifier = Modifier.size(width = 118.dp, height = 82.dp))
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isStreaming: Boolean = false) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (isUser) {
                Text(
                    message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    MarkdownText(
                        markdown = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isStreaming) {
                        StreamingCursor()
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingCursor() {
    val transition = rememberInfiniteTransition(label = "streaming-cursor")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor-alpha"
    )
    Text(
        "▍",
        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun ComposerBar(
    input: String,
    isLoading: Boolean,
    contextNotes: List<IdeaEntity>,
    contextChips: List<String>,
    selectedCount: Int,
    onInputChange: (String) -> Unit,
    onOpenNotePicker: () -> Unit,
    onRemoveContextNote: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    )
                )
                .animateContentSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (contextNotes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    contextNotes.zip(contextChips).forEach { (note, chip) ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveContextNote(note.id) },
                            label = { Text(chip, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除引用",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onOpenNotePicker) {
                    if (selectedCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text("$selectedCount") }
                            }
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "引用笔记",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "引用笔记",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("问 Air") },
                    minLines = 1,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                IconButton(
                    onClick = onSend,
                    enabled = input.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentModelSheet(
    modelConfig: ModelProviderConfig,
    activeProvider: String,
    onSelectProvider: (ModelProviderType) -> Unit,
    onSaveCustomModel: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var customBaseUrl by remember(modelConfig.customModel.baseUrl) {
        mutableStateOf(modelConfig.customModel.baseUrl)
    }
    var customApiKey by remember(modelConfig.customModel.apiKey) {
        mutableStateOf(modelConfig.customModel.apiKey)
    }
    var customModelName by remember(modelConfig.customModel.modelName) {
        mutableStateOf(modelConfig.customModel.modelName)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("模型", style = MaterialTheme.typography.titleMedium)
            agentModelOptions().forEach { option ->
                val selected = modelConfig.providerType == option.type
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProvider(option.type) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(option.type.modelIcon(), contentDescription = null, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text(option.status, style = MaterialTheme.typography.titleMedium)
                            Text(
                                option.caption,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = "已选择", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (modelConfig.providerType == ModelProviderType.CustomOpenAi) {
                Text(
                    "第三方兼容",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                TextField(
                    value = customBaseUrl,
                    onValueChange = { customBaseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                TextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                TextField(
                    value = customModelName,
                    onValueChange = { customModelName = it },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                Button(
                    onClick = { onSaveCustomModel(customBaseUrl, customApiKey, customModelName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
            } else {
                Text(
                    activeProvider,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotePickerSheet(
    allIdeas: List<IdeaEntity>,
    isSelected: (String) -> Boolean,
    onSelect: (IdeaEntity) -> Unit,
    selectedCount: Int,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "引用笔记",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(
                onClick = onDone,
                enabled = selectedCount > 0
            ) {
                Text("完成($selectedCount)")
            }
        }
        if (allIdeas.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("还没有可引用的笔记")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(allIdeas, key = { it.id }) { idea ->
                    val selected = isSelected(idea.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .background(if (selected) primaryContainer.copy(alpha = 0.45f) else surface)
                            .clickable { onSelect(idea) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                idea.summary.ifBlank { idea.content },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                idea.category.ifBlank { "未分类" },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ModelProviderType.modelIcon(): ImageVector = when (this) {
    ModelProviderType.OnDeviceFirst -> Icons.Default.Storage
    ModelProviderType.VivoCloud -> Icons.Default.CloudQueue
    ModelProviderType.CustomOpenAi -> Icons.Default.Api
    ModelProviderType.LocalRule -> Icons.Default.Lock
}
