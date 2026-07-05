package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaMemoryRankerTest {

    private val ranker = IdeaMemoryRanker()
    private val now = 2_000_000_000_000L

    @Test
    fun `prioritizes explicit context before long term matches`() {
        val selected = idea("selected", "端侧 AI 隐私助手", tags = """["端侧","隐私"]""")
        val related = idea("related", "蓝心端侧模型做灵感整理", tags = """["端侧"]""")

        val context = ranker.buildContext(
            query = "端侧模型如何保护隐私",
            allIdeas = listOf(related, selected),
            selectedIdeas = listOf(selected),
            now = now
        )

        assertEquals(listOf("selected"), context.shortTermIdeas.map { it.id })
        assertEquals("related", context.longTermMatches.first().idea.id)
    }

    @Test
    fun `finds theme clusters and dormant candidates`() {
        val oldButRelevant = idea(
            id = "old",
            content = "科研假设生成与实验验证",
            category = "科研",
            tags = """["科研","假设"]""",
            createdAt = now - 100L * 24 * 60 * 60 * 1000
        )
        val recent = idea(
            id = "recent",
            content = "用 Agent 把灵感转成研究假设",
            category = "科研",
            tags = """["科研","Agent"]""",
            createdAt = now - 2L * 24 * 60 * 60 * 1000
        )

        val context = ranker.buildContext(
            query = "研究假设 Agent",
            allIdeas = listOf(oldButRelevant, recent),
            selectedIdeas = emptyList(),
            now = now
        )

        assertTrue(context.themeClusters.any { it.label == "科研" && it.ideaIds.containsAll(listOf("old", "recent")) })
        assertTrue(context.dormantCandidates.any { it.id == "old" })
    }

    private fun idea(
        id: String,
        content: String,
        category: String = "产品",
        tags: String = "[]",
        createdAt: Long = now
    ) = IdeaEntity(
        id = id,
        content = content,
        category = category,
        summary = content.take(20),
        tags = tags,
        createdAt = createdAt,
        updatedAt = createdAt
    )
}
