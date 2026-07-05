package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity

data class RankedIdea(
    val idea: IdeaEntity,
    val score: Double,
    val reasons: List<String>
)

data class ThemeCluster(
    val label: String,
    val ideaIds: List<String>,
    val size: Int = ideaIds.size
)

data class IdeaMemoryContext(
    val shortTermIdeas: List<IdeaEntity>,
    val longTermMatches: List<RankedIdea>,
    val themeClusters: List<ThemeCluster>,
    val dormantCandidates: List<IdeaEntity>,
    val knowledgeGraph: IdeaKnowledgeGraphSnapshot = IdeaKnowledgeGraphSnapshot(
        atoms = emptyList(),
        relations = emptyList(),
        communities = emptyList(),
        globalSummary = ""
    )
) {
    val allRelevantIdeas: List<IdeaEntity>
        get() = (shortTermIdeas + longTermMatches.map { it.idea }).distinctBy { it.id }
}
