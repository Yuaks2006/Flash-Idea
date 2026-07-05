package com.flashidea.app.ai

import com.flashidea.app.ai.agent.AgentRuntime
import com.flashidea.app.ai.model.AiMessage
import com.flashidea.app.ai.model.AiModelRequest
import com.flashidea.app.ai.model.AiTaskType
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.ui.newnote.AiProcessResult
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

// ── 内部解析模型 ──────────────────────────────────────────────────────────────

private data class BasicAiOutput(
    val category: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList()
)

private data class RawLinkItem(val id: String = "", val strength: Float = 0.5f, val reason: String = "")
private data class RawInsightItem(val type: String = "hidden_link", val content: String = "", val relatedIds: List<String> = emptyList())

private data class IncrementalAiOutput(
    val category: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val links: List<RawLinkItem> = emptyList(),
    val insight: RawInsightItem? = null
)

private data class FullLinkAiOutput(
    val pairs: List<RawPairItem> = emptyList(),
    val insights: List<RawInsightItem> = emptyList()
)

private data class RawPairItem(
    val a: String = "",
    val b: String = "",
    val strength: Float = 0.5f,
    val reason: String = ""
)

// ── 公开返回类型 ──────────────────────────────────────────────────────────────

data class LinkSuggestion(val targetId: String, val strength: Float, val reason: String)
data class InsightSuggestion(val content: String, val relatedIds: List<String>)

data class FullAiResult(
    val category: String,
    val summary: String,
    val tags: List<String>,
    val links: List<LinkSuggestion>,
    val insight: InsightSuggestion?
)

data class RawPair(val a: String, val b: String, val strength: Float, val reason: String)

// ── IdeaProcessor ─────────────────────────────────────────────────────────────

@Singleton
class IdeaProcessor @Inject constructor(
    private val agentRuntime: AgentRuntime,
    private val localAnalyzer: LocalIdeaAnalyzer
) {

    private val gson = Gson()

    // 保留原接口供向后兼容（NewNoteViewModel 旧分支可能还用到）
    suspend fun process(idea: IdeaEntity): AiProcessResult {
        val raw = agentRuntime.generate(
            AiModelRequest(
                taskType = AiTaskType.NoteAnalysis,
                messages = listOf(
                    AiMessage("system", PromptBuilder.ideaAnalysisSystem),
                    AiMessage("user", idea.content)
                ),
                expectJson = true
            )
        ).content.ifBlank { "{}" }
        val output = runCatching { gson.fromJson(raw, BasicAiOutput::class.java) }
            .getOrDefault(BasicAiOutput())
        if (output.summary.isBlank() && output.category.isBlank()) {
            val local = localAnalyzer.analyze(idea.content)
            return AiProcessResult(local.category, local.summary, local.tags)
        }
        return AiProcessResult(category = output.category, summary = output.summary, tags = output.tags)
    }

    /**
     * 增量分析：分析新笔记与候选笔记集合的关联，返回 category/summary/tags/links/insight。
     * 候选列表最多取 30 条（调用方负责截断与排序）。
     */
    suspend fun processWithLinks(idea: IdeaEntity, candidates: List<IdeaEntity>): FullAiResult {
        val candidatePairs = candidates.map { c ->
            val text = c.summary.takeIf { it.isNotBlank() } ?: c.content
            Pair(c.id, text)
        }
        val candidateBlock = PromptBuilder.buildCandidateList(candidatePairs)
        val userMsg = buildString {
            append("【新笔记】\n")
            append(idea.content)
            append("\n\n【候选笔记列表】\n")
            append(candidateBlock)
        }

        val raw = agentRuntime.generate(
            AiModelRequest(
                taskType = AiTaskType.LinkDiscovery,
                messages = listOf(
                    AiMessage("system", PromptBuilder.incrementalLinkSystem),
                    AiMessage("user", userMsg)
                ),
                expectJson = true
            )
        ).content.ifBlank { "{}" }

        val output = runCatching { gson.fromJson(raw, IncrementalAiOutput::class.java) }
            .getOrDefault(IncrementalAiOutput())
        if (output.summary.isBlank() && output.category.isBlank()) {
            return localResult(idea, candidates)
        }

        val links = output.links.map { l ->
            LinkSuggestion(targetId = l.id, strength = l.strength.coerceIn(0f, 1f), reason = l.reason)
        }
        val insight = output.insight?.takeIf { it.content.isNotBlank() }?.let {
            InsightSuggestion(content = it.content, relatedIds = it.relatedIds)
        }

        return FullAiResult(
            category = output.category,
            summary = output.summary,
            tags = output.tags,
            links = links,
            insight = insight
        )
    }

    /**
     * 全量关联分析：一次性对所有笔记两两分析，返回有效的关联对和洞察。
     * 调用方（GraphViewModel）负责写库。
     */
    suspend fun analyzeAllLinks(ideas: List<IdeaEntity>): Pair<List<RawPair>, List<InsightSuggestion>> {
        if (ideas.size < 2) return Pair(emptyList(), emptyList())

        val candidatePairs = ideas.map { idea ->
            val text = idea.summary.takeIf { it.isNotBlank() } ?: idea.content
            Pair(idea.id, text)
        }
        val listBlock = PromptBuilder.buildCandidateList(candidatePairs)

        val raw = agentRuntime.generate(
            AiModelRequest(
                taskType = AiTaskType.LinkDiscovery,
                messages = listOf(
                    AiMessage("system", PromptBuilder.fullLinkAnalysisSystem),
                    AiMessage("user", "请分析以下笔记列表之间的关联：\n\n$listBlock")
                ),
                expectJson = true
            )
        ).content.ifBlank { "{}" }

        val output = runCatching { gson.fromJson(raw, FullLinkAiOutput::class.java) }
            .getOrDefault(FullLinkAiOutput())

        val pairs = output.pairs
            .filter { it.a.isNotBlank() && it.b.isNotBlank() && it.a != it.b }
            .map { RawPair(a = it.a, b = it.b, strength = it.strength.coerceIn(0f, 1f), reason = it.reason) }

        val insights = output.insights
            .filter { it.content.isNotBlank() }
            .map { InsightSuggestion(content = it.content, relatedIds = it.relatedIds) }

        return if (pairs.isEmpty()) localGraphResult(ideas) else Pair(pairs, insights)
    }

    private fun localResult(idea: IdeaEntity, candidates: List<IdeaEntity>): FullAiResult {
        val analysis = localAnalyzer.analyze(idea.content)
        val links = localAnalyzer.suggestLinks(idea.content, candidates)
        val strongLinks = links.filter { it.strength >= 0.7f }
        val insight = strongLinks.takeIf { it.isNotEmpty() }?.let {
            InsightSuggestion(
                content = "本地分析发现这条灵感与 ${it.size} 条既有笔记存在共同主题，可以合并为一个推进方向。",
                relatedIds = listOf(idea.id) + it.map(LinkSuggestion::targetId)
            )
        }
        return FullAiResult(
            category = analysis.category,
            summary = analysis.summary,
            tags = analysis.tags,
            links = links,
            insight = insight
        )
    }

    private fun localGraphResult(
        ideas: List<IdeaEntity>
    ): Pair<List<RawPair>, List<InsightSuggestion>> {
        val pairs = buildList {
            ideas.forEachIndexed { index, idea ->
                localAnalyzer.suggestLinks(idea.content, ideas.drop(index + 1))
                    .forEach { link ->
                        add(
                            RawPair(
                                a = idea.id,
                                b = link.targetId,
                                strength = link.strength,
                                reason = link.reason
                            )
                        )
                    }
            }
        }.distinctBy { minOf(it.a, it.b) to maxOf(it.a, it.b) }

        val insight = if (pairs.isEmpty()) {
            emptyList()
        } else {
            listOf(
                InsightSuggestion(
                    content = "本地分析已从 ${ideas.size} 条笔记中找到 ${pairs.size} 组主题关联。",
                    relatedIds = pairs.flatMap { listOf(it.a, it.b) }.distinct()
                )
            )
        }
        return pairs to insight
    }
}
