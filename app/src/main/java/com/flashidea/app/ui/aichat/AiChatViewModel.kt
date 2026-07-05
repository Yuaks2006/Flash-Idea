package com.flashidea.app.ui.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.ai.AiChatService
import com.flashidea.app.ai.model.config.CustomModelConfig
import com.flashidea.app.ai.model.config.ModelPreferenceRepository
import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.repository.IdeaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val role: String, val content: String)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val repository: IdeaRepository,
    private val aiChatService: AiChatService,
    private val modelPreferences: ModelPreferenceRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input = _input.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _activeProvider = MutableStateFlow("端侧优先")
    val activeProvider = _activeProvider.asStateFlow()

    val modelConfig: StateFlow<ModelProviderConfig> = modelPreferences.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelProviderConfig())

    val lastRunSummary: StateFlow<String> = repository.getRecentAgentRuns(1)
        .map { runs ->
            runs.firstOrNull()?.let { run ->
                val provider = run.providerName.ifBlank { "等待模型" }
                val latency = if (run.latencyMs > 0) " · ${run.latencyMs}ms" else ""
                "${run.status} · $provider$latency"
            } ?: "尚无 Agent 运行记录"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "尚无 Agent 运行记录")

    private val _contextNotes = MutableStateFlow<List<IdeaEntity>>(emptyList())
    val contextNotes = _contextNotes.asStateFlow()

    private val _showNotePicker = MutableStateFlow(false)
    val showNotePicker = _showNotePicker.asStateFlow()

    val allIdeas: StateFlow<List<IdeaEntity>> = repository.getAllIdeas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 保留 contextChips 供 Screen 显示标签
    val contextChips: StateFlow<List<String>> = _contextNotes
        .map { notes ->
            notes.map { note ->
                val preview = note.content.trim().replace('\n', ' ')
                if (preview.length > 18) preview.take(18) + "…" else preview
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onInputChange(text: String) { _input.value = text }

    fun openNotePicker() { _showNotePicker.value = true }
    fun closeNotePicker() { _showNotePicker.value = false }

    fun toggleNote(idea: IdeaEntity) {
        val current = _contextNotes.value
        _contextNotes.value = if (current.any { it.id == idea.id }) {
            current.filter { it.id != idea.id }
        } else {
            current + idea
        }
    }

    fun isNoteSelected(ideaId: String) = _contextNotes.value.any { it.id == ideaId }

    fun removeContextNote(ideaId: String) {
        _contextNotes.value = _contextNotes.value.filter { it.id != ideaId }
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

    fun send() {
        val text = _input.value.trim().ifEmpty { return }
        sendPrompt(text)
    }

    fun sendPrompt(prompt: String) {
        val text = prompt.trim().ifEmpty { return }
        if (_isLoading.value) return
        val history = _messages.value.map { it.role to it.content }
        val notes = _contextNotes.value
        _messages.value += ChatMessage("user", text)
        _input.value = ""
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val reply = aiChatService.chat(text, history, notes)
                _activeProvider.value = if (reply.isFallback) {
                    "${reply.providerName}（兜底）"
                } else {
                    reply.providerName
                }
                _messages.value += ChatMessage("assistant", reply.content)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
