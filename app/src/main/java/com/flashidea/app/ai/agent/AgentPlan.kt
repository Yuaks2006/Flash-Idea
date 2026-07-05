package com.flashidea.app.ai.agent

import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse

data class AgentTask(
    val goal: String,
    val modelRequest: AiModelRequest
)

data class AgentPlan(
    val goal: String,
    val steps: List<AgentStep>
)

data class AgentStep(
    val description: String,
    val observation: String = ""
)

data class AgentResult(
    val plan: AgentPlan,
    val finalAnswer: String,
    val modelResponse: AiModelResponse
)
