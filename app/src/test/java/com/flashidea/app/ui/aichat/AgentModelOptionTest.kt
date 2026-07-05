package com.flashidea.app.ui.aichat

import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentModelOptionTest {

    @Test
    fun chatModelOptionsUseRealProviderTypes() {
        assertEquals(
            listOf(
                ModelProviderType.OnDeviceFirst,
                ModelProviderType.VivoCloud,
                ModelProviderType.CustomOpenAi,
                ModelProviderType.LocalRule
            ),
            agentModelOptions().map { it.type }
        )
    }

    @Test
    fun selectedModelLabelReflectsSavedProviderConfig() {
        assertEquals(
            "云端",
            ModelProviderConfig(providerType = ModelProviderType.VivoCloud).agentModelShortLabel()
        )
        assertEquals(
            "自定义",
            ModelProviderConfig(providerType = ModelProviderType.CustomOpenAi).agentModelShortLabel()
        )
    }
}
