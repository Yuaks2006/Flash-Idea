package com.flashidea.app.ai.model

import com.flashidea.app.data.local.IdeaEntity

data class AiModelRequest(
    val taskType: AiTaskType,
    val messages: List<AiMessage>,
    val contextNotes: List<IdeaEntity> = emptyList(),
    val temperature: Double = 0.2,
    val maxTokens: Int = 2048,
    val expectJson: Boolean = false,
    val privacyMode: AiPrivacyMode = AiPrivacyMode.LocalOnly
)

data class AiMessage(
    val role: String,
    val content: String
)

enum class AiTaskType {
    NoteAnalysis,
    LinkDiscovery,
    ChatAssistant,
    ActionPlanning,
    ExportSummary
}

enum class AiPrivacyMode {
    NoSensitiveContext,
    LocalOnly,
    UserSelectedContext,
    CloudAuthorizedContext
}
