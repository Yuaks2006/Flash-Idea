package com.flashidea.app.ai.model

interface AiModelProvider {
    val id: ModelProviderId
    val displayName: String
    val status: ModelRuntimeStatus

    fun supports(taskType: AiTaskType): Boolean = true

    suspend fun generate(request: AiModelRequest): AiModelResponse
}
