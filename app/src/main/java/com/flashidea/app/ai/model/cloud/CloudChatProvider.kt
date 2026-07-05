package com.flashidea.app.ai.model.cloud

import com.flashidea.app.BuildConfig
import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelProvider
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse
import com.flashidea.app.ai.model.ModelProviderId
import com.flashidea.app.ai.model.ModelRuntimeStatus
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudChatProvider @Inject constructor(
    retrofit: Retrofit
) : AiModelProvider {

    private interface Api {
        @Headers("Content-Type: application/json")
        @POST("chat/completions")
        suspend fun complete(@Body req: ChatRequest): ChatResponse
    }

    private data class ChatRequest(
        val model: String,
        val messages: List<AiMessage>,
        val stream: Boolean = false,
        val temperature: Double,
        val max_tokens: Int,
        val reasoning_effort: String = "minimal"
    )

    private data class ChatResponse(val choices: List<Choice>)
    private data class Choice(val message: AiMessage)

    override val id: ModelProviderId = ModelProviderId.Cloud
    override val displayName: String = "云端大模型"
    override val status: ModelRuntimeStatus
        get() = if (BuildConfig.AI_API_KEY.isBlank()) {
            ModelRuntimeStatus.CredentialMissing
        } else {
            ModelRuntimeStatus.Ready
        }

    private val api = retrofit.create(Api::class.java)

    override suspend fun generate(request: AiModelRequest): AiModelResponse {
        if (BuildConfig.AI_API_KEY.isBlank()) {
            return AiModelResponse.unavailable(
                providerId = id,
                providerName = displayName,
                status = status,
                message = "云端 API Key 未配置"
            )
        }

        val response = api.complete(
            ChatRequest(
                model = BuildConfig.AI_MODEL,
                messages = request.messages,
                temperature = request.temperature,
                max_tokens = request.maxTokens
            )
        )

        val content = response.choices.firstOrNull()?.message?.content.orEmpty()
        return AiModelResponse(
            content = content,
            providerId = id,
            providerName = displayName,
            status = ModelRuntimeStatus.Ready,
            success = content.isNotBlank()
        )
    }
}
