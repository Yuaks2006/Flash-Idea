package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class IdeaMemoryRanker @Inject constructor() {

    fun buildContext(
        query: String,
        allIdeas: List<IdeaEntity>,
        selectedIdeas: List<IdeaEntity>,
        now: Long = System.currentTimeMillis(),
        longTermLimit: Int = 12
    ): IdeaMemoryContext {
        val selectedIds = selectedIdeas.map { it.id }.toSet()
        val tokens = tokenize(query)
        val ranked = allIdeas
            .filterNot { it.id in selectedIds }
            .mapNotNull { idea ->
                scoreIdea(idea, tokens, now).takeIf { it.score > 0.0 }
            }
            .sortedWith(compareByDescending<RankedIdea> { it.score }.thenByDescending { it.idea.updatedAt })
            .take(longTermLimit)

        return IdeaMemoryContext(
            shortTermIdeas = selectedIdeas.distinctBy { it.id },
            longTermMatches = ranked,
            themeClusters = buildThemeClusters(allIdeas),
            dormantCandidates = ranked
                .filter { now - max(it.idea.updatedAt, it.idea.createdAt) >= DORMANT_AGE_MS }
                .map { it.idea }
        )
    }

    private fun scoreIdea(idea: IdeaEntity, tokens: Set<String>, now: Long): RankedIdea {
        val searchable = listOf(idea.content, idea.summary, idea.category, idea.tags)
            .joinToString(" ")
            .lowercase()
        val reasons = mutableListOf<String>()
        var score = 0.0

        tokens.forEach { token ->
            if (token.isBlank()) return@forEach
            if (idea.summary.lowercase().contains(token)) {
                score += 3.0
                reasons += "摘要匹配：$token"
            } else if (idea.tags.lowercase().contains(token)) {
                score += 2.5
                reasons += "标签匹配：$token"
            } else if (idea.category.lowercase().contains(token)) {
                score += 2.0
                reasons += "分类匹配：$token"
            } else if (searchable.contains(token)) {
                score += 1.0
                reasons += "正文匹配：$token"
            }
        }

        val ageDays = ((now - max(idea.updatedAt, idea.createdAt)) / DAY_MS).coerceAtLeast(0)
        if (ageDays <= 7) {
            score += 0.5
            reasons += "近期活跃"
        }

        return RankedIdea(idea = idea, score = score, reasons = reasons.distinct())
    }

    private fun buildThemeClusters(ideas: List<IdeaEntity>): List<ThemeCluster> {
        return ideas
            .groupBy { idea -> idea.category.ifBlank { firstTag(idea.tags) ?: "未分类" } }
            .filterValues { it.size >= 2 }
            .map { (label, group) ->
                ThemeCluster(label = label, ideaIds = group.map { it.id })
            }
            .sortedWith(compareByDescending<ThemeCluster> { it.size }.thenBy { it.label })
    }

    private fun tokenize(text: String): Set<String> {
        val normalized = text.lowercase()
        val rough = normalized.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 2 }
        val cjkBigrams = normalized
            .filter { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
            .windowed(size = 2, step = 1, partialWindows = false)
        return (rough + cjkBigrams).toSet()
    }

    private fun firstTag(rawTags: String): String? =
        Regex("\"([^\"]+)\"").find(rawTags)?.groupValues?.getOrNull(1)

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val DORMANT_AGE_MS = 60L * DAY_MS
    }
}
