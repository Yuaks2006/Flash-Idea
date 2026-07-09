package com.flashidea.app.ai.agent

import com.flashidea.app.ai.AiChatReply
import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse
import com.flashidea.app.ai.model.AiModelStreamChunk
import com.flashidea.app.ai.model.ModelRouter
import com.flashidea.app.ai.model.ModelRuntimeSnapshot
import com.flashidea.app.data.local.AgentRunEntity
import com.flashidea.app.data.local.InsightEntity
import com.flashidea.app.data.repository.IdeaRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent 流式事件。sealed interface 便于 UI 穷举 `when`。
 */
sealed interface AgentStreamEvent {
    /** 工具调用阶段提示。 */
    data class ToolPhase(val trace: String) : AgentStreamEvent

    /** 增量文本（流式生成阶段）。 */
    data class Delta(val text: String) : AgentStreamEvent

    /** 完整回复（流式结束或错误兜底）。 */
    data class Final(val reply: AiChatReply) : AgentStreamEvent
}

@Singleton
class AgentRuntime @Inject constructor(
    private val modelRouter: ModelRouter,
    private val toolRegistry: ToolRegistry,
    private val taskPlanner: AgentTaskPlanner,
    private val workflowEngine: AgentWorkflowEngine,
    private val repository: IdeaRepository
) {
    private val gson = Gson()

    suspend fun generate(request: AiModelRequest): AiModelResponse {
        return modelRouter.generate(request)
    }

    fun modelSnapshot(): ModelRuntimeSnapshot = modelRouter.snapshot()

    fun availableTools(): List<AgentTool> = toolRegistry.availableTools()

    suspend fun run(task: AgentTask): AgentResult {
        val startedAt = System.currentTimeMillis()
        val runtimePlan = taskPlanner.plan(task.goal)
        val workflow = workflowEngine.buildWorkflow(runtimePlan)
        var runRecord = AgentRunEntity(
            goal = task.goal,
            taskType = runtimePlan.taskType.name,
            status = "running",
            privacyMode = task.modelRequest.privacyMode.name,
            planJson = gson.toJson(workflow.nodes.map { "${it.phase.name}: ${it.description}" }),
            relatedIdeaIds = gson.toJson(task.modelRequest.contextNotes.map { it.id }),
            createdAt = startedAt
        )
        repository.saveAgentRun(runRecord)

        val toolContext = AgentToolContext(
            goal = task.goal,
            contextNotes = task.modelRequest.contextNotes
        )
        val toolResults = mutableListOf<AgentToolResult>()

        return runCatching {
            workflow.nodes.forEach { node ->
                node.toolCalls.forEach { call ->
                    if (toolResults.none { it.name == call.name }) {
                        toolResults += toolRegistry.execute(call, toolContext)
                    }
                }
            }

            val request = task.modelRequest.copy(
                taskType = runtimePlan.taskType,
                messages = task.modelRequest.messages + buildObservationMessage(toolResults)
            )
            val response = generate(request)
            val plan = AgentPlan(
                goal = task.goal,
                steps = workflow.nodes.map { node ->
                    AgentStep(
                        description = "工作流阶段：${node.phase.name}",
                        observation = node.description
                    )
                } + toolResults.map { result ->
                    AgentStep(
                        description = "工具观察：${result.name.id}",
                        observation = result.content
                    )
                }
            )
            val reflection = buildReflection(task.goal, response, toolResults)
            val relatedIds = (task.modelRequest.contextNotes.map { it.id } +
                toolResults.flatMap { it.relatedIdeaIds }).distinct()
            val completedAt = System.currentTimeMillis()
            runRecord = runRecord.copy(
                status = "completed",
                providerId = response.providerId.name,
                providerName = response.providerName,
                toolTraceJson = gson.toJson(toolResults.map { result ->
                    mapOf(
                        "tool" to result.name.id,
                        "success" to result.success,
                        "content" to result.content,
                        "relatedIdeaIds" to result.relatedIdeaIds
                    )
                }),
                relatedIdeaIds = gson.toJson(relatedIds),
                finalAnswer = response.content,
                reflection = reflection,
                completedAt = completedAt,
                latencyMs = completedAt - startedAt
            )
            repository.saveAgentRun(runRecord)
            if (reflection.isNotBlank()) {
                repository.saveInsight(
                    InsightEntity(
                        type = "agent_reflection",
                        content = reflection,
                        relatedIdeaIds = gson.toJson(relatedIds),
                        generatedBy = response.providerId.name
                    )
                )
            }
            AgentResult(
                plan = plan,
                finalAnswer = response.content,
                modelResponse = response
            )
        }.getOrElse { error ->
            repository.saveAgentRun(
                runRecord.copy(
                    status = "failed",
                    toolTraceJson = gson.toJson(toolResults),
                    errorMessage = error.message.orEmpty(),
                    completedAt = System.currentTimeMillis(),
                    latencyMs = System.currentTimeMillis() - startedAt
                )
            )
            throw error
        }
    }

    /**
     * 流式运行 Agent。工具阶段同步执行并 emit [AgentStreamEvent.ToolPhase]，
     * 最终生成阶段透传 [ModelRouter.stream] 的 Delta，结束时构造 [AiChatReply] emit Final。
     * 保留 [run] 非流式路径不破坏。
     */
    fun runStreaming(task: AgentTask): Flow<AgentStreamEvent> = flow {
        val runtimePlan = taskPlanner.plan(task.goal)
        val workflow = workflowEngine.buildWorkflow(runtimePlan)

        val toolContext = AgentToolContext(
            goal = task.goal,
            contextNotes = task.modelRequest.contextNotes
        )
        val toolResults = mutableListOf<AgentToolResult>()

        workflow.nodes.forEach { node ->
            node.toolCalls.forEach { call ->
                if (toolResults.none { it.name == call.name }) {
                    emit(AgentStreamEvent.ToolPhase("调用工具: ${call.name.id}"))
                    runCatching { toolRegistry.execute(call, toolContext) }
                        .onSuccess { toolResults += it }
                        .onFailure {
                            emit(AgentStreamEvent.ToolPhase("工具 ${call.name.id} 失败: ${it.message}"))
                        }
                }
            }
        }

        val request = task.modelRequest.copy(
            taskType = runtimePlan.taskType,
            messages = task.modelRequest.messages + buildObservationMessage(toolResults)
        )

        val accumulated = StringBuilder()
        var providerName = ""
        var isFallback = false

        modelRouter.stream(request).collect { chunk ->
            when (chunk) {
                is AiModelStreamChunk.Delta -> {
                    accumulated.append(chunk.text)
                    emit(AgentStreamEvent.Delta(chunk.text))
                }
                is AiModelStreamChunk.Final -> {
                    val finalContent = if (accumulated.isEmpty()) {
                        chunk.response.content
                    } else {
                        accumulated.toString()
                    }
                    providerName = chunk.response.providerName
                    isFallback = chunk.response.isFallback
                    emit(
                        AgentStreamEvent.Final(
                            AiChatReply(
                                content = finalContent.ifBlank { "（AI 未返回内容）" },
                                providerName = providerName,
                                isFallback = isFallback
                            )
                        )
                    )
                }
                is AiModelStreamChunk.Error -> {
                    emit(
                        AgentStreamEvent.Final(
                            AiChatReply(
                                content = "（生成失败：${chunk.throwable.message ?: "未知错误"}）",
                                providerName = providerName.ifBlank { "未知" },
                                isFallback = true
                            )
                        )
                    )
                }
                is AiModelStreamChunk.ToolCallTrace -> {
                    emit(AgentStreamEvent.ToolPhase(chunk.phase))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildObservationMessage(results: List<AgentToolResult>): AiMessage {
        val content = buildString {
            append("以下是 Flash Idea Agent 在本地工具层得到的观察结果，请据此回答用户：\n")
            results.forEach { result ->
                append("\n[tool:")
                append(result.name.id)
                append("]\n")
                append(result.content)
                append('\n')
            }
        }
        return AiMessage(role = "system", content = content)
    }

    private fun buildReflection(
        goal: String,
        response: AiModelResponse,
        toolResults: List<AgentToolResult>
    ): String {
        val tools = toolResults.joinToString("、") { it.name.id }
        return """
## Agent 运行反思

目标：$goal

使用模型：${response.providerName}${if (response.isFallback) "（兜底）" else ""}

调用工具：${tools.ifBlank { "无" }}

结果边界：本次回答基于用户授权上下文与 Flash Idea 本地记忆生成，可继续通过补充笔记、标记授权或切换模型提升结果质量。
""".trimIndent()
    }
}
