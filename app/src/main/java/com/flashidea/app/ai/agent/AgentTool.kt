package com.flashidea.app.ai.agent

enum class AgentToolName(val id: String) {
    SearchNotes("search_notes"),
    AnalyzeNote("analyze_note"),
    DiscoverLinks("discover_links"),
    MakeActionPlan("make_action_plan"),
    ExportSummary("export_summary"),
    ClusterIdeas("cluster_ideas"),
    FindContradictions("find_contradictions"),
    ReviveDormantIdeas("revive_dormant_ideas"),
    BuildResearchHypothesis("build_research_hypothesis")
}

data class AgentTool(
    val name: AgentToolName,
    val description: String,
    val requiresConfirmation: Boolean = false
) {
    val id: String get() = name.id
}

data class AgentToolCall(
    val name: AgentToolName,
    val input: String = "",
    val limit: Int = 5
)

data class AgentToolContext(
    val goal: String,
    val contextNotes: List<com.flashidea.app.data.local.IdeaEntity> = emptyList()
)

data class AgentToolResult(
    val name: AgentToolName,
    val content: String,
    val relatedIdeaIds: List<String> = emptyList(),
    val success: Boolean = true
)
