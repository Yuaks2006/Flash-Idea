package com.flashidea.app.ui.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.ai.AiChatService
import com.flashidea.app.ai.agent.AgentStreamEvent
import com.flashidea.app.ai.model.config.CustomModelConfig
import com.flashidea.app.ai.model.config.ModelPreferenceRepository
import com.flashidea.app.ai.model.config.ModelProviderConfig
import com.flashidea.app.ai.model.config.ModelProviderType
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.repository.IdeaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private val _aiPhase = MutableStateFlow<String?>(null)
    val aiPhase: StateFlow<String?> = _aiPhase.asStateFlow()

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

    private var chatJob: Job? = null

    fun onInputChange(text: String) { _input.value = text }

    fun openNotePicker() { _showNotePicker.value = true }
    fun closeNotePicker() { _showNotePicker.value = false }

    // 选中笔记 id 集合 — 多选语义；UI 通过 selectedCount 显示数量
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    val selectedCount: StateFlow<Int> = _selectedNoteIds
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 列表点击只 select 不 toggle 取消，已选中的保留选中
    fun toggleNote(idea: IdeaEntity) {
        selectNote(idea)
    }

    private fun selectNote(idea: IdeaEntity) {
        if (_contextNotes.value.any { it.id == idea.id }) return
        _contextNotes.value = _contextNotes.value + idea
        _selectedNoteIds.value = _selectedNoteIds.value + idea.id
    }

    fun deselectNote(id: String) = removeSelectedNote(id)

    fun removeSelectedNote(id: String) {
        _contextNotes.value = _contextNotes.value.filter { it.id != id }
        _selectedNoteIds.value = _selectedNoteIds.value - id
    }

    fun isNoteSelected(ideaId: String) = _contextNotes.value.any { it.id == ideaId }

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
        chatJob?.cancel()
        val history = _messages.value.map { it.role to it.content }
        val notes = _contextNotes.value
        _messages.value += ChatMessage("user", text)
        _messages.value += ChatMessage("assistant", "")
        _input.value = ""
        _isLoading.value = true
        _aiPhase.value = "思考中..."
        chatJob = viewModelScope.launch {
            try {
                aiChatService.streamChat(text, history, notes).collect { event ->
                    when (event) {
                        is AgentStreamEvent.ToolPhase -> {
                            _aiPhase.value = event.trace
                        }
                        is AgentStreamEvent.Delta -> {
                            _aiPhase.value = null
                            val current = _messages.value.toMutableList()
                            val lastIdx = current.lastIndex
                            if (lastIdx >= 0 && current[lastIdx].role == "assistant") {
                                current[lastIdx] = current[lastIdx].copy(
                                    content = current[lastIdx].content + event.text
                                )
                                _messages.value = current
                            }
                        }
                        is AgentStreamEvent.Final -> {
                            _aiPhase.value = null
                            _activeProvider.value = if (event.reply.isFallback) {
                                "${event.reply.providerName}（兜底）"
                            } else {
                                event.reply.providerName
                            }
                            if (event.reply.content.isNotBlank()) {
                                val current = _messages.value.toMutableList()
                                val lastIdx = current.lastIndex
                                if (lastIdx >= 0 && current[lastIdx].role == "assistant") {
                                    current[lastIdx] = current[lastIdx].copy(content = event.reply.content)
                                    _messages.value = current
                                }
                            }
                        }
                    }
                }
            } finally {
                _isLoading.value = false
                _aiPhase.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatJob?.cancel()
    }
}
