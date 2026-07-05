package com.flashidea.app.ai.model.config

data class ModelProviderConfig(
    val providerType: ModelProviderType = ModelProviderType.OnDeviceFirst,
    val customModel: CustomModelConfig = CustomModelConfig()
) {
    fun strategyLabel(): String = when (providerType) {
        ModelProviderType.OnDeviceFirst -> "端侧优先，云端兜底，本地规则保底"
        ModelProviderType.VivoCloud -> "vivo 云端优先，端侧与本地兜底"
        ModelProviderType.CustomOpenAi -> "用户自定义模型优先，端侧与本地兜底"
        ModelProviderType.LocalRule -> "仅使用本地规则"
    }
}
