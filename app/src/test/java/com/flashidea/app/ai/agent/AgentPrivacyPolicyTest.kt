package com.flashidea.app.ai.agent

import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiPrivacyMode
import com.flashidea.app.ai.model.AiTaskType
import com.flashidea.app.ai.model.ModelProviderId
import com.flashidea.app.data.local.IdeaEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPrivacyPolicyTest {

    private val policy = AgentPrivacyPolicy()

    @Test
    fun `always allows on device and local providers`() {
        val request = requestWithPrivateNote(AiPrivacyMode.LocalOnly)

        assertTrue(policy.canUse(ModelProviderId.VivoOnDevice, request))
        assertTrue(policy.canUse(ModelProviderId.LocalRule, request))
    }

    @Test
    fun `blocks cloud and custom providers for local only context`() {
        val request = requestWithPrivateNote(AiPrivacyMode.LocalOnly)

        assertFalse(policy.canUse(ModelProviderId.Cloud, request))
        assertFalse(policy.canUse(ModelProviderId.CustomOpenAi, request))
    }

    @Test
    fun `allows cloud providers when context was explicitly selected by user`() {
        val request = requestWithPrivateNote(AiPrivacyMode.UserSelectedContext)

        assertTrue(policy.canUse(ModelProviderId.Cloud, request))
        assertTrue(policy.canUse(ModelProviderId.CustomOpenAi, request))
    }

    private fun requestWithPrivateNote(mode: AiPrivacyMode) = AiModelRequest(
        taskType = AiTaskType.ChatAssistant,
        messages = listOf(AiMessage("user", "分析一下")),
        contextNotes = listOf(IdeaEntity(content = "尚未授权的私有想法")),
        privacyMode = mode
    )
}
