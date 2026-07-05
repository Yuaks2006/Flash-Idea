package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.local.MemoryAtomEntity
import com.flashidea.app.data.local.MemoryCommunityEntity
import com.flashidea.app.data.local.MemoryRelationEntity
import com.flashidea.app.data.repository.IdeaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdeaMemoryService @Inject constructor(
    private val repository: IdeaRepository,
    private val ranker: IdeaMemoryRanker,
    private val graphBuilder: IdeaKnowledgeGraphBuilder
) {
    suspend fun buildContext(
        query: String,
        selectedIdeas: List<IdeaEntity> = emptyList()
    ): IdeaMemoryContext {
        val ideas = repository.getAllIdeas().first()
        val links = repository.getAllLinks().first()
        val graph = graphBuilder.buildSnapshot(ideas, links)
        persistGraph(graph)
        val ranked = ranker.buildContext(
            query = query,
            allIdeas = ideas,
            selectedIdeas = selectedIdeas
        )
        return ranked.copy(knowledgeGraph = graph)
    }

    private suspend fun persistGraph(graph: IdeaKnowledgeGraphSnapshot) {
        repository.saveMemoryGraph(
            atoms = graph.atoms.map { atom ->
                MemoryAtomEntity(
                    id = atom.id,
                    type = atom.type.name,
                    label = atom.label,
                    sourceIdeaId = atom.sourceIdeaId,
                    createdAt = atom.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            },
            relations = graph.relations.map { relation ->
                MemoryRelationEntity(
                    id = "${relation.sourceId}:${relation.type.name}:${relation.targetId}",
                    sourceId = relation.sourceId,
                    targetId = relation.targetId,
                    type = relation.type.name,
                    weight = relation.weight,
                    evidence = relation.evidence
                )
            },
            communities = graph.communities.map { community ->
                MemoryCommunityEntity(
                    id = "community:${community.label.lowercase()}",
                    label = community.label,
                    ideaIds = community.ideaIds.joinToString(","),
                    summary = community.summary
                )
            }
        )
    }
}
