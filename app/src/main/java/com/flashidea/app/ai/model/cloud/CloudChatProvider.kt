package com.flashidea.app.ai.model.cloud

import com.flashidea.app.BuildConfig
import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelProvider
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse
import com.flashidea.app.ai.model.AiModelStreamChunk
import com.flashidea.app.ai.model.ModelProviderId
import com.flashidea.app.ai.model.ModelRuntimeStatus
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
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

        @Streaming
        @Headers("Content-Type: application/json")
        @POST("chat/completions")
        suspend fun stream(@Body req: ChatRequest): ResponseBody
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

    override fun supportsStreaming(): Boolean = true

    override fun stream(request: AiModelRequest): Flow<AiModelStreamChunk> = flow {
        if (BuildConfig.AI_API_KEY.isBlank()) {
            emit(AiModelStreamChunk.Error(IllegalStateException("云端 API Key 未配置")))
            return@flow
        }

        try {
            val body = api.stream(
                ChatRequest(
                    model = BuildConfig.AI_MODEL,
                    messages = request.messages,
                    stream = true,
                    temperature = request.temperature,
                    max_tokens = request.maxTokens
                )
            )
            val accumulated = StringBuilder()
            body.use { responseBody ->
                val source = responseBody.source()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                    if (!trimmed.startsWith("data:")) continue
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        return@use
                    }
                    val delta = parseDelta(data)
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(AiModelStreamChunk.Delta(delta))
                    }
                }
            }
            emit(
                AiModelStreamChunk.Final(
                    AiModelResponse(
                        content = accumulated.toString(),
                        providerId = id,
                        providerName = displayName,
                        status = ModelRuntimeStatus.Ready,
                        success = accumulated.isNotEmpty()
                    )
                )
            )
        } catch (t: Throwable) {
            emit(AiModelStreamChunk.Error(t))
        }
    }.flowOn(Dispatchers.IO)

    private fun parseDelta(json: String): String {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            obj.getAsJsonArray("choices")?.firstOrNull()
                ?.asJsonObject?.getAsJsonObject("delta")
                ?.get("content")?.takeIf { !it.isJsonNull }
                ?.asString ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
