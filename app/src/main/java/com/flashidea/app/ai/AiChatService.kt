package com.flashidea.app.ai

import com.flashidea.app.ai.agent.AgentRuntime
import com.flashidea.app.ai.agent.AgentStreamEvent
import com.flashidea.app.ai.agent.AgentTask
import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiPrivacyMode
import com.flashidea.app.ai.model.AiTaskType
import com.flashidea.app.data.local.IdeaEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiChatService @Inject constructor(
    private val agentRuntime: AgentRuntime
) {
    suspend fun chat(
        userMessage: String,
        history: List<Pair<String, String>>,
        contextNotes: List<IdeaEntity> = emptyList()
    ): AiChatReply {
        val systemContent = buildSystemContent(contextNotes)
        val messages = mutableListOf(AiMessage("system", systemContent))
        history.forEach { (role, content) -> messages += AiMessage(role, content) }
        messages += AiMessage("user", userMessage)

        val result = agentRuntime.run(
            AgentTask(
                goal = userMessage,
                modelRequest = AiModelRequest(
                    taskType = AiTaskType.ChatAssistant,
                    messages = messages,
                    contextNotes = contextNotes,
                    temperature = 0.7,
                    maxTokens = 1024,
                    privacyMode = if (contextNotes.isEmpty()) {
                        AiPrivacyMode.NoSensitiveContext
                    } else {
                        AiPrivacyMode.UserSelectedContext
                    }
                )
            )
        )
        return AiChatReply(
            content = result.finalAnswer.ifBlank { "（AI 未返回内容）" },
            providerName = result.modelResponse.providerName,
            isFallback = result.modelResponse.isFallback
        )
    }

    fun streamChat(
        userMessage: String,
        history: List<Pair<String, String>>,
        contextNotes: List<IdeaEntity> = emptyList()
    ): Flow<AgentStreamEvent> {
        val systemContent = buildSystemContent(contextNotes)
        val messages = mutableListOf(AiMessage("system", systemContent))
        history.forEach { (role, content) -> messages += AiMessage(role, content) }
        messages += AiMessage("user", userMessage)

        return agentRuntime.runStreaming(
            AgentTask(
                goal = userMessage,
                modelRequest = AiModelRequest(
                    taskType = AiTaskType.ChatAssistant,
                    messages = messages,
                    contextNotes = contextNotes,
                    temperature = 0.7,
                    maxTokens = 1024,
                    privacyMode = if (contextNotes.isEmpty()) {
                        AiPrivacyMode.NoSensitiveContext
                    } else {
                        AiPrivacyMode.UserSelectedContext
                    }
                )
            )
        )
    }

    private fun buildSystemContent(contextNotes: List<IdeaEntity>): String = buildString {
        append("你是 Flash Idea 助手，帮用户深化和连接他们的灵感笔记。用中文回答，简洁有深度。")
        if (contextNotes.isNotEmpty()) {
            append("\n\n用户引用的笔记上下文：\n")
            contextNotes.forEachIndexed { i, note ->
                append("【笔记${i + 1}】${note.content.take(300)}\n")
            }
        }
    }
}

data class AiChatReply(
    val content: String,
    val providerName: String,
    val isFallback: Boolean
)
