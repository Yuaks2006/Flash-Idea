package com.flashidea.app.ai.model.ondevice

import com.flashidea.app.ai.model.AiModelRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VivoOnDeviceSdkBridge @Inject constructor() {

    fun isAvailable(): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    suspend fun generate(request: AiModelRequest): String {
        // TODO: 接入 vivo VCAP/蓝心端侧大模型 SDK 后，在这里完成：
        // 1. 初始化 SDK 与模型会话
        // 2. 将 AiModelRequest.messages 转换为 SDK 所需 prompt/messages
        // 3. 执行端侧推理
        // 4. 返回生成文本或 JSON 字符串
        return ""
    }
}
