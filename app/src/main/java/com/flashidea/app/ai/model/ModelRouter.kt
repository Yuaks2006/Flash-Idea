package com.flashidea.app.ai.model

import com.flashidea.app.ai.agent.AgentPrivacyPolicy
import com.flashidea.app.ai.model.cloud.CloudChatProvider
import com.flashidea.app.ai.model.config.ModelPreferenceRepository
import com.flashidea.app.ai.model.custom.CustomOpenAiProvider
import com.flashidea.app.ai.model.local.RuleBasedFallbackProvider
import com.flashidea.app.ai.model.ondevice.VivoOnDeviceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRouter @Inject constructor(
    private val onDeviceProvider: VivoOnDeviceProvider,
    private val cloudProvider: CloudChatProvider,
    private val customProvider: CustomOpenAiProvider,
    private val localProvider: RuleBasedFallbackProvider,
    private val preferences: ModelPreferenceRepository,
    private val selectionPolicy: ModelSelectionPolicy,
    private val privacyPolicy: AgentPrivacyPolicy
) {
    suspend fun generate(request: AiModelRequest): AiModelResponse {
        val providerMap = listOf(onDeviceProvider, cloudProvider, customProvider, localProvider)
            .associateBy { it.id }
        val config = preferences.config.first()
        val providers = selectionPolicy
            .orderProviders(config, providerMap.keys.toList())
            .mapNotNull { providerMap[it] }
            .filter { it.supports(request.taskType) }
            .filter { privacyPolicy.canUse(it.id, request) }

        providers.forEachIndexed { index, provider ->
            val response = runCatching { provider.generate(request) }.getOrElse {
                AiModelResponse.unavailable(
                    providerId = provider.id,
                    providerName = provider.displayName,
                    status = ModelRuntimeStatus.Failed,
                    message = it.message.orEmpty()
                )
            }
            if (response.success) {
                return response.copy(isFallback = index > 0)
            }
        }

        return localProvider.generate(request).copy(isFallback = true)
    }

    fun stream(request: AiModelRequest): Flow<AiModelStreamChunk> = flow {
        val providerMap = listOf(onDeviceProvider, cloudProvider, customProvider, localProvider)
            .associateBy { it.id }
        val config = preferences.config.first()
        val providers = selectionPolicy
            .orderProviders(config, providerMap.keys.toList())
            .mapNotNull { providerMap[it] }
            .filter { it.supports(request.taskType) }
            .filter { privacyPolicy.canUse(it.id, request) }

        val streamingProvider = providers.firstOrNull { it.supportsStreaming() }

        if (streamingProvider != null) {
            var streamErrored = false
            streamingProvider.stream(request).collect { chunk ->
                if (chunk is AiModelStreamChunk.Error) {
                    streamErrored = true
                } else {
                    emit(chunk)
                }
            }
            if (!streamErrored) return@flow
        }

        runCatching { generate(request) }
            .onSuccess { emit(AiModelStreamChunk.Final(it)) }
            .onFailure { emit(AiModelStreamChunk.Error(it)) }
    }.flowOn(Dispatchers.IO)

    fun snapshot(): ModelRuntimeSnapshot = ModelRuntimeSnapshot(
        onDeviceStatus = onDeviceProvider.status,
        cloudStatus = cloudProvider.status,
        customStatus = customProvider.status,
        localStatus = localProvider.status,
        activeStrategy = "端侧优先，云端兜底，本地规则保底"
    )
}
