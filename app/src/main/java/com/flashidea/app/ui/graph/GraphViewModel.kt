package com.flashidea.app.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashidea.app.ai.IdeaProcessor
import com.flashidea.app.ai.InsightSuggestion
import com.flashidea.app.ai.RawPair
import com.flashidea.app.ai.memory.TagLinkBuilder
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.local.InsightEntity
import com.flashidea.app.data.local.LinkEntity
import com.flashidea.app.data.repository.IdeaRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Running : AnalysisState()
    data class Done(val linkCount: Int, val insightCount: Int) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val repository: IdeaRepository,
    private val ideaProcessor: IdeaProcessor
) : ViewModel() {

    val ideas: StateFlow<List<IdeaEntity>> = repository.getAllIdeas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val storedLinks: Flow<List<LinkEntity>> = repository.getAllLinks()

    val links: StateFlow<List<LinkEntity>> = combine(ideas, storedLinks) { allIdeas, dbLinks ->
        mergeLinks(dbLinks, TagLinkBuilder.buildLinks(allIdeas))
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasUnreadInsights: StateFlow<Boolean> = repository.hasUnreadInsights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    fun analyzeAllLinks() {
        if (_analysisState.value is AnalysisState.Running) return
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Running
            runCatching {
                val allIdeas = ideas.value
                val (pairs, insights) = ideaProcessor.analyzeAllLinks(allIdeas)
                val validIds = allIdeas.map { it.id }.toSet()
                val tagLinks = TagLinkBuilder.buildLinks(allIdeas)

                // 无向去重写入 LinkEntity
                val seen = mutableSetOf<String>()
                var linkCount = 0
                tagLinks
                    .filter { it.sourceId in validIds && it.targetId in validIds }
                    .forEach { link ->
                        val key = unorderedKey(link.sourceId, link.targetId)
                        if (seen.add(key)) {
                            repository.saveLink(link)
                            linkCount++
                        }
                    }
                pairs
                    .filter { it.a in validIds && it.b in validIds }
                    .forEach { pair ->
                        val key = unorderedKey(pair.a, pair.b)
                        if (seen.add(key)) {
                            repository.saveLink(
                                LinkEntity(
                                    sourceId = pair.a,
                                    targetId = pair.b,
                                    strength = pair.strength,
                                    createdBy = "local_ai"
                                )
                            )
                            linkCount++
                        }
                    }

                // 写入 InsightEntity
                var insightCount = 0
                insights.forEach { suggestion ->
                    repository.saveInsight(
                        InsightEntity(
                            type = "hidden_link",
                            content = suggestion.content,
                            relatedIdeaIds = Gson().toJson(suggestion.relatedIds),
                            generatedBy = "local_ai",
                            isRead = false
                        )
                    )
                    insightCount++
                }

                Pair(linkCount, insightCount)
            }.onSuccess { (links, insights) ->
                _analysisState.value = AnalysisState.Done(linkCount = links, insightCount = insights)
            }.onFailure { e ->
                _analysisState.value = AnalysisState.Error(e.message ?: "分析失败")
            }
        }
    }

    fun resetAnalysisState() {
        _analysisState.value = AnalysisState.Idle
    }

    private fun mergeLinks(
        dbLinks: List<LinkEntity>,
        tagLinks: List<LinkEntity>
    ): List<LinkEntity> {
        val merged = linkedMapOf<String, LinkEntity>()
        tagLinks.forEach { merged[unorderedKey(it.sourceId, it.targetId)] = it }
        dbLinks.forEach { link ->
            val key = unorderedKey(link.sourceId, link.targetId)
            val current = merged[key]
            merged[key] = if (current == null || link.strength >= current.strength) link else current
        }
        return merged.values.toList()
    }

    private fun unorderedKey(a: String, b: String): String = "${minOf(a, b)}_${maxOf(a, b)}"
}
