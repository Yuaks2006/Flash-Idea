package com.flashidea.app.ai.agent

import com.flashidea.app.ai.memory.IdeaMemoryContext
import com.flashidea.app.ai.memory.IdeaMemoryService
import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.repository.IdeaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    private val repository: IdeaRepository,
    private val memoryService: IdeaMemoryService
) {
    private val tools = listOf(
        AgentTool(
            name = AgentToolName.SearchNotes,
            description = "搜索用户已保存的灵感笔记，用于回答上下文问题或寻找相关想法。"
        ),
        AgentTool(
            name = AgentToolName.AnalyzeNote,
            description = "为新笔记生成分类、摘要和标签。"
        ),
        AgentTool(
            name = AgentToolName.DiscoverLinks,
            description = "发现笔记之间的隐藏关联，并为图谱生成关联建议。"
        ),
        AgentTool(
            name = AgentToolName.MakeActionPlan,
            description = "把灵感拆解为下一步可执行计划。"
        ),
        AgentTool(
            name = AgentToolName.ExportSummary,
            description = "为导出、PPT 或宣传视频生成摘要材料。"
        ),
        AgentTool(
            name = AgentToolName.ClusterIdeas,
            description = "把长期沉淀的想法聚合成主题簇。"
        ),
        AgentTool(
            name = AgentToolName.FindContradictions,
            description = "寻找笔记之间的矛盾、风险和待验证假设。"
        ),
        AgentTool(
            name = AgentToolName.ReviveDormantIdeas,
            description = "从旧灵感中找出值得重新孵化的方向。"
        ),
        AgentTool(
            name = AgentToolName.BuildResearchHypothesis,
            description = "把想法转化为可验证的科研或产品假设。"
        )
    )

    fun availableTools(): List<AgentTool> = tools

    suspend fun execute(call: AgentToolCall, context: AgentToolContext): AgentToolResult {
        val memory = memoryService.buildContext(context.goal, context.contextNotes)
        val notes = memory.allRelevantIdeas.ifEmpty { repository.getAllIdeas().first() }

        return when (call.name) {
            AgentToolName.SearchNotes -> searchNotes(call, notes)
            AgentToolName.AnalyzeNote -> AgentToolResult(
                name = call.name,
                content = "待分析内容：${call.input.ifBlank { context.goal }.take(500)}"
            )
            AgentToolName.DiscoverLinks -> AgentToolResult(
                name = call.name,
                content = buildLinkObservation(notes),
                relatedIdeaIds = notes.take(5).map { it.id }
            )
            AgentToolName.MakeActionPlan -> AgentToolResult(
                name = call.name,
                content = buildActionPlanObservation(context.goal, notes),
                relatedIdeaIds = notes.take(5).map { it.id }
            )
            AgentToolName.ExportSummary -> AgentToolResult(
                name = call.name,
                content = buildExportObservation(notes),
                relatedIdeaIds = notes.take(8).map { it.id }
            )
            AgentToolName.ClusterIdeas -> AgentToolResult(
                name = call.name,
                content = buildClusterObservation(memory),
                relatedIdeaIds = memory.themeClusters.flatMap { it.ideaIds }.distinct()
            )
            AgentToolName.FindContradictions -> AgentToolResult(
                name = call.name,
                content = buildContradictionObservation(notes),
                relatedIdeaIds = notes.take(8).map { it.id }
            )
            AgentToolName.ReviveDormantIdeas -> AgentToolResult(
                name = call.name,
                content = buildDormantObservation(memory),
                relatedIdeaIds = memory.dormantCandidates.map { it.id }
            )
            AgentToolName.BuildResearchHypothesis -> AgentToolResult(
                name = call.name,
                content = buildHypothesisObservation(context.goal, notes),
                relatedIdeaIds = notes.take(6).map { it.id }
            )
        }
    }

    private fun searchNotes(call: AgentToolCall, notes: List<IdeaEntity>): AgentToolResult {
        val query = call.input.trim()
        val keywords = query
            .split(Regex("\\s+|，|,|。|？|\\?"))
            .map { it.trim() }
            .filter { it.length >= 2 }

        val matched = notes
            .filter { note ->
                keywords.isEmpty() || keywords.any { word ->
                    note.content.contains(word, ignoreCase = true) ||
                        note.summary.contains(word, ignoreCase = true) ||
                        note.category.contains(word, ignoreCase = true) ||
                        note.tags.contains(word, ignoreCase = true)
                }
            }
            .take(call.limit)

        return AgentToolResult(
            name = call.name,
            content = if (matched.isEmpty()) {
                "没有找到直接匹配的历史笔记。"
            } else {
                matched.joinToString("\n") { note ->
                    "- ${note.id}: ${note.summary.ifBlank { note.content.take(80) }}"
                }
            },
            relatedIdeaIds = matched.map { it.id }
        )
    }

    private fun buildLinkObservation(notes: List<IdeaEntity>): String {
        if (notes.size < 2) return "当前上下文笔记不足，暂时无法形成可靠关联。"
        return notes.take(6).joinToString("\n") { note ->
            "- ${note.id}: ${note.category.ifBlank { "未分类" }} / ${note.summary.ifBlank { note.content.take(80) }}"
        }
    }

    private fun buildActionPlanObservation(goal: String, notes: List<IdeaEntity>): String = buildString {
        append("用户目标：").append(goal).append('\n')
        append("可用于拆解的上下文：\n")
        notes.take(5).forEachIndexed { index, note ->
            append("${index + 1}. ")
            append(note.summary.ifBlank { note.content.take(80) })
            append('\n')
        }
    }.trim()

    private fun buildExportObservation(notes: List<IdeaEntity>): String {
        if (notes.isEmpty()) return "当前没有可导出的笔记上下文。"
        val categories = notes.groupingBy { it.category.ifBlank { "未分类" } }.eachCount()
        return buildString {
            append("笔记数量：").append(notes.size).append('\n')
            append("主题分布：")
            append(categories.entries.joinToString("；") { "${it.key} ${it.value} 条" })
            append("\n代表性笔记：\n")
            notes.take(6).forEach { note ->
                append("- ").append(note.summary.ifBlank { note.content.take(80) }).append('\n')
            }
        }.trim()
    }

    private fun buildClusterObservation(memory: IdeaMemoryContext): String {
        val graphCommunities = memory.knowledgeGraph.communities.take(6)
        if (memory.themeClusters.isEmpty() && graphCommunities.isEmpty()) return "暂未形成稳定主题簇。"
        return buildString {
            if (memory.knowledgeGraph.globalSummary.isNotBlank()) {
                append(memory.knowledgeGraph.globalSummary).append('\n')
            }
            graphCommunities.forEach { community ->
                append("- ")
                append(community.label)
                append(": ")
                append(community.summary)
                append(" ids=")
                append(community.ideaIds.joinToString(","))
                append('\n')
            }
            memory.themeClusters
                .filterNot { cluster -> graphCommunities.any { it.label == cluster.label } }
                .take(4)
                .forEach { cluster ->
                    append("- ")
                    append(cluster.label)
                    append(": ")
                    append(cluster.size)
                    append(" 条笔记，ids=")
                    append(cluster.ideaIds.joinToString(","))
                    append('\n')
                }
        }
    }

    private fun buildContradictionObservation(notes: List<IdeaEntity>): String {
        val riskNotes = notes.filter { note ->
            val text = "${note.content} ${note.summary}"
            listOf("但是", "问题", "风险", "担心", "不确定", "反例", "限制").any { text.contains(it) }
        }
        if (riskNotes.isEmpty()) {
            return "未发现显式矛盾；建议从目标用户、数据来源、验证成本三个角度继续追问。"
        }
        return riskNotes.take(5).joinToString("\n") { note ->
            "- ${note.id}: ${note.summary.ifBlank { note.content.take(80) }}"
        }
    }

    private fun buildDormantObservation(memory: IdeaMemoryContext): String {
        if (memory.dormantCandidates.isEmpty()) return "没有找到与当前目标强相关的休眠灵感。"
        return memory.dormantCandidates.take(5).joinToString("\n") { note ->
            "- ${note.id}: ${note.summary.ifBlank { note.content.take(80) }}，可与当前问题重新组合。"
        }
    }

    private fun buildHypothesisObservation(goal: String, notes: List<IdeaEntity>): String {
        val basis = notes.take(4).joinToString("；") { it.summary.ifBlank { it.content.take(40) } }
        return """
研究/产品目标：$goal
可验证假设模板：
1. 如果将「${basis.ifBlank { "当前灵感" }}」组织成一个明确场景，则目标用户会更快形成下一步行动。
2. 关键变量：输入灵感质量、历史笔记关联密度、生成结果可执行性。
3. 验证方式：让用户对 Agent 输出的关联、计划、假设分别打分，并记录是否被采纳。
""".trimIndent()
    }
}
