package com.flashidea.app.ai.model

import com.flashidea.app.ai.model.config.CustomModelConfig
import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelSelectionPolicyTest {

    private val policy = ModelSelectionPolicy()
    private val providers = listOf(
        ModelProviderId.VivoOnDevice,
        ModelProviderId.Cloud,
        ModelProviderId.CustomOpenAi,
        ModelProviderId.LocalRule
    )

    @Test
    fun `default strategy starts with on device then cloud then local fallback`() {
        val order = policy.orderProviders(ModelProviderConfig(), providers)

        assertEquals(
            listOf(ModelProviderId.VivoOnDevice, ModelProviderId.Cloud, ModelProviderId.LocalRule),
            order
        )
    }

    @Test
    fun `custom strategy starts with custom provider when config is usable`() {
        val order = policy.orderProviders(
            ModelProviderConfig(
                providerType = ModelProviderType.CustomOpenAi,
                customModel = CustomModelConfig(
                    baseUrl = "https://example.com/v1/",
                    apiKey = "sk-test",
                    modelName = "test-model"
                )
            ),
            providers
        )

        assertEquals(
            listOf(ModelProviderId.CustomOpenAi, ModelProviderId.VivoOnDevice, ModelProviderId.LocalRule),
            order
        )
    }

    @Test
    fun `local only strategy uses only local fallback`() {
        val order = policy.orderProviders(
            ModelProviderConfig(providerType = ModelProviderType.LocalRule),
            providers
        )

        assertEquals(listOf(ModelProviderId.LocalRule), order)
    }
}
