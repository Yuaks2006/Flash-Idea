package com.flashidea.app.ui.settings

import com.flashidea.app.data.local.IdeaEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFormatTest {

    private val note = IdeaEntity(
        id = "idea-1",
        content = "打磨 Flash Idea 复赛版本",
        category = "项目火种",
        summary = "完成复赛产品化",
        tags = """["复赛","产品"]""",
        createdAt = 1_700_000_000_000
    )

    @Test
    fun `markdown includes title content summary and tags`() {
        val markdown = ExportFormat.toMarkdown(listOf(note))

        assertTrue(markdown.contains("打磨 Flash Idea 复赛版本"))
        assertTrue(markdown.contains("完成复赛产品化"))
        assertTrue(markdown.contains("#复赛"))
    }

    @Test
    fun `json includes tags and timestamps`() {
        val json = ExportFormat.toJson(listOf(note))

        assertTrue(json.contains("\"tags\""))
        assertTrue(json.contains("\"createdAt\""))
        assertTrue(json.contains("idea-1"))
    }
}
