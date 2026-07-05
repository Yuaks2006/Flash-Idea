package com.flashidea.app.ai.model.custom

import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelProvider
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse
import com.flashidea.app.ai.model.ModelProviderId
import com.flashidea.app.ai.model.ModelRuntimeStatus
import com.flashidea.app.ai.model.config.ModelPreferenceRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomOpenAiProvider @Inject constructor(
    private val preferences: ModelPreferenceRepository
) : AiModelProvider {

    private val okHttpClient = OkHttpClient.Builder().build()
    private val gson = Gson()

    private data class ChatRequest(
        val model: String,
        val messages: List<AiMessage>,
        val stream: Boolean = false,
        val temperature: Double,
        val max_tokens: Int
    )

    private data class ChatResponse(val choices: List<Choice> = emptyList())
    private data class Choice(val message: AiMessage? = null)

    override val id: ModelProviderId = ModelProviderId.CustomOpenAi
    override val displayName: String = "用户自定义模型"
    override val status: ModelRuntimeStatus = ModelRuntimeStatus.Configurable

    override suspend fun generate(request: AiModelRequest): AiModelResponse {
        val config = preferences.config.first().customModel
        if (!config.isUsable()) {
            return AiModelResponse.unavailable(
                providerId = id,
                providerName = displayName,
                status = ModelRuntimeStatus.CredentialMissing,
                message = "自定义模型 Base URL、API Key 或 Model Name 未配置"
            )
        }

        val bodyJson = gson.toJson(
            ChatRequest(
                model = config.modelName,
                messages = request.messages,
                temperature = request.temperature,
                max_tokens = request.maxTokens
            )
        )

        val httpRequest = Request.Builder()
            .url(config.normalizedBaseUrl() + "chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AiModelResponse.unavailable(
                        providerId = id,
                        providerName = displayName,
                        status = ModelRuntimeStatus.Failed,
                        message = "自定义模型请求失败：HTTP ${response.code}"
                    )
                }

                val content = gson.fromJson(
                    response.body?.string().orEmpty(),
                    ChatResponse::class.java
                ).choices.firstOrNull()?.message?.content.orEmpty()

                AiModelResponse(
                    content = content,
                    providerId = id,
                    providerName = displayName,
                    status = ModelRuntimeStatus.Ready,
                    success = content.isNotBlank()
                )
            }
        }
    }
}
