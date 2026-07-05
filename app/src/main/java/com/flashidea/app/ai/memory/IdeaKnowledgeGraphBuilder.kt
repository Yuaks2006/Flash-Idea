package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.local.LinkEntity
import javax.inject.Inject
import javax.inject.Singleton

enum class MemoryAtomType {
    Idea,
    Theme,
    Question,
    Hypothesis,
    Constraint,
    Action
}

enum class MemoryRelationType {
    IdeaHasTheme,
    IdeaRelatedTo,
    SupportsHypothesis,
    HasConstraint,
    NextAction
}

data class MemoryAtom(
    val id: String,
    val type: MemoryAtomType,
    val label: String,
    val sourceIdeaId: String = "",
    val createdAt: Long = 0L
)

data class MemoryRelation(
    val sourceId: String,
    val targetId: String,
    val type: MemoryRelationType,
    val weight: Double,
    val evidence: String = ""
)

data class KnowledgeCommunity(
    val label: String,
    val ideaIds: List<String>,
    val summary: String
)

data class IdeaKnowledgeGraphSnapshot(
    val atoms: List<MemoryAtom>,
    val relations: List<MemoryRelation>,
    val communities: List<KnowledgeCommunity>,
    val globalSummary: String
)

@Singleton
class IdeaKnowledgeGraphBuilder @Inject constructor() {

    fun buildSnapshot(
        ideas: List<IdeaEntity>,
        links: List<LinkEntity>
    ): IdeaKnowledgeGraphSnapshot {
        val atoms = mutableListOf<MemoryAtom>()
        val relations = mutableListOf<MemoryRelation>()

        ideas.forEach { idea ->
            val ideaAtom = MemoryAtom(
                id = idea.id,
                type = MemoryAtomType.Idea,
                label = idea.summary.ifBlank { idea.content.take(30) },
                sourceIdeaId = idea.id,
                createdAt = idea.createdAt
            )
            atoms += ideaAtom

            extractThemes(idea).forEach { theme ->
                val themeId = "theme:${theme.lowercase()}"
                atoms += MemoryAtom(
                    id = themeId,
                    type = MemoryAtomType.Theme,
                    label = theme,
                    sourceIdeaId = idea.id,
                    createdAt = idea.createdAt
                )
                relations += MemoryRelation(
                    sourceId = idea.id,
                    targetId = themeId,
                    type = MemoryRelationType.IdeaHasTheme,
                    weight = 0.72,
                    evidence = "分类/标签/摘要提取"
                )
            }

            inferSpecialAtoms(idea).forEach { atom ->
                atoms += atom
                relations += MemoryRelation(
                    sourceId = idea.id,
                    targetId = atom.id,
                    type = when (atom.type) {
                        MemoryAtomType.Hypothesis -> MemoryRelationType.SupportsHypothesis
                        MemoryAtomType.Constraint -> MemoryRelationType.HasConstraint
                        MemoryAtomType.Action -> MemoryRelationType.NextAction
                        else -> MemoryRelationType.IdeaRelatedTo
                    },
                    weight = 0.64,
                    evidence = "文本意图提取"
                )
            }
        }

        links.forEach { link ->
            relations += MemoryRelation(
                sourceId = link.sourceId,
                targetId = link.targetId,
                type = MemoryRelationType.IdeaRelatedTo,
                weight = (link.strength * 100).toInt() / 100.0,
                evidence = "Flash Idea 双链"
            )
        }

        val communities = buildCommunities(ideas)
        return IdeaKnowledgeGraphSnapshot(
            atoms = atoms.distinctBy { it.id to it.sourceIdeaId },
            relations = relations.distinctBy { Triple(it.sourceId, it.targetId, it.type) },
            communities = communities,
            globalSummary = "图谱覆盖 ${ideas.size} 条笔记、${communities.size} 个主题社区、${links.size} 条显式关联。"
        )
    }

    private fun extractThemes(idea: IdeaEntity): List<String> {
        val tags = Regex("\"([^\"]+)\"").findAll(idea.tags).map { it.groupValues[1] }.toList()
        val category = idea.category.takeIf { it.isNotBlank() }
        val summaryTokens = idea.summary
            .split(Regex("\\s+|，|,|。|/|\\|"))
            .map { it.trim() }
            .filter { it.length in 2..12 }
            .take(3)
        return (tags + listOfNotNull(category) + summaryTokens).distinct().take(8)
    }

    private fun inferSpecialAtoms(idea: IdeaEntity): List<MemoryAtom> {
        val text = "${idea.content} ${idea.summary}"
        val atoms = mutableListOf<MemoryAtom>()
        if (listOf("假设", "验证", "实验").any { text.contains(it) }) {
            atoms += MemoryAtom(
                id = "hypothesis:${idea.id}",
                type = MemoryAtomType.Hypothesis,
                label = idea.summary.ifBlank { idea.content.take(40) },
                sourceIdeaId = idea.id,
                createdAt = idea.createdAt
            )
        }
        if (listOf("风险", "问题", "限制", "担心", "不确定").any { text.contains(it) }) {
            atoms += MemoryAtom(
                id = "constraint:${idea.id}",
                type = MemoryAtomType.Constraint,
                label = idea.summary.ifBlank { idea.content.take(40) },
                sourceIdeaId = idea.id,
                createdAt = idea.createdAt
            )
        }
        if (listOf("下一步", "计划", "行动", "待办").any { text.contains(it) }) {
            atoms += MemoryAtom(
                id = "action:${idea.id}",
                type = MemoryAtomType.Action,
                label = idea.summary.ifBlank { idea.content.take(40) },
                sourceIdeaId = idea.id,
                createdAt = idea.createdAt
            )
        }
        return atoms
    }

    private fun buildCommunities(ideas: List<IdeaEntity>): List<KnowledgeCommunity> {
        val pairs = ideas.flatMap { idea ->
            extractThemes(idea).map { theme -> theme to idea }
        }
        return pairs
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size >= 2 }
            .map { (label, group) ->
                val sorted = group.distinctBy { it.id }.sortedByDescending { it.updatedAt }
                KnowledgeCommunity(
                    label = label,
                    ideaIds = sorted.map { it.id },
                    summary = "$label 主题下有 ${sorted.size} 条想法，适合做跨笔记综合。"
                )
            }
            .sortedWith(compareByDescending<KnowledgeCommunity> { it.ideaIds.size }.thenBy { it.label })
    }
}
