package com.flashidea.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.ai.agent.AgentRuntime
import com.flashidea.app.ai.model.ModelRuntimeSnapshot
import com.flashidea.app.ai.model.config.CustomModelConfig
import com.flashidea.app.ai.model.config.ModelPreferenceRepository
import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.prefs.AppPreferences
import com.flashidea.app.data.repository.IdeaRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repository: IdeaRepository,
    agentRuntime: AgentRuntime,
    private val modelPreferences: ModelPreferenceRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    val allIdeas: StateFlow<List<IdeaEntity>> = repository.getAllIdeas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modelConfig: StateFlow<ModelProviderConfig> = modelPreferences.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelProviderConfig())

    val modelSnapshot: ModelRuntimeSnapshot = agentRuntime.modelSnapshot()

    val autoIncubate: StateFlow<Boolean> = appPreferences.autoIncubate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_AUTO_INCUBATE)

    fun setAutoIncubate(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAutoIncubate(enabled) }
    }

    fun selectProvider(type: ModelProviderType) {
        viewModelScope.launch {
            modelPreferences.saveProviderType(type)
        }
    }

    fun saveCustomModel(baseUrl: String, apiKey: String, modelName: String) {
        viewModelScope.launch {
            modelPreferences.saveCustomModel(
                CustomModelConfig(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    modelName = modelName
                )
            )
            modelPreferences.saveProviderType(ModelProviderType.CustomOpenAi)
        }
    }
}

internal object ExportFormat {
    private fun dateFmt() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun toMarkdown(ideas: List<IdeaEntity>): String = buildString {
        ideas.forEach { note ->
            append("## ${dateFmt().format(Date(note.createdAt))}")
            if (note.category.isNotEmpty()) append(" · ${note.category}")
            append("\n\n")
            append(note.content)
            if (note.summary.isNotEmpty()) append("\n\n> ${note.summary}")
            parseTags(note.tags).takeIf { it.isNotEmpty() }?.let { tags ->
                append("\n\n")
                append(tags.joinToString(" ") { "#$it" })
            }
            append("\n\n---\n\n")
        }
    }.trimEnd()

    fun toTxt(ideas: List<IdeaEntity>): String = buildString {
        ideas.forEach { note ->
            append("[${dateFmt().format(Date(note.createdAt))}]\n")
            append(note.content)
            append("\n\n")
        }
    }.trimEnd()

    fun toJson(ideas: List<IdeaEntity>): String = Gson().toJson(ideas.map { note ->
        mapOf(
            "id" to note.id,
            "content" to note.content,
            "category" to note.category,
            "summary" to note.summary,
            "tags" to parseTags(note.tags),
            "createdAt" to note.createdAt,
            "createdAtText" to dateFmt().format(Date(note.createdAt))
        )
    })

    private fun parseTags(json: String): List<String> =
        runCatching {
            Gson().fromJson<List<String>>(
                json,
                object : TypeToken<List<String>>() {}.type
            )
        }.getOrDefault(emptyList())
}
