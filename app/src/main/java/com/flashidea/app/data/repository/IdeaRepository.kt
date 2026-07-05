package com.flashidea.app.data.repository

import com.flashidea.app.data.local.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdeaRepository @Inject constructor(
    private val ideaDao: IdeaDao,
    private val linkDao: LinkDao,
    private val insightDao: InsightDao,
    private val agentRunDao: AgentRunDao,
    private val memoryGraphDao: MemoryGraphDao
) {
    fun getAllIdeas(): Flow<List<IdeaEntity>> = ideaDao.getAllIdeas()
    fun searchIdeas(query: String): Flow<List<IdeaEntity>> = ideaDao.searchIdeas(query)
    suspend fun getIdeaById(id: String): IdeaEntity? = ideaDao.getIdeaById(id)
    suspend fun saveIdea(idea: IdeaEntity) = ideaDao.insert(idea)
    suspend fun updateIdea(idea: IdeaEntity) = ideaDao.update(idea)
    suspend fun deleteIdea(id: String) {
        ideaDao.deleteById(id)
        linkDao.deleteLinksForIdea(id)
    }

    fun getAllLinks(): Flow<List<LinkEntity>> = linkDao.getAllLinks()
    fun getLinksForIdea(ideaId: String): Flow<List<LinkEntity>> = linkDao.getLinksForIdea(ideaId)
    suspend fun saveLink(link: LinkEntity) = linkDao.insert(link)

    fun getAllInsights(): Flow<List<InsightEntity>> = insightDao.getAllInsights()
    fun hasUnreadInsights(): Flow<Boolean> = insightDao.hasUnreadInsights()
    suspend fun saveInsight(insight: InsightEntity) = insightDao.insert(insight)
    suspend fun markAllInsightsRead() = insightDao.markAllRead()

    fun getRecentAgentRuns(limit: Int = 30): Flow<List<AgentRunEntity>> =
        agentRunDao.getRecentRuns(limit)

    suspend fun saveAgentRun(run: AgentRunEntity) = agentRunDao.save(run)

    fun getMemoryAtoms(): Flow<List<MemoryAtomEntity>> = memoryGraphDao.getAtoms()
    fun getMemoryRelations(): Flow<List<MemoryRelationEntity>> = memoryGraphDao.getRelations()
    fun getMemoryCommunities(): Flow<List<MemoryCommunityEntity>> = memoryGraphDao.getCommunities()

    suspend fun saveMemoryGraph(
        atoms: List<MemoryAtomEntity>,
        relations: List<MemoryRelationEntity>,
        communities: List<MemoryCommunityEntity>
    ) {
        memoryGraphDao.saveAtoms(atoms)
        memoryGraphDao.saveRelations(relations)
        memoryGraphDao.saveCommunities(communities)
    }
}
