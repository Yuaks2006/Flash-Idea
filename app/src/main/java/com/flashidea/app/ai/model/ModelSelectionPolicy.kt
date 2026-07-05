package com.flashidea.app.ai.model

import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelSelectionPolicy @Inject constructor() {

    fun orderProviders(
        config: ModelProviderConfig,
        availableProviderIds: List<ModelProviderId>
    ): List<ModelProviderId> {
        val desiredOrder = when (config.providerType) {
            ModelProviderType.OnDeviceFirst -> listOf(
                ModelProviderId.VivoOnDevice,
                ModelProviderId.Cloud,
                ModelProviderId.LocalRule
            )
            ModelProviderType.VivoCloud -> listOf(
                ModelProviderId.Cloud,
                ModelProviderId.VivoOnDevice,
                ModelProviderId.LocalRule
            )
            ModelProviderType.CustomOpenAi -> if (config.customModel.isUsable()) {
                listOf(
                    ModelProviderId.CustomOpenAi,
                    ModelProviderId.VivoOnDevice,
                    ModelProviderId.LocalRule
                )
            } else {
                listOf(
                    ModelProviderId.VivoOnDevice,
                    ModelProviderId.Cloud,
                    ModelProviderId.LocalRule
                )
            }
            ModelProviderType.LocalRule -> listOf(ModelProviderId.LocalRule)
        }

        return desiredOrder.filter { it in availableProviderIds }.distinct()
    }
}
