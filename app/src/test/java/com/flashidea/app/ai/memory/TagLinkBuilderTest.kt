package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagLinkBuilderTest {

    @Test
    fun `connects ideas that share at least one tag`() {
        val ideas = listOf(
            idea("a", """["Agent","产品"]"""),
            idea("b", """["Agent","设计"]"""),
            idea("c", """["科研"]""")
        )

        val links = TagLinkBuilder.buildLinks(ideas)

        assertEquals(1, links.size)
        assertEquals(setOf("a", "b"), setOf(links.first().sourceId, links.first().targetId))
        assertEquals("tag_rule", links.first().createdBy)
        assertTrue(links.first().strength >= 0.6f)
    }

    @Test
    fun `keeps tag links undirected and deterministic`() {
        val forward = TagLinkBuilder.buildLinks(
            listOf(idea("a", """["隐私"]"""), idea("b", """["隐私"]"""))
        )
        val reversed = TagLinkBuilder.buildLinks(
            listOf(idea("b", """["隐私"]"""), idea("a", """["隐私"]"""))
        )

        assertEquals(forward.map { it.id }, reversed.map { it.id })
        assertEquals("tag_a_b", forward.single().id)
    }

    private fun idea(id: String, tags: String) = IdeaEntity(
        id = id,
        content = "note $id",
        tags = tags
    )
}
