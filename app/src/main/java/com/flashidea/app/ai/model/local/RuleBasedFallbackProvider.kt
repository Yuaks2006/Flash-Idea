package com.flashidea.app.ai.model.local

import com.flashidea.app.ai.LocalChatResponder
import com.flashidea.app.ai.model.AiModelProvider
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse
import com.flashidea.app.ai.model.AiTaskType
import com.flashidea.app.ai.model.ModelProviderId
import com.flashidea.app.ai.model.ModelRuntimeStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleBasedFallbackProvider @Inject constructor(
    private val localChatResponder: LocalChatResponder
) : AiModelProvider {

    override val id: ModelProviderId = ModelProviderId.LocalRule
    override val displayName: String = "本地规则兜底"
    override val status: ModelRuntimeStatus = ModelRuntimeStatus.Ready

    override suspend fun generate(request: AiModelRequest): AiModelResponse {
        val userMessage = request.messages.lastOrNull { it.role == "user" }?.content.orEmpty()
        val content = when (request.taskType) {
            AiTaskType.ChatAssistant,
            AiTaskType.ActionPlanning,
            AiTaskType.LinkDiscovery,
            AiTaskType.ExportSummary -> {
                localChatResponder.reply(userMessage, request.contextNotes)
            }
            AiTaskType.NoteAnalysis -> buildLocalAnalysis(userMessage)
        }

        return AiModelResponse(
            content = content,
            providerId = id,
            providerName = displayName,
            status = status,
            success = content.isNotBlank(),
            isFallback = true
        )
    }

    private fun buildLocalAnalysis(content: String): String {
        val compact = content.trim().replace('\n', ' ')
        val summary = (if (compact.length > 15) compact.take(15) else compact)
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return """
{
  "category": "灵感",
  "summary": "$summary",
  "tags": ["本地分析"]
}
""".trimIndent()
    }
}
