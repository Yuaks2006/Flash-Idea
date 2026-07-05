package com.flashidea.app.ai.agent

import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiPrivacyMode
import com.flashidea.app.ai.model.ModelProviderId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentPrivacyPolicy @Inject constructor() {

    fun canUse(providerId: ModelProviderId, request: AiModelRequest): Boolean {
        if (providerId == ModelProviderId.VivoOnDevice || providerId == ModelProviderId.LocalRule) {
            return true
        }

        return when (request.privacyMode) {
            AiPrivacyMode.NoSensitiveContext,
            AiPrivacyMode.UserSelectedContext,
            AiPrivacyMode.CloudAuthorizedContext -> true
            AiPrivacyMode.LocalOnly -> request.contextNotes.all { it.isCloudAuthorized }
        }
    }
}
