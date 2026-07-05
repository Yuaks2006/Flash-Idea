package com.flashidea.app.ai.model.ondevice

import com.flashidea.app.ai.model.AiModelProvider
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiModelResponse
import com.flashidea.app.ai.model.ModelProviderId
import com.flashidea.app.ai.model.ModelRuntimeStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VivoOnDeviceProvider @Inject constructor(
    private val sdkBridge: VivoOnDeviceSdkBridge
) : AiModelProvider {

    override val id: ModelProviderId = ModelProviderId.VivoOnDevice
    override val displayName: String = "vivo 端侧大模型"
    override val status: ModelRuntimeStatus
        get() = if (sdkBridge.isAvailable()) ModelRuntimeStatus.Ready else ModelRuntimeStatus.SdkMissing

    override suspend fun generate(request: AiModelRequest): AiModelResponse {
        if (!sdkBridge.isAvailable()) {
            return AiModelResponse.unavailable(
                providerId = id,
                providerName = displayName,
                status = status,
                message = "端侧大模型 SDK 尚未接入"
            )
        }

        val content = sdkBridge.generate(request)
        return AiModelResponse(
            content = content,
            providerId = id,
            providerName = displayName,
            status = ModelRuntimeStatus.Ready,
            success = content.isNotBlank()
        )
    }
}
