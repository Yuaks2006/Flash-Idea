package com.flashidea.app.ai.agent

import com.flashidea.app.ai.model.AiTaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTaskPlannerTest {

    private val planner = AgentTaskPlanner()

    @Test
    fun `plans link discovery when user asks for hidden relations`() {
        val plan = planner.plan("帮我找出这些灵感之间的隐藏关联")

        assertEquals(AiTaskType.LinkDiscovery, plan.taskType)
        assertTrue(plan.toolCalls.any { it.name == AgentToolName.DiscoverLinks })
    }

    @Test
    fun `plans export summary when user asks for presentation material`() {
        val plan = planner.plan("把我最近的想法整理成路演 PPT 大纲")

        assertEquals(AiTaskType.ExportSummary, plan.taskType)
        assertTrue(plan.toolCalls.any { it.name == AgentToolName.ExportSummary })
    }

    @Test
    fun `defaults to chat assistant with note search support`() {
        val plan = planner.plan("这个想法有什么风险")

        assertEquals(AiTaskType.ChatAssistant, plan.taskType)
        assertTrue(plan.toolCalls.any { it.name == AgentToolName.SearchNotes })
    }

    @Test
    fun `plans research hypothesis building for scientific intent`() {
        val plan = planner.plan("基于这些笔记帮我生成一个可验证的研究假设")

        assertEquals(AiTaskType.ActionPlanning, plan.taskType)
        assertTrue(plan.toolCalls.any { it.name == AgentToolName.BuildResearchHypothesis })
        assertTrue(plan.toolCalls.any { it.name == AgentToolName.FindContradictions })
    }

    @Test
    fun `plans dormant idea revival for incubation intent`() {
        val plan = planner.plan("帮我从旧灵感里找几个值得重新孵化的方向")

        assertEquals(AiTaskType.LinkDiscovery, plan.taskType)
        assertTrue(plan.toolCalls.any { it.name == AgentToolName.ReviveDormantIdeas })
        assertTrue(plan.toolCalls.any { it.name == AgentToolName.ClusterIdeas })
    }
}
