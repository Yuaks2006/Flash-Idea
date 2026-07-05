package com.flashidea.app.ai.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * AI 模型 Provider 统一接口。
 *
 * - [generate] 为同步（非流式）入口，所有实现必须支持。
 * - [supportsStreaming] / [stream] 为流式扩展，默认 fallback 走 [generate]，
 *   旧 Provider（[com.flashidea.app.ai.model.local.RuleBasedFallbackProvider] /
 *   [com.flashidea.app.ai.model.ondevice.VivoOnDeviceProvider]）零改动即可兼容。
 */
interface AiModelProvider {
    val id: ModelProviderId
    val displayName: String
    val status: ModelRuntimeStatus

    fun supports(taskType: AiTaskType): Boolean = true

    suspend fun generate(request: AiModelRequest): AiModelResponse

    /** 是否支持流式输出。默认 false，由具体 Provider 重写。 */
    fun supportsStreaming(): Boolean = false

    /**
     * 流式输出。默认实现走 [generate] 并包成单个 [AiModelStreamChunk.Final]，
     * 保证不支持流式的 Provider 也能被流式调用复用。
     */
    fun stream(request: AiModelRequest): Flow<AiModelStreamChunk> = flow {
        runCatching { generate(request) }
            .onSuccess { emit(AiModelStreamChunk.Final(it)) }
            .onFailure { emit(AiModelStreamChunk.Error(it)) }
    }
}

/**
 * 流式输出的事件块。sealed interface 限制可类型集合，便于 `when` 穷举。
 */
sealed interface AiModelStreamChunk {
    /** 增量文本（流式生成阶段）。 */
    data class Delta(val text: String) : AiModelStreamChunk

    /** 工具调用阶段标识（非流式，整段返回）。 */
    data class ToolCallTrace(val phase: String) : AiModelStreamChunk

    /** 完整结果（流式结束或非流式 fallback）。 */
    data class Final(val response: AiModelResponse) : AiModelStreamChunk

    /** 错误。 */
    data class Error(val throwable: Throwable) : AiModelStreamChunk
}
