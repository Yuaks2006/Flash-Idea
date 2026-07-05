package com.flashidea.app.ai.agent

import com.flashidea.app.ai.model.AiTaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentWorkflowEngineTest {

    private val engine = AgentWorkflowEngine()

    @Test
    fun `research workflow includes generation critique ranking and evolution`() {
        val workflow = engine.buildWorkflow(
            AgentRuntimePlan(
                goal = "生成一个可验证的研究假设",
                taskType = AiTaskType.ActionPlanning,
                steps = emptyList(),
                toolCalls = listOf(
                    AgentToolCall(AgentToolName.BuildResearchHypothesis),
                    AgentToolCall(AgentToolName.FindContradictions)
                )
            )
        )

        assertEquals(AgentWorkflowKind.ResearchHypothesis, workflow.kind)
        assertTrue(workflow.nodes.any { it.phase == AgentWorkflowPhase.Generate })
        assertTrue(workflow.nodes.any { it.phase == AgentWorkflowPhase.Critique })
        assertTrue(workflow.nodes.any { it.phase == AgentWorkflowPhase.Rank })
        assertTrue(workflow.nodes.any { it.phase == AgentWorkflowPhase.Evolve })
    }

    @Test
    fun `incubation workflow expands graph before synthesis`() {
        val workflow = engine.buildWorkflow(
            AgentRuntimePlan(
                goal = "孵化旧灵感",
                taskType = AiTaskType.LinkDiscovery,
                steps = emptyList(),
                toolCalls = listOf(
                    AgentToolCall(AgentToolName.ClusterIdeas),
                    AgentToolCall(AgentToolName.ReviveDormantIdeas)
                )
            )
        )

        val phases = workflow.nodes.map { it.phase }
        assertEquals(AgentWorkflowKind.IdeaIncubation, workflow.kind)
        assertTrue(phases.indexOf(AgentWorkflowPhase.ExpandGraph) < phases.indexOf(AgentWorkflowPhase.Synthesize))
    }
}
