package com.flashidea.app.ai.model.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.modelProviderDataStore by preferencesDataStore(name = "model_provider_config")

@Singleton
class ModelPreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val providerType = stringPreferencesKey("provider_type")
        val customBaseUrl = stringPreferencesKey("custom_base_url")
        val customApiKey = stringPreferencesKey("custom_api_key")
        val customModelName = stringPreferencesKey("custom_model_name")
    }

    val config: Flow<ModelProviderConfig> = context.modelProviderDataStore.data.map { prefs ->
        ModelProviderConfig(
            providerType = runCatching {
                ModelProviderType.valueOf(
                    prefs[Keys.providerType] ?: ModelProviderType.OnDeviceFirst.name
                )
            }.getOrDefault(ModelProviderType.OnDeviceFirst),
            customModel = CustomModelConfig(
                baseUrl = prefs[Keys.customBaseUrl].orEmpty(),
                apiKey = prefs[Keys.customApiKey].orEmpty(),
                modelName = prefs[Keys.customModelName].orEmpty()
            )
        )
    }

    suspend fun saveProviderType(type: ModelProviderType) {
        context.modelProviderDataStore.edit { prefs ->
            prefs[Keys.providerType] = type.name
        }
    }

    suspend fun saveCustomModel(config: CustomModelConfig) {
        context.modelProviderDataStore.edit { prefs ->
            prefs[Keys.customBaseUrl] = config.baseUrl.trim()
            prefs[Keys.customApiKey] = config.apiKey.trim()
            prefs[Keys.customModelName] = config.modelName.trim()
        }
    }
}
