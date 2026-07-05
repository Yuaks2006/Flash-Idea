package com.flashidea.app.ai

import com.flashidea.app.data.local.IdeaEntity
import javax.inject.Inject
import javax.inject.Singleton

data class LocalIdeaAnalysis(
    val category: String,
    val summary: String,
    val tags: List<String>
)

@Singleton
class LocalIdeaAnalyzer @Inject constructor() {

    private val tagKeywords = linkedMapOf(
        "复赛" to listOf("复赛", "比赛", "答辩", "演示"),
        "产品" to listOf("产品", "用户", "体验", "功能"),
        "AI" to listOf("AI", "模型", "智能", "蓝心"),
        "隐私" to listOf("隐私", "本地", "端侧", "离线"),
        "开发" to listOf("开发", "代码", "Android", "Compose"),
        "内容" to listOf("视频", "文案", "文章", "脚本"),
        "学习" to listOf("学习", "课程", "阅读", "知识"),
        "生活" to listOf("生活", "旅行", "健康", "运动")
    )

    fun analyze(content: String): LocalIdeaAnalysis {
        val normalized = content.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return LocalIdeaAnalysis("", "", emptyList())

        val category = when {
            containsAny(normalized, "完成", "整理", "提交", "明天", "待办", "计划", "记得") -> "任务"
            normalized.contains("？") || normalized.contains("?") ||
                containsAny(normalized, "为什么", "如何", "怎么", "是否") -> "问题"
            containsAny(normalized, "做一个", "产品", "项目", "方案", "应用", "App", "APP") -> "项目火种"
            containsAny(normalized, "开心", "焦虑", "难过", "期待", "感受") -> "情绪"
            containsAny(normalized, "发现", "观察", "注意到", "看到") -> "观察"
            else -> "灵感"
        }

        val summary = normalized
            .split(Regex("[。！？!?；;\\n]"))
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .take(48)

        val tags = tagKeywords
            .filterValues { keywords -> keywords.any(normalized::contains) }
            .keys
            .take(4)
            .toMutableList()
            .apply {
                if (isEmpty()) add(category)
            }

        return LocalIdeaAnalysis(category, summary, tags)
    }

    fun suggestLinks(
        content: String,
        candidates: List<IdeaEntity>
    ): List<LinkSuggestion> {
        val sourceTokens = tokens(content)
        if (sourceTokens.isEmpty()) return emptyList()

        return candidates.mapNotNull { candidate ->
            val candidateText = listOf(candidate.content, candidate.summary, candidate.category)
                .joinToString(" ")
            val targetTokens = tokens(candidateText)
            val overlap = sourceTokens.intersect(targetTokens).size
            if (overlap == 0) return@mapNotNull null

            val strength = (0.45f + overlap * 0.12f).coerceAtMost(0.88f)
            LinkSuggestion(
                targetId = candidate.id,
                strength = strength,
                reason = "本地关键词发现 $overlap 个共同主题"
            )
        }.sortedByDescending(LinkSuggestion::strength).take(5)
    }

    private fun tokens(text: String): Set<String> {
        val normalized = text.lowercase()
        val keywordTokens = tagKeywords
            .filterValues { keywords -> keywords.any { normalized.contains(it.lowercase()) } }
            .keys
        val wordTokens = Regex("[a-zA-Z0-9]{2,}|[\\u4e00-\\u9fa5]{2,}")
            .findAll(normalized)
            .map { it.value }
            .toList()
        return (keywordTokens + wordTokens).toSet()
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any(text::contains)
}
