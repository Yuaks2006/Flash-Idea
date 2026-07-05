package com.flashidea.app.ai.agent

import com.flashidea.app.ai.model.AiTaskType
import javax.inject.Inject
import javax.inject.Singleton

data class AgentRuntimePlan(
    val goal: String,
    val taskType: AiTaskType,
    val steps: List<AgentStep>,
    val toolCalls: List<AgentToolCall>
)

@Singleton
class AgentTaskPlanner @Inject constructor() {

    fun plan(goal: String): AgentRuntimePlan {
        val normalized = goal.lowercase()
        val taskType = when {
            normalized.hasAny("研究假设", "假设", "实验", "验证", "scientific", "hypothesis") -> AiTaskType.ActionPlanning
            normalized.hasAny("孵化", "旧灵感", "沉睡", "休眠", "重新", "revive") -> AiTaskType.LinkDiscovery
            normalized.hasAny("关联", "关系", "隐藏", "link", "relation") -> AiTaskType.LinkDiscovery
            normalized.hasAny("行动", "计划", "下一步", "拆解", "action", "plan") -> AiTaskType.ActionPlanning
            normalized.hasAny("导出", "总结", "摘要", "ppt", "路演", "材料", "export") -> AiTaskType.ExportSummary
            normalized.hasAny("分类", "标签", "分析笔记", "analyze") -> AiTaskType.NoteAnalysis
            else -> AiTaskType.ChatAssistant
        }

        val toolCalls = when (taskType) {
            AiTaskType.LinkDiscovery -> if (normalized.hasAny("孵化", "旧灵感", "沉睡", "休眠", "重新", "revive")) {
                listOf(
                    AgentToolCall(AgentToolName.SearchNotes, goal),
                    AgentToolCall(AgentToolName.ClusterIdeas, goal),
                    AgentToolCall(AgentToolName.ReviveDormantIdeas, goal),
                    AgentToolCall(AgentToolName.DiscoverLinks, goal)
                )
            } else {
                listOf(
                    AgentToolCall(AgentToolName.SearchNotes, goal),
                    AgentToolCall(AgentToolName.ClusterIdeas, goal),
                    AgentToolCall(AgentToolName.DiscoverLinks, goal)
                )
            }
            AiTaskType.ActionPlanning -> if (normalized.hasAny("研究假设", "假设", "实验", "验证", "scientific", "hypothesis")) {
                listOf(
                    AgentToolCall(AgentToolName.SearchNotes, goal),
                    AgentToolCall(AgentToolName.FindContradictions, goal),
                    AgentToolCall(AgentToolName.BuildResearchHypothesis, goal),
                    AgentToolCall(AgentToolName.MakeActionPlan, goal)
                )
            } else {
                listOf(
                    AgentToolCall(AgentToolName.SearchNotes, goal),
                    AgentToolCall(AgentToolName.FindContradictions, goal),
                    AgentToolCall(AgentToolName.MakeActionPlan, goal)
                )
            }
            AiTaskType.ExportSummary -> listOf(
                AgentToolCall(AgentToolName.SearchNotes, goal),
                AgentToolCall(AgentToolName.ClusterIdeas, goal),
                AgentToolCall(AgentToolName.ExportSummary, goal)
            )
            AiTaskType.NoteAnalysis -> listOf(AgentToolCall(AgentToolName.AnalyzeNote, goal))
            AiTaskType.ChatAssistant -> listOf(AgentToolCall(AgentToolName.SearchNotes, goal, limit = 3))
        }

        return AgentRuntimePlan(
            goal = goal,
            taskType = taskType,
            steps = listOf(
                AgentStep("识别任务类型：${taskType.name}"),
                AgentStep("构建本地笔记上下文"),
                AgentStep("按需调用内部工具"),
                AgentStep("选择模型并生成结果")
            ),
            toolCalls = toolCalls
        )
    }

    private fun String.hasAny(vararg keywords: String): Boolean =
        keywords.any { contains(it) }
}
