package com.flashidea.app.ai

import com.flashidea.app.data.local.IdeaEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalChatResponderTest {

    private val responder = LocalChatResponder()

    @Test
    fun `responds with next steps using referenced notes`() {
        val reply = responder.reply(
            userMessage = "帮我把这个想法变成行动计划",
            contextNotes = listOf(
                IdeaEntity(
                    content = "做一个端侧隐私灵感助手",
                    summary = "端侧隐私型灵感助手"
                )
            )
        )

        assertTrue(reply.contains("端侧隐私型灵感助手"))
        assertTrue(reply.contains("下一步"))
        assertTrue(reply.contains("1."))
    }

    @Test
    fun `responds helpfully without context`() {
        val reply = responder.reply("这个想法有什么风险？", emptyList())

        assertTrue(reply.contains("风险"))
        assertTrue(reply.length > 30)
    }
}
