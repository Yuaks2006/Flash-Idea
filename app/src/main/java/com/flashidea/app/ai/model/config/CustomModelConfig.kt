package com.flashidea.app.ai.model.config

data class CustomModelConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val modelName: String = ""
) {
    fun isUsable(): Boolean =
        baseUrl.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()

    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/') + "/"
}
