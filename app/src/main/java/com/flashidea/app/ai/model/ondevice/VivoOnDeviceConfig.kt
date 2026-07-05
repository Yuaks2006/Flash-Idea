package com.flashidea.app.ai.model.ondevice

data class VivoOnDeviceConfig(
    val preferOnDevice: Boolean = true,
    val modelName: String = "vivo BlueLM on-device",
    val sdkEnabled: Boolean = false
)
