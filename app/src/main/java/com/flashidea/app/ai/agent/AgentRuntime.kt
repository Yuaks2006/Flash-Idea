package com.flashidea.app.ai.agent

import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse
import com.flashidea.app.ai.model.ModelRouter
import com.flashidea.app.ai.model.ModelRuntimeSnapshot
import com.flashidea.app.data.local.AgentRunEntity
import com.flashidea.app.data.local.InsightEntity
import com.flashidea.app.data.repository.IdeaRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

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
