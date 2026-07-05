package com.flashidea.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.flashidea.app.BuildConfig
import com.flashidea.app.ai.model.ModelRuntimeStatus
import com.flashidea.app.service.FlashGestureService
import com.flashidea.app.ui.common.QuietBackground
import com.flashidea.app.ui.common.QuietDivider
import com.flashidea.app.ui.common.QuietRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val allIdeas by viewModel.allIdeas.collectAsState()
    val modelConfig by viewModel.modelConfig.collectAsState()
    val modelSnapshot = viewModel.modelSnapshot
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var gestureEnabled by remember { mutableStateOf(false) }
    var showModelSheet by rememberSaveable { mutableStateOf(false) }
    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    var customBaseUrl by rememberSaveable { mutableStateOf("") }
    var customApiKey by rememberSaveable { mutableStateOf("") }
    var customModelName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(modelConfig.customModel) {
        customBaseUrl = modelConfig.customModel.baseUrl
        customApiKey = modelConfig.customModel.apiKey
        customModelName = modelConfig.customModel.modelName
    }

    fun refreshSystemState() {
        gestureEnabled = FlashGestureService.isEnabled(context)
    }

    DisposableEffect(lifecycleOwner) {
        refreshSystemState()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshSystemState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("模型配置", style = MaterialTheme.typography.titleMedium)
                Text(
                    modelConfig.strategyLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RuntimeLine("端侧", modelSnapshot.onDeviceStatus)
                RuntimeLine("云端", modelSnapshot.cloudStatus)
                RuntimeLine("第三方", modelSnapshot.customStatus)
                RuntimeLine("本地", modelSnapshot.localStatus)
                OutlinedTextField(
                    value = customBaseUrl,
                    onValueChange = { customBaseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customModelName,
                    onValueChange = { customModelName = it },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.saveCustomModel(customBaseUrl, customApiKey, customModelName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
            }
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("导出", style = MaterialTheme.typography.titleMedium)
                ExportButton("Markdown") {
                    shareText(context, ExportFormat.toMarkdown(allIdeas), "Flash Idea 笔记导出.md")
                }
                ExportButton("TXT") {
                    shareText(context, ExportFormat.toTxt(allIdeas), "Flash Idea 笔记导出.txt")
                }
                ExportButton("JSON") {
                    shareText(context, ExportFormat.toJson(allIdeas), "Flash Idea 笔记导出.json")
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )
            )
        }
    ) { innerPadding ->
        QuietBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                QuietRow("模型", value = currentModelLabel(), icon = Icons.Default.Tune) {
                    showModelSheet = true
                }
                QuietDivider()
                ToggleRow(
                    icon = Icons.Default.Gesture,
                    title = "三指双击",
                    value = if (gestureEnabled) "已启用" else "系统设置",
                    checked = gestureEnabled,
                    onClick = { FlashGestureService.openSettings(context) }
                )
                QuietDivider()
                QuietRow("下拉磁贴", value = "记录入口", icon = Icons.Default.TouchApp)
                QuietDivider()
                QuietRow("系统分享", value = "文本入口", icon = Icons.Default.Share)
                QuietDivider()
                QuietRow("导出", value = "${allIdeas.size} 条", icon = Icons.Default.FileDownload) {
                    showExportSheet = true
                }
                QuietDivider()
                QuietRow("隐私", value = "端侧优先", icon = if (BuildConfig.AI_API_KEY.isBlank()) Icons.Default.CloudOff else Icons.Default.Lock)
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun RuntimeLine(label: String, status: ModelRuntimeStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            status.toUiText(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    value: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { onClick() })
    }
}

@Composable
private fun ExportButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

private fun shareText(context: android.content.Context, text: String, title: String) {
    if (text.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(Intent.createChooser(intent, "导出笔记"))
}

private fun ModelRuntimeStatus.toUiText(): String = when (this) {
    ModelRuntimeStatus.Ready -> "可用"
    ModelRuntimeStatus.Configurable -> "可配置"
    ModelRuntimeStatus.SdkMissing -> "未接入"
    ModelRuntimeStatus.CredentialMissing -> "未配置"
    ModelRuntimeStatus.Unavailable -> "不可用"
    ModelRuntimeStatus.Failed -> "异常"
}

private fun currentModelLabel(): String = when {
    BuildConfig.AI_API_KEY.isBlank() -> "本地"
    BuildConfig.AI_PROVIDER == "vivo" -> "蓝心云端"
    else -> "第三方"
}
