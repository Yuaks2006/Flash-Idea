package com.flashidea.app.ai.agent

import javax.inject.Inject
import javax.inject.Singleton

enum class AgentWorkflowKind {
    GeneralWorkbench,
    IdeaIncubation,
    ResearchHypothesis,
    ExportTransformation
}

enum class AgentWorkflowPhase {
    Observe,
    RetrieveMemory,
    ExpandGraph,
    Generate,
    Critique,
    Rank,
    Evolve,
    Synthesize,
    Reflect,
    Persist
}

data class AgentWorkflowNode(
    val phase: AgentWorkflowPhase,
    val toolCalls: List<AgentToolCall> = emptyList(),
    val description: String
)

data class AgentWorkflow(
    val kind: AgentWorkflowKind,
    val nodes: List<AgentWorkflowNode>
)

@Singleton
class AgentWorkflowEngine @Inject constructor() {

    fun buildWorkflow(plan: AgentRuntimePlan): AgentWorkflow {
        val toolNames = plan.toolCalls.map { it.name }.toSet()
        val kind = when {
            AgentToolName.BuildResearchHypothesis in toolNames -> AgentWorkflowKind.ResearchHypothesis
            AgentToolName.ReviveDormantIdeas in toolNames -> AgentWorkflowKind.IdeaIncubation
            AgentToolName.ExportSummary in toolNames -> AgentWorkflowKind.ExportTransformation
            else -> AgentWorkflowKind.GeneralWorkbench
        }

        return AgentWorkflow(
            kind = kind,
            nodes = when (kind) {
                AgentWorkflowKind.ResearchHypothesis -> researchWorkflow(plan)
                AgentWorkflowKind.IdeaIncubation -> incubationWorkflow(plan)
                AgentWorkflowKind.ExportTransformation -> exportWorkflow(plan)
                AgentWorkflowKind.GeneralWorkbench -> generalWorkflow(plan)
            }
        )
    }

    private fun generalWorkflow(plan: AgentRuntimePlan) = listOf(
        AgentWorkflowNode(AgentWorkflowPhase.Observe, description = "理解用户目标与上下文"),
        AgentWorkflowNode(AgentWorkflowPhase.RetrieveMemory, plan.toolCalls, "检索相关笔记与工具观察"),
        AgentWorkflowNode(AgentWorkflowPhase.Synthesize, description = "生成回答"),
        AgentWorkflowNode(AgentWorkflowPhase.Reflect, description = "记录运行反思"),
        AgentWorkflowNode(AgentWorkflowPhase.Persist, description = "沉淀运行轨迹")
    )

    private fun incubationWorkflow(plan: AgentRuntimePlan) = listOf(
        AgentWorkflowNode(AgentWorkflowPhase.Observe, description = "识别需要重新孵化的目标"),
        AgentWorkflowNode(AgentWorkflowPhase.RetrieveMemory, calls(plan, AgentToolName.SearchNotes), "召回长期记忆"),
        AgentWorkflowNode(AgentWorkflowPhase.ExpandGraph, calls(plan, AgentToolName.ClusterIdeas, AgentToolName.ReviveDormantIdeas), "扩展主题社区与休眠灵感"),
        AgentWorkflowNode(AgentWorkflowPhase.Generate, calls(plan, AgentToolName.DiscoverLinks), "生成新关联"),
        AgentWorkflowNode(AgentWorkflowPhase.Synthesize, description = "合成孵化方向"),
        AgentWorkflowNode(AgentWorkflowPhase.Reflect, description = "记录孵化边界"),
        AgentWorkflowNode(AgentWorkflowPhase.Persist, description = "写入运行与洞察")
    )

    private fun researchWorkflow(plan: AgentRuntimePlan) = listOf(
        AgentWorkflowNode(AgentWorkflowPhase.Observe, description = "定义科研/产品问题"),
        AgentWorkflowNode(AgentWorkflowPhase.RetrieveMemory, calls(plan, AgentToolName.SearchNotes), "召回证据与先验"),
        AgentWorkflowNode(AgentWorkflowPhase.Generate, calls(plan, AgentToolName.BuildResearchHypothesis), "生成候选假设"),
        AgentWorkflowNode(AgentWorkflowPhase.Critique, calls(plan, AgentToolName.FindContradictions), "批判假设与风险"),
        AgentWorkflowNode(AgentWorkflowPhase.Rank, description = "按新颖性、可验证性、证据密度排序"),
        AgentWorkflowNode(AgentWorkflowPhase.Evolve, calls(plan, AgentToolName.MakeActionPlan), "演化为可执行验证计划"),
        AgentWorkflowNode(AgentWorkflowPhase.Synthesize, description = "输出最终建议"),
        AgentWorkflowNode(AgentWorkflowPhase.Reflect, description = "记录可复用经验"),
        AgentWorkflowNode(AgentWorkflowPhase.Persist, description = "写入运行与洞察")
    )

    private fun exportWorkflow(plan: AgentRuntimePlan) = listOf(
        AgentWorkflowNode(AgentWorkflowPhase.Observe, description = "识别转化目标"),
        AgentWorkflowNode(AgentWorkflowPhase.RetrieveMemory, calls(plan, AgentToolName.SearchNotes), "召回相关素材"),
        AgentWorkflowNode(AgentWorkflowPhase.ExpandGraph, calls(plan, AgentToolName.ClusterIdeas), "整理主题结构"),
        AgentWorkflowNode(AgentWorkflowPhase.Synthesize, calls(plan, AgentToolName.ExportSummary), "生成可导出结构"),
        AgentWorkflowNode(AgentWorkflowPhase.Reflect, description = "记录转化结果"),
        AgentWorkflowNode(AgentWorkflowPhase.Persist, description = "写入运行与洞察")
    )

    private fun calls(plan: AgentRuntimePlan, vararg names: AgentToolName): List<AgentToolCall> =
        plan.toolCalls.filter { it.name in names }
}
