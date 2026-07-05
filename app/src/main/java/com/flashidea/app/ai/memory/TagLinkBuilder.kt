package com.flashidea.app.ai.memory

import com.flashidea.app.data.local.IdeaEntity
import com.flashidea.app.data.local.LinkEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TagLinkBuilder {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    fun buildLinks(ideas: List<IdeaEntity>): List<LinkEntity> {
        val tagsByIdea = ideas.associate { idea ->
            idea.id to parseTags(idea.tags)
        }

        val links = mutableListOf<LinkEntity>()
        for (i in ideas.indices) {
            for (j in i + 1 until ideas.size) {
                val a = ideas[i]
                val b = ideas[j]
                val shared = tagsByIdea.getValue(a.id).intersect(tagsByIdea.getValue(b.id))
                if (shared.isNotEmpty()) {
                    val source = minOf(a.id, b.id)
                    val target = maxOf(a.id, b.id)
                    val strength = (0.58f + shared.size * 0.08f).coerceAtMost(0.9f)
                    links += LinkEntity(
                        id = "tag_${source}_${target}",
                        sourceId = source,
                        targetId = target,
                        strength = strength,
                        createdBy = "tag_rule"
                    )
                }
            }
        }
        return links.sortedBy { it.id }
    }

    private fun parseTags(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        val parsed = runCatching {
            gson.fromJson<List<String>>(raw, listType)
        }.getOrNull()
        val tags = parsed ?: raw
            .removePrefix("[")
            .removeSuffix("]")
            .split(",", "，", "#", " ")
        return tags
            .map { it.trim().trim('"', '\'', '“', '”', '#') }
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .toSet()
    }
}
