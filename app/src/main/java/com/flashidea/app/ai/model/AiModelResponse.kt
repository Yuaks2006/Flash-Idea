package com.flashidea.app.ai.model

data class AiModelResponse(
    val content: String,
    val providerId: ModelProviderId,
    val providerName: String,
    val status: ModelRuntimeStatus,
    val success: Boolean,
    val isFallback: Boolean = false,
    val message: String = ""
) {
    companion object {
        fun unavailable(
            providerId: ModelProviderId,
            providerName: String,
            status: ModelRuntimeStatus,
            message: String
        ): AiModelResponse = AiModelResponse(
            content = "",
            providerId = providerId,
            providerName = providerName,
            status = status,
            success = false,
            message = message
        )
    }
}
