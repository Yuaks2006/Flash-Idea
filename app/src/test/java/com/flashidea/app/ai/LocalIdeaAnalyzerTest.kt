package com.flashidea.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalIdeaAnalyzerTest {

    private val analyzer = LocalIdeaAnalyzer()

    @Test
    fun `classifies actionable content and creates useful summary`() {
        val result = analyzer.analyze("明天下午完成复赛演示视频，并整理三项核心卖点")

        assertEquals("任务", result.category)
        assertTrue(result.summary.contains("复赛演示视频"))
        assertTrue(result.tags.contains("复赛"))
    }

    @Test
    fun `classifies product concept as project seed`() {
        val result = analyzer.analyze("做一个能在本地连接碎片灵感的 AI 产品")

        assertEquals("项目火种", result.category)
        assertTrue(result.tags.isNotEmpty())
        assertTrue(result.summary.length <= 48)
    }

    @Test
    fun `blank content returns empty analysis`() {
        val result = analyzer.analyze("   ")

        assertEquals("", result.category)
        assertEquals("", result.summary)
        assertTrue(result.tags.isEmpty())
    }
}
