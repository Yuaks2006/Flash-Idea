package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.local.LinkEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaKnowledgeGraphBuilderTest {

    private val builder = IdeaKnowledgeGraphBuilder()

    @Test
    fun `builds atoms relations and communities from ideas and links`() {
        val ideas = listOf(
            IdeaEntity(
                id = "idea-a",
                content = "端侧 AI 保护隐私并整理灵感",
                category = "产品",
                summary = "端侧隐私灵感助手",
                tags = """["端侧","隐私","Agent"]""",
                createdAt = 1_000
            ),
            IdeaEntity(
                id = "idea-b",
                content = "把旧灵感孵化成研究假设",
                category = "科研",
                summary = "灵感转研究假设",
                tags = """["科研","Agent"]""",
                createdAt = 2_000
            )
        )
        val links = listOf(
            LinkEntity(sourceId = "idea-a", targetId = "idea-b", strength = 0.82f)
        )

        val snapshot = builder.buildSnapshot(ideas, links)

        assertTrue(snapshot.atoms.any { it.type == MemoryAtomType.Theme && it.label == "Agent" })
        assertTrue(snapshot.relations.any { it.type == MemoryRelationType.IdeaRelatedTo && it.weight == 0.82 })
        assertTrue(snapshot.communities.any { it.label == "Agent" && it.ideaIds.containsAll(listOf("idea-a", "idea-b")) })
        assertTrue(snapshot.globalSummary.contains("2 条笔记"))
    }
}
