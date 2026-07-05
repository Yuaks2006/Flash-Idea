package com.flashidea.app.ui.aichat

import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType

data class AgentModelOption(
    val type: ModelProviderType,
    val shortLabel: String,
    val status: String,
    val caption: String
)

fun agentModelOptions(): List<AgentModelOption> = listOf(
    AgentModelOption(
        type = ModelProviderType.OnDeviceFirst,
        shortLabel = "端侧",
        status = "端侧优先",
        caption = "私密笔记优先留在设备内处理"
    ),
    AgentModelOption(
        type = ModelProviderType.VivoCloud,
        shortLabel = "云端",
        status = "蓝心云端",
        caption = "适合更复杂的推理与生成"
    ),
    AgentModelOption(
        type = ModelProviderType.CustomOpenAi,
        shortLabel = "自定义",
        status = "OpenAI 兼容",
        caption = "使用你配置的 Base URL 和 Key"
    ),
    AgentModelOption(
        type = ModelProviderType.LocalRule,
        shortLabel = "本地",
        status = "规则兜底",
        caption = "断网演示与基础整理可用"
    )
)

fun ModelProviderConfig.agentModelShortLabel(): String =
    providerType.agentModelOption().shortLabel

fun ModelProviderType.agentModelOption(): AgentModelOption =
    agentModelOptions().first { it.type == this }
