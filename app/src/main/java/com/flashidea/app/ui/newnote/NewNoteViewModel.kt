package com.flashidea.app.ui.newnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.ai.IdeaProcessor
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.local.InsightEntity
import com.flashidea.app.data.local.LinkEntity
import com.flashidea.app.data.repository.IdeaRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiProcessResult(
    val category: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList()
)

@HiltViewModel
class NewNoteViewModel @Inject constructor(
    private val repository: IdeaRepository,
    private val ideaProcessor: IdeaProcessor
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _aiResult = MutableStateFlow<AiProcessResult?>(null)
    val aiResult = _aiResult.asStateFlow()

    private val _savedNoteId = MutableStateFlow<String?>(null)
    val savedNoteId = _savedNoteId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun onContentChange(text: String) { _content.value = text }

    fun applyInitialContent(text: String) {
        if (_content.value.isBlank() && text.isNotBlank()) {
            _content.value = text.trim()
        }
    }

    fun save(onDone: () -> Unit = {}) {
        val text = _content.value.trim()
        if (text.isBlank()) {
            _message.value = "先写下一点内容"
            return
        }
        viewModelScope.launch {
            persistDraft(text)
            onDone()
        }
    }

    fun sendToAi() {
        val text = _content.value.trim()
        if (text.isBlank()) {
            _message.value = "先写下一点内容"
            return
        }
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            val idea = persistDraft(text)
            try {
                // 取候选列表（最多 30 条，按 updatedAt 倒序，排除自己）
                val allIdeas = repository.getAllIdeas().first()
                val candidates = allIdeas
                    .filter { it.id != idea.id }
                    .sortedByDescending { it.updatedAt }
                    .take(30)

                val result = ideaProcessor.processWithLinks(idea, candidates)

                // 更新笔记的 category / summary / tags
                val tagsJson = Gson().toJson(result.tags)
                repository.updateIdea(
                    idea.copy(
                        category = result.category,
                        summary = result.summary,
                        tags = tagsJson
                    )
                )

                // 写 LinkEntity（无向去重：用 minOf/maxOf 统一对顺序）
                val validIds = allIdeas.map { it.id }.toSet()
                val seen = mutableSetOf<String>()
                result.links
                    .filter { it.targetId in validIds }
                    .forEach { suggestion ->
                        val pairKey = "${minOf(idea.id, suggestion.targetId)}_${maxOf(idea.id, suggestion.targetId)}"
                        if (seen.add(pairKey)) {
                            repository.saveLink(
                                LinkEntity(
                                    sourceId = idea.id,
                                    targetId = suggestion.targetId,
                                    strength = suggestion.strength,
                                    createdBy = "local_ai"
                                )
                            )
                        }
                    }

                // 写 InsightEntity（仅当存在强关联时）
                val hasStrongLink = result.links.any { it.strength >= 0.7f }
                if (hasStrongLink && result.insight != null) {
                    repository.saveInsight(
                        InsightEntity(
                            type = "hidden_link",
                            content = result.insight.content,
                            relatedIdeaIds = Gson().toJson(result.insight.relatedIds),
                            generatedBy = "local_ai",
                            isRead = false
                        )
                    )
                }

                _aiResult.value = AiProcessResult(
                    category = result.category,
                    summary = result.summary,
                    tags = result.tags
                )
            } catch (e: Exception) {
                _aiResult.value = AiProcessResult(summary = "AI 处理失败，笔记已保存")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun dismissAiResult() { _aiResult.value = null }

    fun consumeMessage() {
        _message.value = null
    }

    private suspend fun persistDraft(text: String): IdeaEntity {
        val existing = _savedNoteId.value?.let { repository.getIdeaById(it) }
        return if (existing == null) {
            IdeaEntity(content = text).also {
                repository.saveIdea(it)
                _savedNoteId.value = it.id
            }
        } else {
            val updated = existing.copy(
                content = text,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateIdea(updated)
            updated
        }
    }
}
