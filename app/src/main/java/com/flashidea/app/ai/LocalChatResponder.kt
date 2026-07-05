package com.flashidea.app.ai

import com.flashidea.app.data.local.IdeaEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalChatResponder @Inject constructor() {

    fun reply(userMessage: String, contextNotes: List<IdeaEntity>): String {
        val focus = contextNotes.firstOrNull()?.let {
            it.summary.ifBlank { it.content.trim().replace('\n', ' ').take(36) }
        }
        val subject = focus?.let { "“$it”" } ?: "这个想法"

        return when {
            userMessage.contains("行动") || userMessage.contains("计划") ->
                """
                围绕$subject，可以先压缩成三个下一步：

                1. 写清目标用户、核心场景和唯一价值。
                2. 做一个 30 分钟内能验证的最小实验。
                3. 记录结果，只保留真正影响体验的改动。

                建议今天先完成第 1 步，并给它设一个明确截止时间。
                """.trimIndent()

            userMessage.contains("风险") || userMessage.contains("问题") ->
                """
                ${subject}目前最值得检查的风险有三类：

                1. 用户是否真的愿意改变现有习惯。
                2. 核心能力在离线、弱网或无权限时是否仍可用。
                3. 演示亮点是否能转化为长期价值。

                先用一个真实用户和一次完整流程验证第 1、2 点，结论会比继续扩功能更有价值。
                """.trimIndent()

            else ->
                """
                我先把${subject}拆成一条更清晰的推进链：

                核心假设 → 最小验证 → 用户反馈 → 下一次迭代。

                你现在可以补充“谁会在什么时刻使用它”，我再帮你把它收敛成具体方案。当前为本地离线助手，未上传任何笔记。
                """.trimIndent()
        }
    }
}
