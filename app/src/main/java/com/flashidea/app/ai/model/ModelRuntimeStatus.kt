package com.flashidea.app.ai.model

enum class ModelProviderId {
    VivoOnDevice,
    Cloud,
    CustomOpenAi,
    LocalRule
}

enum class ModelRuntimeStatus {
    Ready,
    Configurable,
    SdkMissing,
    CredentialMissing,
    Unavailable,
    Failed
}

data class ModelRuntimeSnapshot(
    val onDeviceStatus: ModelRuntimeStatus,
    val cloudStatus: ModelRuntimeStatus,
    val customStatus: ModelRuntimeStatus = ModelRuntimeStatus.Configurable,
    val localStatus: ModelRuntimeStatus,
    val activeStrategy: String,
    val lastProviderName: String = ""
)
